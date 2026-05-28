import type { MenuItem, MenuRoute } from '@/config/menuConfig';
import { flattenMenuLeaves } from '@/config/menuTraversal';
import type {
  PermissionMatrix,
  PermissionSetMatrix,
  PermissionSetSummary,
  ProfileSummary,
} from '@/api/admin/permission';
import type { SfEntityOperation, SfSystemPermission } from '@/hooks/usePermission';
import { formatEntityLabel, formatOperationLabel, formatSystemPermissionLabel } from '@/constants/permissionLabels';

/**
 * "페이지별 필요 권한" 페이지의 한 행.
 *
 * - requirementKind 별 의미:
 *   - `entity` : entity + operation 권한이 필요. byProfile/byPermissionSet 매트릭스로 매칭.
 *   - `system` : 시스템 권한 (VIEW_ALL_DATA 등) 이 필요. Profile/PS 의 해당 flag 매칭.
 *   - `profileName` : `allowedProfileNames` 매칭 (Profile.name 기반). PS 로는 만족 불가.
 *   - `open` : 권한 메타 미지정 — 로그인 사용자 전체 접근 가능.
 */
export type RequirementKind = 'entity' | 'system' | 'profileName' | 'open';

export interface PageAccessGuideRow {
  key: string;
  category: string;
  pageName: string;
  path: string;
  requirementKind: RequirementKind;
  /** 사람이 읽는 요구사항 라벨. raw 코드 병기 ("거래처 (account, READ/조회)"). */
  requirementLabel: string;
  /** 요구사항을 만족하는 Profile 목록. */
  satisfyingProfiles: { profileId: number; name: string }[];
  /** 요구사항을 만족하는 PermissionSet 목록. */
  satisfyingPermissionSets: { permissionSetId: number; name: string; label: string | null }[];
}

interface BuildRowsInput {
  menu: MenuRoute;
  profiles: ProfileSummary[];
  permissionSets: PermissionSetSummary[];
  profileMatrix: PermissionMatrix;
  permissionSetMatrix: PermissionSetMatrix;
}

export function buildRows(input: BuildRowsInput): PageAccessGuideRow[] {
  const { menu, profiles, profileMatrix, permissionSetMatrix } = input;

  const leaves = flattenMenuLeaves(menu);

  return leaves.map(({ category, item }) => {
    const requirement = classify(item);
    const requirementLabel = formatRequirement(requirement);
    const { satisfyingProfiles, satisfyingPermissionSets } = match(requirement, {
      profiles,
      profileMatrix,
      permissionSetMatrix,
    });

    return {
      key: item.path ?? `${category}/${item.name}`,
      category,
      pageName: item.name,
      path: item.path ?? '',
      requirementKind: requirement.kind,
      requirementLabel,
      satisfyingProfiles,
      satisfyingPermissionSets,
    };
  });
}

// ───────── 내부 ─────────

type Requirement =
  | { kind: 'entity'; entity: string; operation: SfEntityOperation }
  | { kind: 'system'; systemPermission: SfSystemPermission }
  | { kind: 'profileName'; allowedProfileNames: string[] }
  | { kind: 'open' };

function classify(item: MenuItem): Requirement {
  // allowedProfileNames 가 가장 강력한 가드 — AdminLayout.itemAllowed 와 동일 우선순위
  if (item.allowedProfileNames && item.allowedProfileNames.length > 0) {
    return { kind: 'profileName', allowedProfileNames: item.allowedProfileNames };
  }
  if (item.entity && item.operation) {
    return { kind: 'entity', entity: item.entity, operation: item.operation };
  }
  if (item.systemPermission) {
    return { kind: 'system', systemPermission: item.systemPermission };
  }
  return { kind: 'open' };
}

function formatRequirement(req: Requirement): string {
  switch (req.kind) {
    case 'entity':
      return `${formatEntityLabel(req.entity)} (${req.entity}, ${req.operation}/${formatOperationLabel(req.operation)})`;
    case 'system':
      return `${formatSystemPermissionLabel(req.systemPermission)} (${req.systemPermission})`;
    case 'profileName':
      return `특정 Profile 만 접근: [${req.allowedProfileNames.join(', ')}]`;
    case 'open':
      return '(로그인 사용자 전체)';
  }
}

