import { cloneElement, isValidElement, type ReactElement, type ReactNode } from 'react';
import { ConfigProvider, Tooltip } from 'antd';
import { usePermission, type SfEntityOperation, type SfSystemPermission } from '@/hooks/usePermission';

/**
 * SF 권한 모델 기반 선언적 UI 가드.
 *
 * 권한 판정 규칙은 [PermissionRoute] 와 동일하다:
 * - entity + operation: 특정 entity 의 CRUD 권한 검사
 * - systemPermission: SF 시스템 권한 검사
 * - 둘 다 지정 시 둘 중 하나라도 충족하면 통과
 * - 둘 다 미지정 시 통과 (가드 미사용)
 *
 * 라우트 진입 차단 (PermissionRoute) 과 달리, 화면 내부 요소(버튼/폼/섹션)를 권한에 따라
 * 다양하게 적용한다. `mode` 로 적용 방식을 선택한다.
 *
 * ## mode
 * - `'hide'` (기본): 권한 없으면 `fallback` (기본 `null`) 을 렌더 → 요소를 숨긴다.
 * - `'disable'`: 권한 없으면 자식 element 에 `disabled` prop 을 주입 → 보이되 비활성.
 *   자식은 `disabled` 를 받는 단일 element 여야 한다 (antd Button/Input 등).
 * - `'readonly'`: 권한 없으면 자식 트리를 antd `disabled` context 로 감싼다 → 폼 전체 비활성.
 *   antd `<Form>` / Form 컴포넌트 묶음에 사용.
 *
 * ## 툴팁
 * `deniedTooltip` 지정 시, 권한이 없는 상태에서 (`disable`/`readonly`/숨김 직전의 자식)
 * 위에 hover 안내를 노출한다. `hide` 모드는 요소 자체가 사라지므로 `fallback` 에
 * 안내 요소를 직접 넣어야 의미가 있다 — `deniedTooltip` 은 `disable`/`readonly` 에서만 적용된다.
 */
export type PermissionGateMode = 'hide' | 'disable' | 'readonly';

interface PermissionGateProps {
  entity?: string;
  operation?: SfEntityOperation;
  systemPermission?: SfSystemPermission;
  /** 적용 방식. 기본 `'hide'`. */
  mode?: PermissionGateMode;
  /** `mode='hide'` 에서 권한 없을 때 대체 렌더. 기본 `null` (아무것도 표시 안 함). */
  fallback?: ReactNode;
  /** `disable`/`readonly` 에서 권한 없을 때 hover 안내 문구. */
  deniedTooltip?: ReactNode;
  children: ReactElement;
}

export default function PermissionGate({
  entity,
  operation,
  systemPermission,
  mode = 'hide',
  fallback = null,
  deniedTooltip,
  children,
}: PermissionGateProps) {
  const { hasEntityPermission, hasSystemPermission } = usePermission();

  const requiresEntity = !!(entity && operation);
  const requiresSystem = !!systemPermission;
  const allowed =
    (!requiresEntity && !requiresSystem) ||
    (requiresEntity && hasEntityPermission(entity!, operation!)) ||
    (requiresSystem && hasSystemPermission(systemPermission!));

  if (allowed) {
    return children;
  }

  if (mode === 'hide') {
    return <>{fallback}</>;
  }

  const denied =
    mode === 'disable' ? (
      isValidElement(children) ? (
        cloneElement(children as ReactElement<{ disabled?: boolean }>, { disabled: true })
      ) : (
        children
      )
    ) : (
      // readonly: antd 컴포넌트 disabled context 로 자식 트리 전체를 비활성화한다.
      <ConfigProvider componentDisabled>{children}</ConfigProvider>
    );

  if (deniedTooltip) {
    return <Tooltip title={deniedTooltip}>{denied}</Tooltip>;
  }

  return denied;
}