function match(
  req: Requirement,
  data: {
    profiles: ProfileSummary[];
    profileMatrix: PermissionMatrix;
    permissionSetMatrix: PermissionSetMatrix;
  },
): {
  satisfyingProfiles: PageAccessGuideRow['satisfyingProfiles'];
  satisfyingPermissionSets: PageAccessGuideRow['satisfyingPermissionSets'];
} {
  if (req.kind === 'open') {
    return { satisfyingProfiles: [], satisfyingPermissionSets: [] };
  }

  if (req.kind === 'profileName') {
    const matched = data.profiles
      .filter((p) => req.allowedProfileNames.includes(p.name))
      .map((p) => ({ profileId: p.profileId, name: p.name }));
    // allowedProfileNames 가드는 Profile.name 매칭이라 PS 로 만족 불가.
    return { satisfyingProfiles: matched, satisfyingPermissionSets: [] };
  }

  if (req.kind === 'system') {
    const flagKey = systemFlagKeyOf(req.systemPermission);
    const profiles = data.profiles
      .filter((p) => p[flagKey])
      .map((p) => ({ profileId: p.profileId, name: p.name }));
    // PS 는 viewAllData / modifyAllData 만 매칭 가능 — 나머지 플래그는 API 부재.
    const psMatches =
      req.systemPermission === 'VIEW_ALL_DATA' || req.systemPermission === 'MODIFY_ALL_DATA'
        ? data.permissionSetMatrix.permissionSets
            .filter((ps) =>
              req.systemPermission === 'VIEW_ALL_DATA' ? ps.viewAllData : ps.modifyAllData,
            )
            .map((ps) => ({ permissionSetId: ps.permissionSetId, name: ps.name, label: ps.label }))
        : [];
    return { satisfyingProfiles: profiles, satisfyingPermissionSets: psMatches };
  }

  // entity + operation
  const opFlag = entityOperationFlagOf(req.operation);
  const profileRow = data.profileMatrix.rows.find((r) => r.entity === req.entity);
  const profileMatches = profileRow
    ? profileRow.byProfile
        .filter((bp) => bp[opFlag])
        .map((bp) => {
          const profile = data.profiles.find((p) => p.profileId === bp.profileId);
          return profile
            ? { profileId: bp.profileId, name: profile.name }
            : { profileId: bp.profileId, name: `(profileId=${bp.profileId})` };
        })
    : [];
  const psMatches = data.permissionSetMatrix.permissionSets
    .filter((ps) => {
      const op = ps.objectPermissions.find((p) => p.entity === req.entity);
      return op ? op[opFlag] : false;
    })
    .map((ps) => ({ permissionSetId: ps.permissionSetId, name: ps.name, label: ps.label }));

  return { satisfyingProfiles: profileMatches, satisfyingPermissionSets: psMatches };
}

function systemFlagKeyOf(
  systemPermission: SfSystemPermission,
): 'viewAllData' | 'modifyAllData' | 'viewAllUsers' | 'manageUsers' | 'apiEnabled' {
  switch (systemPermission) {
    case 'VIEW_ALL_DATA':
      return 'viewAllData';
    case 'MODIFY_ALL_DATA':
      return 'modifyAllData';
    case 'VIEW_ALL_USERS':
      return 'viewAllUsers';
    case 'MANAGE_USERS':
      return 'manageUsers';
    case 'API_ENABLED':
      return 'apiEnabled';
  }
}

function entityOperationFlagOf(
  operation: SfEntityOperation,
): 'canRead' | 'canCreate' | 'canEdit' | 'canDelete' {
  switch (operation) {
    case 'READ':
      return 'canRead';
    case 'CREATE':
      return 'canCreate';
    case 'EDIT':
      return 'canEdit';
    case 'DELETE':
      return 'canDelete';
  }
}
