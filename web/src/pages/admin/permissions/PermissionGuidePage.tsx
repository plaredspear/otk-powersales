import { useMemo, useState } from 'react';
import {
  Alert,
  Anchor,
  Card,
  Collapse,
  Descriptions,
  Divider,
  Result,
  Select,
  Space,
  Steps,
  Table,
  Tag,
  Typography,
} from 'antd';
import {
  BookOutlined,
  BulbOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  ExperimentOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons';
import { ENTITY_LABELS, OPERATION_LABELS } from '@/constants/permissionLabels';

const { Title, Paragraph, Text } = Typography;

interface ScenarioRow {
  who: string;
  goal: string;
  how: string;
}

// ───────── 시뮬레이터용 권한 모델 ─────────
type Op = 'READ' | 'CREATE' | 'EDIT' | 'DELETE';

interface SystemBits {
  viewAllData?: boolean;
  modifyAllData?: boolean;
  viewAllUsers?: boolean;
  manageUsers?: boolean;
  apiEnabled?: boolean;
}

/** 엔티티 → 작업 sparse 매트릭스. 미정의는 false 로 간주. */
type EntityMatrix = Partial<Record<string, Partial<Record<Op, boolean>>>>;

interface PermissionDef {
  key: string;
  label: string;
  bits: SystemBits;
  matrix: EntityMatrix;
}

interface SimScenario {
  key: string;
  label: string;
  profileKey: string;
  permissionSetKeys: string[];
  description: string;
}

/** 시뮬레이터 Select 가 노출할 entity 목록 — 한글 라벨은 ENTITY_LABELS 상수에서 합성. */
const SIMULATOR_ENTITY_KEYS: string[] = [
  'account',
  'employee',
  'promotion',
  'team_member_schedule',
  'claim',
  'monthly_sales_history',
  'attendance_log',
  'notice_post',
  'product',
  'upload_file',
  'user',
];

const ENTITIES: Array<{ value: string; label: string }> = SIMULATOR_ENTITY_KEYS.map((value) => ({
  value,
  label: `${ENTITY_LABELS[value] ?? value} (${value})`,
}));

const OPERATIONS: Array<{ value: Op; label: string }> = (['READ', 'CREATE', 'EDIT', 'DELETE'] as const).map(
  (value) => ({ value, label: OPERATION_LABELS[value] }),
);

/**
 * 프로파일 권한 정의 (시뮬레이션용 — SF 레거시 추정).
 * 운영 실측과 100% 일치는 아니며, 학습/시뮬레이션 목적의 대표 모델.
 */
const PROFILE_DEFS: PermissionDef[] = [
  {
    key: 'admin',
    label: '시스템 관리자',
    bits: { modifyAllData: true, viewAllData: true, viewAllUsers: true, manageUsers: true, apiEnabled: true },
    matrix: {},
  },
  {
    key: 'ceo',
    label: 'CEO',
    bits: { viewAllData: true, viewAllUsers: true, apiEnabled: true },
    matrix: {},
  },
  {
    key: 'p1_director',
    label: '1.본부장',
    bits: { viewAllData: true, viewAllUsers: true, apiEnabled: true },
    matrix: {
      account: { READ: true, EDIT: true },
      employee: { READ: true },
      promotion: { READ: true, EDIT: true },
      monthly_sales_history: { READ: true },
    },
  },
  {
    key: 'p2_div',
    label: '2.사업부장',
    bits: { viewAllUsers: true, apiEnabled: true },
    matrix: {
      account: { READ: true, EDIT: true },
      employee: { READ: true },
      promotion: { READ: true, EDIT: true },
      monthly_sales_history: { READ: true },
      claim: { READ: true },
    },
  },
  {
    key: 'p4_branch',
    label: '4.지점장',
    bits: { apiEnabled: true },
    matrix: {
      account: { READ: true, EDIT: true },
      employee: { READ: true },
      team_member_schedule: { READ: true, EDIT: true },
      monthly_sales_history: { READ: true },
    },
  },
  {
    key: 'p5_sales',
    label: '5.영업사원',
    bits: { apiEnabled: true },
    matrix: {
      account: { READ: true, EDIT: true },
      monthly_sales_history: { READ: true },
      attendance_log: { READ: true, CREATE: true, EDIT: true },
      claim: { READ: true, CREATE: true },
    },
  },
  {
    key: 'p5_sales_ip',
    label: '5.영업사원(로그인 IP 대역 설정)',
    bits: { apiEnabled: true },
    matrix: {
      account: { READ: true, EDIT: true },
      monthly_sales_history: { READ: true },
      attendance_log: { READ: true, CREATE: true, EDIT: true },
      claim: { READ: true, CREATE: true },
    },
  },
  {
    key: 'p6_leader',
    label: '6.조장',
    bits: { apiEnabled: true },
    matrix: {
      team_member_schedule: { READ: true, EDIT: true },
      employee: { READ: true },
      attendance_log: { READ: true },
    },
  },
  {
    key: 'p7_sales_leader',
    label: '7.영업사원 + 조장',
    bits: { apiEnabled: true },
    matrix: {
      account: { READ: true, EDIT: true },
      monthly_sales_history: { READ: true },
      attendance_log: { READ: true, CREATE: true, EDIT: true },
      claim: { READ: true, CREATE: true },
      team_member_schedule: { READ: true, EDIT: true },
      employee: { READ: true },
    },
  },
  {
    key: 'p9_staff',
    label: '9.Staff',
    bits: { apiEnabled: true },
    matrix: {
      notice_post: { READ: true },
      product: { READ: true },
    },
  },
  {
    key: 'p11_marketing_intern',
    label: '11.마케팅(인턴)',
    bits: { apiEnabled: true },
    matrix: {
      promotion: { READ: true },
      product: { READ: true },
    },
  },
  {
    key: 'p12_marketing_lead',
    label: '12.마케팅(팀장 이상)',
    bits: { apiEnabled: true },
    matrix: {
      promotion: { READ: true, CREATE: true, EDIT: true, DELETE: true },
      product: { READ: true, EDIT: true },
      notice_post: { READ: true, CREATE: true, EDIT: true },
    },
  },
  {
    key: 'p_quality',
    label: '품질보증실',
    bits: { apiEnabled: true },
    matrix: {
      claim: { READ: true, EDIT: true },
      product: { READ: true },
    },
  },
];

/**
 * 권한 세트 정의 (시뮬레이션용 — SF 레거시 운영 IsCustom=TRUE 핵심 PS).
 */
const PS_DEFS: PermissionDef[] = [
  {
    key: 'acc_view_all',
    label: '거래처 전체 조회 (Acc_Permission)',
    bits: {},
    matrix: { account: { READ: true } },
  },
  {
    key: 'employee_view_all',
    label: '사원 전체 조회 (Employee_View_All)',
    bits: { viewAllUsers: true },
    matrix: { employee: { READ: true } },
  },
  {
    key: 'claim_view_all',
    label: '클레임 전체 조회 (Claim_View_All)',
    bits: {},
    matrix: { claim: { READ: true } },
  },
  {
    key: 'promotion_view_all',
    label: '행사마스터 전체 조회 (Promotion_Master_View_All)',
    bits: {},
    matrix: { promotion: { READ: true } },
  },
  {
    key: 'tms_view_all',
    label: '여사원일정 전체 조회 (View_All_TeamMemberSchedule)',
    bits: {},
    matrix: { team_member_schedule: { READ: true } },
  },
  {
    key: 'sales_diary_view_all',
    label: '영업일지 전체 조회 (SalesDiary_View_All)',
    bits: {},
    matrix: { monthly_sales_history: { READ: true } },
  },
  {
    key: 'ppt_edit',
    label: '전문행사조 수정 (ProfessionalPromotionTeam)',
    bits: {},
    matrix: { promotion: { READ: true, EDIT: true } },
  },
  {
    key: 'view_all_edit_all',
    label: '현장점검 전체 조회/수정 (View_All_Edit_All)',
    bits: {},
    matrix: { attendance_log: { READ: true, EDIT: true } },
  },
  {
    key: 'upload_file_create_delete',
    label: '업로드 파일 생성/삭제 (Uploadfile_Create_Delete_Permission)',
    bits: {},
    matrix: { upload_file: { READ: true, CREATE: true, DELETE: true } },
  },
  {
    key: 'notice',
    label: '공지사항/교육 수정 (notice)',
    bits: {},
    matrix: { notice_post: { READ: true, CREATE: true, EDIT: true } },
  },
  {
    key: 'rehabilitation',
    label: '여사원 복직 권한 (rehabilitation)',
    bits: {},
    matrix: { employee: { READ: true, EDIT: true } },
  },
  {
    key: 'sales_assistant',
    label: '영업지원실 (SalesAssistant)',
    bits: {},
    matrix: {
      account: { READ: true },
      monthly_sales_history: { READ: true },
      attendance_log: { READ: true },
    },
  },
  {
    key: 'sales_support',
    label: '9.Staff 지원 (SalesSupport)',
    bits: {},
    matrix: {
      employee: { READ: true },
      team_member_schedule: { READ: true },
    },
  },
];

/**
 * 사전 정의 시나리오 — 셀렉트박스에서 한 번에 사용자 프로필을 고르도록.
 * 6번 섹션 "조합 케이스" 와 1:1 대응.
 */
const SIM_SCENARIOS: SimScenario[] = [
  {
    key: 'sc_admin',
    label: '서운영 (운영팀, 시스템 관리자)',
    profileKey: 'admin',
    permissionSetKeys: [],
    description: '시스템 관리자 — 모든 데이터 수정 비트로 전 권한 자동 통과',
  },
  {
    key: 'sc_ceo',
    label: '강분석 (경영진, CEO + 가산 2)',
    profileKey: 'ceo',
    permissionSetKeys: ['sales_diary_view_all'],
    description: '모든 데이터 조회 + 영업일지 분석',
  },
  {
    key: 'sc_sales_new',
    label: '홍영업 (신입 영업사원, 단독)',
    profileKey: 'p5_sales',
    permissionSetKeys: [],
    description: '본인 소유 거래처/매출만 — 기본 권한 단독',
  },
  {
    key: 'sc_sales_special',
    label: '김특판 (특판 영업, +거래처 전체 조회)',
    profileKey: 'p5_sales',
    permissionSetKeys: ['acc_view_all'],
    description: '기본 권한 + 거래처 전체 조회 권한 세트 1개 가산',
  },
  {
    key: 'sc_sales_leader',
    label: '박조장 (영업+조장, +여사원일정 전체 조회)',
    profileKey: 'p7_sales_leader',
    permissionSetKeys: ['tms_view_all'],
    description: '조 간 일정 조정 필요',
  },
  {
    key: 'sc_event_planner',
    label: '정행사 (행사 기획, 다중 가산)',
    profileKey: 'p5_sales',
    permissionSetKeys: ['ppt_edit', 'promotion_view_all'],
    description: '직무 특화 다중 가산',
  },
  {
    key: 'sc_quality',
    label: '최품질 (품질보증실, +클레임 전체 조회)',
    profileKey: 'p_quality',
    permissionSetKeys: ['claim_view_all'],
    description: '품질 분석 시 담당 외 클레임도 필요',
  },
  {
    key: 'sc_support',
    label: '이지원 (영업지원실, 9.Staff + 2개 가산)',
    profileKey: 'p9_staff',
    permissionSetKeys: ['sales_assistant', 'sales_support'],
    description: '9.Staff 자체가 최소 권한 → 가산 필수',
  },
  {
    key: 'sc_rehab',
    label: '윤복직 (인사 복직 처리, 한시 가산)',
    profileKey: 'p5_sales',
    permissionSetKeys: ['rehabilitation'],
    description: '복직 처리 한시 권한 — 완료 후 회수',
  },
  {
    key: 'sc_intern',
    label: '한인턴 (마케팅 인턴, 단독)',
    profileKey: 'p11_marketing_intern',
    permissionSetKeys: [],
    description: '제한된 조회만 — 가산 금지 (감사 정책)',
  },
  {
    key: 'sc_director',
    label: '도본부 (1.본부장, 단독)',
    profileKey: 'p1_director',
    permissionSetKeys: [],
    description: '모든 데이터 조회 비트로 전사 read 가능',
  },
  {
    key: 'sc_marketing_lead',
    label: '오마케 (12.마케팅 팀장, 단독)',
    profileKey: 'p12_marketing_lead',
    permissionSetKeys: [],
    description: '마케팅 전체 CRUD + 공지/제품 수정',
  },
];

interface EvaluationReason {
  source: string;
  reason: string;
}

interface EvaluationResult {
  allowed: boolean;
  reasons: EvaluationReason[];
}

function evaluate(
  profileKey: string,
  permissionSetKeys: string[],
  entity: string,
  op: Op,
): EvaluationResult {
  const reasons: EvaluationReason[] = [];
  let allowed = false;

  const profile = PROFILE_DEFS.find((p) => p.key === profileKey);
  const sets = permissionSetKeys
    .map((k) => PS_DEFS.find((p) => p.key === k))
    .filter((x): x is PermissionDef => !!x);

  // 1) 프로파일 시스템 비트 우회
  if (profile?.bits.modifyAllData) {
    allowed = true;
    reasons.push({ source: `프로파일: ${profile.label}`, reason: '모든 데이터 수정 비트 — 전 작업 자동 통과' });
  }
  if (op === 'READ' && profile?.bits.viewAllData) {
    allowed = true;
    reasons.push({ source: `프로파일: ${profile.label}`, reason: '모든 데이터 조회 비트 — 모든 엔티티 조회 통과' });
  }
  // user 엔티티 특수: VIEW_ALL_USERS 도 조회 통과
  if (op === 'READ' && entity === 'user' && profile?.bits.viewAllUsers) {
    allowed = true;
    reasons.push({ source: `프로파일: ${profile.label}`, reason: '모든 사용자 조회 비트 — user 엔티티 조회 통과' });
  }

  // 2) 프로파일 엔티티 매트릭스
  if (profile?.matrix[entity]?.[op]) {
    allowed = true;
    reasons.push({ source: `프로파일: ${profile.label}`, reason: `엔티티 권한 매트릭스에 ${entity}.${op} 정의됨` });
  }

  // 3) 권한 세트 가산
  for (const ps of sets) {
    if (ps.bits.modifyAllData) {
      allowed = true;
      reasons.push({ source: `권한 세트: ${ps.label}`, reason: '모든 데이터 수정 비트 — 전 작업 자동 통과' });
    }
    if (op === 'READ' && ps.bits.viewAllData) {
      allowed = true;
      reasons.push({ source: `권한 세트: ${ps.label}`, reason: '모든 데이터 조회 비트 — 모든 엔티티 조회 통과' });
    }
    if (op === 'READ' && entity === 'user' && ps.bits.viewAllUsers) {
      allowed = true;
      reasons.push({ source: `권한 세트: ${ps.label}`, reason: '모든 사용자 조회 비트 — user 엔티티 조회 통과' });
    }
    if (ps.matrix[entity]?.[op]) {
      allowed = true;
      reasons.push({ source: `권한 세트: ${ps.label}`, reason: `엔티티 권한 매트릭스에 ${entity}.${op} 정의됨` });
    }
  }

  if (!allowed) {
    reasons.push({
      source: '평가 결과',
      reason: '프로파일/권한 세트 어디에도 매칭되는 권한 없음 → 차단',
    });
  }

  return { allowed, reasons };
}

interface BitRowKr {
  bit: string;
  bitEn: string;
  meaning: string;
  example: string;
}

interface CrudRowKr {
  op: string;
  opEn: string;
  example: string;
}

const systemBits: BitRowKr[] = [
  { bit: '모든 데이터 조회', bitEn: 'VIEW_ALL_DATA', meaning: '모든 데이터 조회 가능 (소유자 / 공유 규칙 무시)', example: '전사 매출 통계 분석 담당자' },
  { bit: '모든 데이터 수정', bitEn: 'MODIFY_ALL_DATA', meaning: '모든 데이터 수정/삭제 가능 (소유자 / 공유 규칙 무시)', example: '시스템 관리자' },
  { bit: '모든 사용자 조회', bitEn: 'VIEW_ALL_USERS', meaning: '모든 사용자 레코드 조회 가능 (조직 외부 포함)', example: '인사팀 조회 권한' },
  { bit: '사용자 관리', bitEn: 'MANAGE_USERS', meaning: '사용자 계정 생성/비활성화, 권한 부여/회수', example: '운영팀 / 시스템 관리자' },
  { bit: 'API 사용', bitEn: 'API_ENABLED', meaning: 'REST API 직접 호출 가능 (모바일 앱 로그인 등)', example: '모든 영업사원' },
];

const crudOps: CrudRowKr[] = [
  { op: '조회', opEn: 'READ', example: '거래처 목록 보기' },
  { op: '생성', opEn: 'CREATE', example: '신규 거래처 등록' },
  { op: '수정', opEn: 'EDIT', example: '기존 거래처 정보 변경' },
  { op: '삭제', opEn: 'DELETE', example: '거래처 레코드 삭제' },
];

interface LegacyProfileRow {
  num: string;
  name: string;
  role: string;
  scope: string;
}

interface LegacyPsRow {
  ps: string;
  label: string;
  purpose: string;
  typicalUser: string;
}

interface CombinationRow {
  user: string;
  profile: string;
  sets: string;
  effect: string;
  why: string;
}

const scenarios: ScenarioRow[] = [
  {
    who: '신규 영업사원 입사',
    goal: '"5.영업사원" 프로파일 만 부여 → 본인 담당 거래처 조회/수정',
    how: '사용자 관리 → 신규 등록 → 프로파일 = "5.영업사원" 선택',
  },
  {
    who: '신입 영업사원이 본인 외 거래처도 봐야 함',
    goal: '프로파일 변경 없이 거래처 전체 조회 가능',
    how: '사원 현황 → 해당 사원 → 권한 → "거래처 전체 조회 (Acc_Permission)" 권한 세트 부여',
  },
  {
    who: '특정 조장에게 전문행사조 수정 권한',
    goal: '프로파일 (6.조장) 그대로 두고 행사조 수정만 가산',
    how: '권한 세트 관리 → "전문행사조 수정 (ProfessionalPromotionTeam)" 선택 → 사용자 추가 → 사번 선택',
  },
  {
    who: '영업지원실 신규 입사자',
    goal: '영업 전반 조회 + 일부 입력 (지원실 역할)',
    how: '프로파일 = "9.Staff" + 권한 세트 "영업지원실 (SalesAssistant)" 가산',
  },
  {
    who: '여사원 복직 처리',
    goal: '복직 처리 권한 한시적 부여',
    how: '권한 세트 관리 → "여사원 복직 (rehabilitation)" 부여 → 복직 처리 완료 후 회수',
  },
  {
    who: '신규 마케팅(인턴) 입사',
    goal: '"11.마케팅(인턴)" 프로파일 만 부여 → 제한된 마케팅 조회',
    how: '사용자 관리 → 신규 등록 → 프로파일 = "11.마케팅(인턴)" 선택',
  },
  {
    who: '특정 영업사원이 클레임 전체 조회 필요',
    goal: '본인 담당 외 클레임도 조회',
    how: '권한 세트 "클레임 전체 조회 (Claim_View_All)" 가산 (담당 외 클레임 조회 가능)',
  },
  {
    who: '특정 운영자에게 업로드 파일 삭제 권한',
    goal: '파일 업로드는 다수 가능하지만 삭제는 한정',
    how: '권한 세트 "업로드 파일 생성/삭제 (Uploadfile_Create_Delete_Permission)" 가산',
  },
  {
    who: '운영팀이 권한 검토',
    goal: '프로파일 별로 어떤 엔티티의 어떤 조회/생성/수정/삭제 권한이 열려있는지 한눈에 확인',
    how: '권한 매트릭스 → 엔티티 검색 / 프로파일 컬럼 비교',
  },
  {
    who: '직원이 특정 화면에 진입 못 함 (403)',
    goal: '어떤 권한이 누락되었는지 진단',
    how: '사원 현황 → 해당 직원 → "권한" 섹션 → 실효 엔티티 × 조회/생성/수정/삭제 매트릭스 확인',
  },
  {
    who: '경영진 (CEO) 신규 추가',
    goal: '전체 데이터 조회 + 일부 입력',
    how: '프로파일 = "CEO" 선택 (모든 데이터 조회 비트 자동 포함)',
  },
  {
    who: '시스템 관리자 권한 위임',
    goal: '운영팀 일원에게 전체 권한 부여',
    how: '프로파일 = "시스템 관리자" (모든 데이터 수정 + 사용자 관리 비트 자동 포함)',
  },
];

// SF 운영 IsCustom=TRUE Profile 인벤토리 (docs/plan/old_source_260516/aladdin_260516_prod/force-app/main/default/profiles/)
// PSG 12개는 force__ namespace (SF 표준 패키지) 라 운영 IsCustom=TRUE PS 49개와 교집합 0 — 본 표 제외
const legacyProfiles: LegacyProfileRow[] = [
  { num: '1', name: '본부장', role: '영업본부 총괄', scope: '본부 전체 데이터 + 승인 권한' },
  { num: '2', name: '사업부장', role: '사업부 총괄', scope: '사업부 하위 데이터 + 승인' },
  { num: '3', name: '영업부장', role: '영업부 총괄', scope: '영업부 하위 데이터 + 승인' },
  { num: '4', name: '지점장', role: '지점 단위 관리', scope: '지점 소속 사원/거래처 관리' },
  { num: '5', name: '영업사원', role: '담당 거래처 영업', scope: '본인 소유 데이터만 (전체 조회 비트 없음)' },
  { num: '5', name: '영업사원(로그인 IP 대역 설정)', role: '영업사원 변형 (IP 제한)', scope: '5.영업사원 + 로그인 IP 화이트리스트' },
  { num: '6', name: '조장', role: '조 단위 관리', scope: '소속 조원 일정 + 본인 데이터' },
  { num: '6-1', name: '조장', role: '6.조장 변형', scope: '서브 조장 (일부 권한 차등)' },
  { num: '7', name: '영업사원 + 조장', role: '영업사원 + 조장 겸임', scope: '5 + 6 결합 (프로파일 1인 1개 원칙이라 별도 정의)' },
  { num: '8', name: '마케팅', role: '마케팅 일반', scope: '마케팅 데이터 + 일부 영업 조회' },
  { num: '9', name: 'Staff', role: '지원 인력 (영업지원실 등)', scope: '제한된 조회 + Staff 가산 권한 세트 필수' },
  { num: '10', name: '마케팅(super)', role: '마케팅 슈퍼유저', scope: '마케팅 전체 + 일부 운영' },
  { num: '11', name: '마케팅(인턴)', role: '마케팅 인턴', scope: '제한된 조회만' },
  { num: '12', name: '마케팅(팀장 이상)', role: '마케팅 팀장+', scope: '마케팅 전체 수정' },
  { num: '-', name: 'CEO', role: '경영진', scope: '전체 데이터 조회' },
  { num: '-', name: '시스템 관리자', role: '시스템 관리자', scope: '모든 데이터 수정 + 사용자 관리 비트 포함' },
  { num: '-', name: '공장관계자', role: '공장/생산 부서', scope: '공장 출하/생산 데이터' },
  { num: '-', name: '품질보증실', role: '품질 검사', scope: '품질/클레임 조회 + 일부 수정' },
];

// SF 운영 IsCustom=TRUE 권한 세트 인벤토리 (운영자에게 의미 있는 18개)
const legacyPS: LegacyPsRow[] = [
  { ps: 'Acc_Permission', label: '거래처 전체 조회', purpose: '본인 담당 외 거래처도 조회', typicalUser: '지점장 / 부장급 / 특판 담당' },
  { ps: 'Sales_User', label: '영업 표준 권한', purpose: 'SF Sales 표준 권한 묶음', typicalUser: '영업 전반' },
  { ps: 'Activity_View_All', label: '현장점검결과 전체 조회', purpose: '담당 외 현장점검 결과 조회', typicalUser: '점검 총괄' },
  { ps: 'Object_View_All', label: '객체 전체 조회', purpose: '특정 객체 전체 조회', typicalUser: '데이터 분석' },
  { ps: 'ProfessionalPromotionTeam', label: '전문행사조 수정', purpose: '전문행사조 마스터 수정', typicalUser: '행사 기획 담당' },
  { ps: 'SalesAssistant', label: '영업지원실', purpose: '영업지원실 추가 권한', typicalUser: '9.Staff + 지원실 인력' },
  { ps: 'SalesSupport', label: '9.Staff 지원', purpose: '9.Staff 프로파일 보강', typicalUser: '9.Staff 전원' },
  { ps: 'notice', label: '공지사항 (회사, 교육)', purpose: '공지/교육 수정', typicalUser: '운영 / 교육 담당' },
  { ps: 'Promotion_Master_View_All', label: '행사마스터 전체 조회', purpose: '담당 외 행사 마스터 조회', typicalUser: '본부장 / 행사 총괄' },
  { ps: 'Employee_View_All', label: '사원 전체 조회', purpose: '담당 외 사원 정보 조회', typicalUser: '인사 / 지점장' },
  { ps: 'Claim_View_All', label: '클레임 전체 조회', purpose: '담당 외 클레임 조회', typicalUser: '품질보증실 / 본부' },
  { ps: 'Uploadfile_Create_Delete_Permission', label: '업로드 파일 생성/삭제', purpose: '파일 삭제 권한 한정', typicalUser: '운영 / 파일 관리자' },
  { ps: 'View_All_Edit_All', label: '현장점검등록 전체 조회/수정', purpose: '점검 전체 수정', typicalUser: '점검 책임자' },
  { ps: 'View_ALL_EVENT', label: '행사사원 전체 조회', purpose: '담당 외 행사 배치 조회', typicalUser: '행사 총괄' },
  { ps: 'View_All_TeamMemberSchedule', label: '여사원일정 전체 조회', purpose: '담당 외 일정 조회', typicalUser: '본부 / 행사 총괄' },
  { ps: 'rehabilitation', label: '여사원 복직 권한', purpose: '복직 처리 한시 권한', typicalUser: '인사 (복직 처리 시점에만)' },
  { ps: 'SalesProgressViewAll', label: '거래처목표마스터 전체 조회', purpose: '목표 진척 분석', typicalUser: '경영 분석 / 본부장' },
  { ps: 'SalesDiary_View_All', label: '영업일지 전체 조회', purpose: '담당 외 영업일지 조회', typicalUser: '지점장 / 본부' },
];

// 프로파일 × 권한 세트 조합 케이스 (가능한 모든 조합 패턴 망라)
const combinationCases: CombinationRow[] = [
  {
    user: '홍영업 (신입 영업사원)',
    profile: '5.영업사원',
    sets: '-',
    effect: '본인 소유 거래처/매출만 조회/생성/수정/삭제',
    why: '프로파일 만으로 기본 권한 충족 — 가산 불요',
  },
  {
    user: '김특판 (특판 영업)',
    profile: '5.영업사원',
    sets: '거래처 전체 조회',
    effect: '기본 권한 + 전체 거래처 조회',
    why: '담당 외 거래처 조회 필요 → 권한 세트 1개 가산',
  },
  {
    user: '박조장 (영업 + 조장)',
    profile: '7.영업사원 + 조장',
    sets: '여사원일정 전체 조회',
    effect: '기본 권한 + 본인 조 외 일정 조회',
    why: '조 간 일정 조정 필요',
  },
  {
    user: '이지원 (영업지원실)',
    profile: '9.Staff',
    sets: '영업지원실 + 9.Staff 지원',
    effect: '기본 권한 + 영업 지원 보조 + 9.Staff 보강',
    why: '9.Staff 프로파일 자체가 최소 권한 → 가산 필수',
  },
  {
    user: '정행사 (행사 기획)',
    profile: '5.영업사원',
    sets: '전문행사조 수정 + 행사마스터 전체 조회 + 행사사원 전체 조회',
    effect: '기본 권한 + 전문행사조 수정 + 행사 마스터 조회 + 행사 배치 조회',
    why: '직무 특화 다중 가산',
  },
  {
    user: '최품질 (품질보증실)',
    profile: '품질보증실',
    sets: '클레임 전체 조회',
    effect: '품질 기본 권한 + 전체 클레임 조회',
    why: '품질 분석 시 담당 외 클레임도 필요',
  },
  {
    user: '강분석 (경영 분석)',
    profile: 'CEO',
    sets: '거래처목표마스터 전체 조회 + 영업일지 전체 조회',
    effect: 'CEO 조회 + 목표 진척 + 영업일지 분석',
    why: '모든 데이터 조회 비트 있어도 일부 엔티티는 권한 세트 가산 필요',
  },
  {
    user: '윤복직 (인사 복직 처리)',
    profile: '5.영업사원',
    sets: '여사원 복직 권한 (1주일 한정)',
    effect: '기본 권한 + 복직 처리 권한',
    why: '한시 권한 — 처리 완료 후 회수',
  },
  {
    user: '서운영 (운영팀)',
    profile: '시스템 관리자',
    sets: '-',
    effect: '전체 엔티티 조회/생성/수정/삭제 + 모든 데이터 수정 + 사용자 관리',
    why: '시스템 관리자 프로파일 단일로 충족 — 가산 의미 없음',
  },
  {
    user: '한인턴 (마케팅 인턴)',
    profile: '11.마케팅(인턴)',
    sets: '-',
    effect: '제한된 조회만',
    why: '인턴은 가산 금지 (감사 정책)',
  },
];

interface SfMappingRow {
  ours: string;
  sfFeature: string;
  sfPath?: string;
  note?: string;
}

interface SalesforceMappingProps {
  title?: string;
  intro?: React.ReactNode;
  rows: SfMappingRow[];
  extra?: React.ReactNode;
}

/**
 * 각 섹션 하단에 일관된 형태로 "Salesforce 대응" 정보 표시.
 * SF 사용자가 익숙한 SF 용어 → 본 시스템 용어로 즉시 매핑할 수 있도록.
 */
function SalesforceMapping({ title = 'Salesforce 대응', intro, rows, extra }: SalesforceMappingProps) {
  return (
    <div
      style={{
        marginTop: 16,
        padding: 16,
        background: '#f6f8ff',
        border: '1px solid #d6e4ff',
        borderRadius: 8,
      }}
    >
      <Space style={{ marginBottom: 8 }}>
        <Tag color="geekblue" style={{ fontWeight: 600 }}>
          ☁ {title}
        </Tag>
      </Space>
      {intro && <Paragraph style={{ marginBottom: 12 }}>{intro}</Paragraph>}
      <Table
        size="small"
        pagination={false}
        rowKey={(_r, i) => String(i)}
        dataSource={rows}
        columns={[
          {
            title: '본 시스템',
            dataIndex: 'ours',
            key: 'ours',
            width: 260,
            render: (v: string) => <Text strong>{v}</Text>,
          },
          {
            title: 'Salesforce 기능',
            dataIndex: 'sfFeature',
            key: 'sfFeature',
            width: 260,
            render: (v: string) => <Tag color="blue">{v}</Tag>,
          },
          {
            title: 'SF Setup 경로 / 비고',
            key: 'sfPathNote',
            render: (_v: unknown, row: SfMappingRow) => (
              <Space direction="vertical" size={0}>
                {row.sfPath && <Text code>{row.sfPath}</Text>}
                {row.note && <Text type="secondary">{row.note}</Text>}
              </Space>
            ),
          },
        ]}
      />
      {extra && <div style={{ marginTop: 12 }}>{extra}</div>}
    </div>
  );
}

function PermissionSimulator() {
  const [scenarioKey, setScenarioKey] = useState<string>(SIM_SCENARIOS[2].key); // 홍영업 기본
  const [entity, setEntity] = useState<string>('account');
  const [op, setOp] = useState<Op>('READ');

  const scenario = SIM_SCENARIOS.find((s) => s.key === scenarioKey)!;
  const profile = PROFILE_DEFS.find((p) => p.key === scenario.profileKey)!;
  const sets = scenario.permissionSetKeys
    .map((k) => PS_DEFS.find((p) => p.key === k))
    .filter((x): x is PermissionDef => !!x);

  const result = useMemo(
    () => evaluate(scenario.profileKey, scenario.permissionSetKeys, entity, op),
    [scenario, entity, op],
  );

  const entityLabel = ENTITIES.find((e) => e.value === entity)?.label ?? entity;
  const opLabel = OPERATIONS.find((o) => o.value === op)?.label ?? op;

  return (
    <>
      <Alert
        type="info"
        showIcon
        message="시뮬레이션 동작 방식"
        description={
          <>
            사용자 시나리오 + 엔티티 + 작업을 선택하면 <Text strong>실효 권한 계산 공식</Text> 에 따라 클라이언트 사이드로 즉시 평가합니다.
            실제 운영 데이터가 아니며 SF 레거시 추정 모델 기반의 학습/시뮬레이션용입니다.
          </>
        }
        style={{ marginBottom: 16 }}
      />

      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Space wrap size={12}>
          <div>
            <Text strong>사용자 시나리오</Text>
            <br />
            <Select
              style={{ width: 420, marginTop: 4 }}
              value={scenarioKey}
              onChange={setScenarioKey}
              options={SIM_SCENARIOS.map((s) => ({ value: s.key, label: s.label }))}
            />
          </div>
          <div>
            <Text strong>엔티티</Text>
            <br />
            <Select
              style={{ width: 280, marginTop: 4 }}
              value={entity}
              onChange={setEntity}
              options={ENTITIES.map((e) => ({ value: e.value, label: e.label }))}
            />
          </div>
          <div>
            <Text strong>작업</Text>
            <br />
            <Select
              style={{ width: 140, marginTop: 4 }}
              value={op}
              onChange={(v: Op) => setOp(v)}
              options={OPERATIONS.map((o) => ({ value: o.value, label: o.label }))}
            />
          </div>
        </Space>

        <Descriptions
          column={1}
          size="small"
          bordered
          items={[
            {
              key: 'profile',
              label: '프로파일',
              children: <Tag color="blue">{profile.label}</Tag>,
            },
            {
              key: 'sets',
              label: '권한 세트',
              children:
                sets.length === 0 ? (
                  <Text type="secondary">없음</Text>
                ) : (
                  sets.map((s) => (
                    <Tag key={s.key} color="orange" style={{ marginBottom: 4 }}>
                      {s.label}
                    </Tag>
                  ))
                ),
            },
            {
              key: 'desc',
              label: '시나리오 설명',
              children: <Text type="secondary">{scenario.description}</Text>,
            },
          ]}
        />

        <Result
          status={result.allowed ? 'success' : 'error'}
          icon={result.allowed ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
          title={
            result.allowed ? (
              <>
                <Tag color="green" style={{ fontSize: 16, padding: '4px 12px' }}>
                  허용
                </Tag>{' '}
                — {entityLabel} 에 대한 {opLabel} 작업 가능
              </>
            ) : (
              <>
                <Tag color="red" style={{ fontSize: 16, padding: '4px 12px' }}>
                  차단
                </Tag>{' '}
                — {entityLabel} 에 대한 {opLabel} 작업 불가
              </>
            )
          }
          subTitle={
            result.allowed
              ? '아래 매칭 근거 중 하나라도 통과하면 허용입니다 (OR 합집합).'
              : '아래 평가 단계에서 매칭되는 권한이 없습니다.'
          }
        />

        <div>
          <Text strong>평가 근거 ({result.reasons.length}건)</Text>
          <Table
            size="small"
            pagination={false}
            rowKey={(_r, i) => String(i)}
            style={{ marginTop: 8 }}
            dataSource={result.reasons}
            columns={[
              { title: '출처', dataIndex: 'source', key: 'source', width: 320 },
              { title: '판정 이유', dataIndex: 'reason', key: 'reason' },
            ]}
          />
        </div>
      </Space>
    </>
  );
}

export default function PermissionGuidePage() {
  return (
    <div style={{ padding: 16, paddingRight: 340, maxWidth: 1540 }}>
      <Title level={3}>
        <BookOutlined /> 권한 시스템 사용 가이드
      </Title>
      <Paragraph type="secondary">
        본 시스템의 권한 모델은 <Text strong>Salesforce</Text> 의 프로파일 (Profile) + 권한 세트 (Permission Set) 구조를 그대로 사용합니다.
        본 가이드는 개념 → 실무 시나리오 → 자주 묻는 질문 순서로 설명합니다.
      </Paragraph>
      <Alert
        type="info"
        showIcon
        message="SF 사용 경험이 있는 분께"
        description={
          <>
            각 섹션 하단에 <Tag color="geekblue">☁ Salesforce 대응</Tag> 패널을 추가했습니다. 본 시스템의 용어/기능을 SF Setup 의 어떤 화면/메타와 매핑되는지 1:1 로 확인할 수 있습니다.
          </>
        }
        style={{ marginBottom: 16 }}
      />

      <div
        style={{
          position: 'fixed',
          right: 24,
          top: 100,
          width: 280,
          maxHeight: 'calc(100vh - 140px)',
          overflowY: 'auto',
          background: '#fff',
          border: '1px solid #f0f0f0',
          borderRadius: 8,
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
          padding: '12px 16px',
          zIndex: 100,
        }}
      >
        <Anchor
          affix={false}
          targetOffset={80}
          items={[
            { key: 'concept', href: '#concept', title: '1. 개념 — 프로파일 vs 권한 세트' },
            { key: 'bits', href: '#bits', title: '2. 시스템 권한 비트 5종' },
            { key: 'matrix', href: '#matrix', title: '3. 엔티티 × 조회/생성/수정/삭제 매트릭스' },
            { key: 'inventory-profile', href: '#inventory-profile', title: '4. 프로파일 인벤토리 (운영 실측)' },
            { key: 'inventory-ps', href: '#inventory-ps', title: '5. 권한 세트 인벤토리 (운영 실측)' },
            { key: 'combinations', href: '#combinations', title: '6. 프로파일 × 권한 세트 조합 케이스' },
            { key: 'simulator', href: '#simulator', title: '7. 권한 시뮬레이터 (실시간 평가)' },
            { key: 'workflow', href: '#workflow', title: '8. 실무 워크플로우' },
            { key: 'scenarios', href: '#scenarios', title: '9. 시나리오별 빠른 참조' },
            { key: 'faq', href: '#faq', title: '10. 자주 묻는 질문' },
          ]}
        />
      </div>

      <Divider />

      <Card id="concept" title="1. 개념 — 프로파일 vs 권한 세트" style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          showIcon
          message="핵심 한 줄"
          description={
            <>
              <Text strong>프로파일</Text> = 사용자 1명에 정확히 1개 (기본 권한). <Text strong>권한 세트</Text> = N개 가산 (옵션).
              실효 권한 = <Tag color="purple">프로파일 ∪ 모든 권한 세트</Tag> 의 합집합 (둘 중 하나라도 주면 통과).
            </>
          }
          style={{ marginBottom: 16 }}
        />

        <Title level={5}>비유</Title>
        <Paragraph>
          <Text strong>프로파일</Text> 은 "직급 카드" 입니다. 입사 시 1장만 받습니다. 이 카드 하나로 평소 업무에 필요한 권한이 다 들어있습니다.
          <br />
          <Text strong>권한 세트</Text> 는 "프로젝트 임시 출입증" 입니다. 직급 카드는 그대로 두고, 한시적으로 추가 권한이 필요할 때
          1장씩 더 발급해서 가산합니다.
        </Paragraph>

        <Title level={5}>왜 분리하나</Title>
        <Paragraph>
          모든 권한을 프로파일 만으로 관리하면 "5.영업사원_거래처수정가능" / "5.영업사원_거래처수정가능_행사조회가능" ... 처럼
          조합 수만큼 프로파일이 늘어납니다. 권한 세트로 가산하면 프로파일은 5~10개로 유지하고 가산 권한만 사용자별로 다르게 줄 수 있습니다.
        </Paragraph>

        <Title level={5}>예시</Title>
        <Table
          size="small"
          pagination={false}
          dataSource={[
            { key: 1, user: '홍영업 (영업사원)', profile: '5.영업사원', sets: '-', effect: '거래처 조회/수정, 매출 조회' },
            { key: 2, user: '김특판 (특판 담당)', profile: '5.영업사원', sets: '전문행사조 수정', effect: '거래처 조회/수정 + 행사 수정 추가' },
            { key: 3, user: '박관리 (운영)', profile: '시스템 관리자', sets: '-', effect: '전체 엔티티 조회/생성/수정/삭제 + 모든 데이터 수정 비트' },
          ]}
          columns={[
            { title: '사용자', dataIndex: 'user', key: 'user' },
            { title: '프로파일 (1개)', dataIndex: 'profile', key: 'profile', render: (v: string) => <Tag color="blue">{v}</Tag> },
            { title: '권한 세트 (N개)', dataIndex: 'sets', key: 'sets', render: (v: string) => v === '-' ? <Text type="secondary">-</Text> : <Tag color="orange">{v}</Tag> },
            { title: '실효 권한', dataIndex: 'effect', key: 'effect' },
          ]}
        />

        <SalesforceMapping
          intro={
            <>
              본 시스템의 권한 모델은 Salesforce 의 <Text strong>Profile + Permission Set</Text> 모델을 그대로 채택한 것이라 SF 사용자에게는 친숙합니다.
              가산 (OR 합집합) 시맨틱도 SF 와 동일합니다.
            </>
          }
          rows={[
            {
              ours: '프로파일',
              sfFeature: 'Profile',
              sfPath: '설정 → 사용자 → 프로파일',
              note: '사용자 1명에 정확히 1개. UserLicense + UserType 결정 + baseline 권한.',
            },
            {
              ours: '권한 세트',
              sfFeature: 'Permission Set',
              sfPath: '설정 → 사용자 → 권한 세트',
              note: '사용자에 가산 부여 (1 user : N permission sets). PermissionSetAssignment 로 매핑.',
            },
            {
              ours: '실효 권한 = 프로파일 ∪ 권한 세트 (OR)',
              sfFeature: 'SF 표준 평가 시맨틱',
              note: 'SF 도 동일하게 어느 한쪽이라도 부여되면 통과 (deny 비트 없음).',
            },
            {
              ours: '본 시스템에 없음',
              sfFeature: 'Permission Set Group (PSG)',
              sfPath: '설정 → 사용자 → 권한 세트 그룹',
              note: '운영 PSG 12개 모두 force__ 네임스페이스 (SF 표준 패키지) — 본 프로젝트 운영 권한 모델 외부.',
            },
          ]}
        />
      </Card>

      <Card id="bits" title="2. 시스템 권한 비트 5종" style={{ marginBottom: 16 }}>
        <Paragraph>
          프로파일과 권한 세트 모두 동일한 <Text strong>5개 시스템 권한 비트</Text> 를 가집니다. 이 비트는 엔티티 (테이블) 단위가 아니라
          <Text strong> 시스템 전체 권한</Text> 입니다.
        </Paragraph>
        <Table
          size="small"
          pagination={false}
          rowKey="bitEn"
          dataSource={systemBits}
          columns={[
            {
              title: '비트 (한글)',
              dataIndex: 'bit',
              key: 'bit',
              width: 160,
              render: (v: string) => <Tag color="red">{v}</Tag>,
            },
            {
              title: 'Salesforce 명',
              dataIndex: 'bitEn',
              key: 'bitEn',
              width: 180,
              render: (v: string) => <Text code>{v}</Text>,
            },
            { title: '의미', dataIndex: 'meaning', key: 'meaning' },
            { title: '대표 예시', dataIndex: 'example', key: 'example' },
          ]}
        />
        <Alert
          type="warning"
          showIcon
          icon={<ExclamationCircleOutlined />}
          message='"모든 데이터 수정" 은 가장 강력한 권한'
          description="모든 데이터 수정 비트를 가진 사용자는 소유자 / 공유 규칙 / 객체 권한을 모두 우회합니다. 시스템 관리자 + 운영팀에만 부여하세요. 본인이 본인의 모든 데이터 수정 부여를 회수하는 것은 시스템이 차단합니다 (자기 자신 잠금 방지)."
          style={{ marginTop: 12 }}
        />

        <SalesforceMapping
          intro={
            <>
              본 5개 비트는 Salesforce <Text strong>userPermissions</Text> (SF 의 시스템 권한 카탈로그 ~80여 개) 중 운영에 필수인 5개를 채택한 것입니다.
              SF 의 다른 userPermissions (ViewSetup, ManageRoles 등) 는 본 시스템 범위 밖.
            </>
          }
          rows={[
            { ours: '모든 데이터 조회', sfFeature: 'View All Data', sfPath: 'Profile/PermissionSet → 시스템 권한', note: 'SF 와 동일. Owner / Sharing Rules 우회 (조회만).' },
            { ours: '모든 데이터 수정', sfFeature: 'Modify All Data', sfPath: 'Profile/PermissionSet → 시스템 권한', note: 'SF 와 동일. Owner / Sharing / Object Permissions 전면 우회.' },
            { ours: '모든 사용자 조회', sfFeature: 'View All Users', sfPath: 'Profile/PermissionSet → 시스템 권한', note: 'SF 와 동일. User 객체에 대한 ViewAll 권한.' },
            { ours: '사용자 관리', sfFeature: 'Manage Users', sfPath: 'Profile/PermissionSet → 시스템 권한', note: 'SF 와 동일. User 생성/비활성화, 권한 부여/회수.' },
            { ours: 'API 사용', sfFeature: 'API Enabled', sfPath: 'Profile/PermissionSet → 시스템 권한', note: 'SF 와 동일. REST/SOAP API 호출 가능.' },
          ]}
          extra={
            <Alert
              type="info"
              showIcon
              message="SF 와의 차이"
              description="SF 의 userPermissions 는 약 80개 이상이며, 본 시스템은 5개로 한정. CustomPermission 도입은 향후 로드맵 (선행 인프라 작업 진행 중)."
            />
          }
        />
      </Card>

      <Card id="matrix" title="3. 엔티티 × 조회/생성/수정/삭제 매트릭스" style={{ marginBottom: 16 }}>
        <Paragraph>
          시스템 권한 비트와 별도로, <Text strong>각 엔티티 (테이블)</Text> 마다 4가지 작업 권한 (조회 / 생성 / 수정 / 삭제) 을 개별 부여합니다.
        </Paragraph>
        <Table
          size="small"
          pagination={false}
          rowKey="opEn"
          dataSource={crudOps}
          columns={[
            {
              title: '작업 (한글)',
              dataIndex: 'op',
              key: 'op',
              width: 110,
              render: (v: string) => <Tag color="cyan">{v}</Tag>,
            },
            {
              title: 'Salesforce 명',
              dataIndex: 'opEn',
              key: 'opEn',
              width: 130,
              render: (v: string) => <Text code>{v}</Text>,
            },
            { title: '예시 (거래처 엔티티)', dataIndex: 'example', key: 'example' },
          ]}
        />
        <Divider />
        <Title level={5}>실효 권한 계산 공식</Title>
        <Paragraph>
          <Text code>실효(엔티티, 작업) = 프로파일.시스템비트 (모든 데이터 수정 등 우회) OR 프로파일.엔티티권한 OR ∪ 모든 권한세트.엔티티권한</Text>
        </Paragraph>
        <Alert
          type="info"
          showIcon
          message="모든 데이터 수정 비트 우회 규칙"
          description="모든 데이터 수정 비트를 가진 프로파일 또는 권한 세트는 모든 엔티티의 모든 작업을 자동 통과합니다. 따라서 시스템 관리자 프로파일의 엔티티 권한 컬럼은 비어있어도 정상 동작합니다."
          style={{ marginTop: 12 }}
        />

        <SalesforceMapping
          intro={
            <>
              SF 의 <Text strong>objectPermissions</Text> 와 동일한 모델입니다. SF 의 6개 비트 (Read/Create/Edit/Delete/ViewAll/ModifyAll) 중 객체 단위 우회 비트인 ViewAllRecords / ModifyAllRecords 는 본 시스템이 시스템 단위 (모든 데이터 조회/수정) 로 통합 운영합니다.
            </>
          }
          rows={[
            { ours: '엔티티 × 작업 매트릭스', sfFeature: 'objectPermissions', sfPath: 'Profile/PermissionSet → 객체 설정', note: 'Object 단위 CRUD 비트. SF Setup 의 "View Permissions" 페이지가 본 시스템의 권한 매트릭스 화면에 해당.' },
            { ours: '조회 / 생성 / 수정 / 삭제', sfFeature: 'Read / Create / Edit / Delete', note: 'SF 와 정확히 동일.' },
            { ours: '본 시스템에 없음 (시스템 단위로 통합)', sfFeature: 'ViewAllRecords / ModifyAllRecords', sfPath: 'Profile → Object → View All / Modify All', note: 'SF 는 객체별 우회 비트를 따로 둠. 본 시스템은 "모든 데이터 조회/수정" 시스템 비트로 통합.' },
            { ours: '엔티티 카탈로그', sfFeature: 'sObject', note: '본 시스템은 JPA @Table(name) 의 snake_case 단수형이 SF API Name 에 1:1 대응 (예: account ↔ Account__c / employee ↔ Employee__c).' },
            { ours: '본 시스템에 없음', sfFeature: 'Field-Level Security (FLS)', sfPath: 'Profile/PermissionSet → 필드 수준 보안', note: 'SF 는 필드 단위 권한도 매트릭스 운영. 본 시스템은 향후 검토.' },
            { ours: '본 시스템에 없음', sfFeature: 'Sharing Rules', sfPath: '설정 → 공유 규칙', note: '레코드 단위 공유. 본 시스템은 Owner / 모든 데이터 조회 비트 2가지로 단순화.' },
          ]}
        />
      </Card>

      <Card id="inventory-profile" title="4. 프로파일 인벤토리 (오뚜기 운영 실측 — 사용자 지정 프로파일 18개)" style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          showIcon
          message="본 표는 Salesforce 레거시 운영 환경의 실제 프로파일 목록입니다"
          description="신규 사용자에게 어떤 프로파일을 줄지 결정할 때 본 표에서 직무에 맞는 기본 권한을 1개만 선택하세요. 모든 직급은 숫자 (1~12) + 직무명 형태로 운영에 들어있습니다."
          style={{ marginBottom: 16 }}
        />
        <Table
          size="small"
          pagination={false}
          rowKey={(r: LegacyProfileRow) => `${r.num}-${r.name}`}
          dataSource={legacyProfiles}
          columns={[
            { title: '#', dataIndex: 'num', key: 'num', width: 60, render: (v: string) => <Tag color="blue">{v}</Tag> },
            { title: '프로파일 명', dataIndex: 'name', key: 'name', width: 240, render: (v: string) => <Text strong>{v}</Text> },
            { title: '역할', dataIndex: 'role', key: 'role', width: 240 },
            { title: '데이터 범위 / 특징', dataIndex: 'scope', key: 'scope' },
          ]}
        />
        <Alert
          type="warning"
          showIcon
          message="프로파일 변형 케이스 주의"
          description={
            <>
              <Text strong>5.영업사원</Text> / <Text strong>5.영업사원(로그인 IP 대역 설정)</Text> 처럼 같은 직급이지만 보안 정책이 다른 변형이 존재합니다.
              <br />
              <Text strong>6.조장</Text> / <Text strong>6-1.조장</Text> / <Text strong>6.조장_test</Text> 도 유사 변형이며 운영에 들어있는 그대로 유지되어 있습니다.
              <br />
              <Text strong>7.영업사원 + 조장</Text> 은 프로파일이 1인 1개 원칙이라 5+6 결합이 필요한 사용자를 위한 별도 프로파일로 정의되었습니다.
            </>
          }
          style={{ marginTop: 12 }}
        />

        <SalesforceMapping
          intro={
            <>
              본 인벤토리는 오뚜기 SF org 의 <Text strong>IsCustom = TRUE</Text> Profile 을 그대로 가져온 것입니다.
              SF Setup → 사용자 → 프로파일 메뉴와 1:1 대응되며, 표준 Profile (Standard User, System Administrator 등) 은 본 시스템에 채택되지 않음.
            </>
          }
          rows={[
            { ours: '프로파일 1~12 + CEO/시스템 관리자/공장관계자/품질보증실', sfFeature: 'Profile (IsCustom=TRUE)', sfPath: '설정 → 사용자 → 프로파일', note: '본 시스템 18개는 모두 SF org 에 존재하는 사용자 지정 프로파일.' },
            { ours: '5.영업사원 / 5.영업사원(IP 대역 설정)', sfFeature: 'Profile + Login IP Ranges', sfPath: '프로파일 → 로그인 IP 범위', note: 'SF 의 동일 직급 프로파일을 IP 보안 정책 따라 분리 운영 (SF Best Practice).' },
            { ours: '7.영업사원 + 조장 별도 프로파일', sfFeature: 'Profile 1인 1개 제약 우회', note: 'SF 도 Profile 은 1 user : 1 profile. 겸직은 (1) 별도 Profile 신설 (현 운영) 또는 (2) Permission Set 가산 두 가지가 표준 패턴.' },
            { ours: '본 시스템에 없음', sfFeature: 'License-bound Profile', sfPath: 'UserLicense → Profile', note: 'SF 는 Profile 이 UserLicense + UserType 에 묶임. 본 시스템은 License 개념 없음.' },
          ]}
        />
      </Card>

      <Card id="inventory-ps" title="5. 권한 세트 인벤토리 (오뚜기 운영 실측 — 사용자 지정 권한 세트 49개 중 핵심 18개)" style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          showIcon
          message="가산 권한은 직무 특화 권한 세트로 부여합니다"
          description="대부분의 권한 세트는 'XXX 전체 조회' 패턴으로 '담당자 범위(소유자) 를 넘어서 전체를 보고 싶다' 는 요구에 대응합니다. 수정/삭제 권한 세트는 신중히 부여하세요."
          style={{ marginBottom: 16 }}
        />
        <Table
          size="small"
          pagination={false}
          rowKey="ps"
          dataSource={legacyPS}
          columns={[
            { title: '라벨 (한글)', dataIndex: 'label', key: 'label', width: 240, render: (v: string) => <Text strong>{v}</Text> },
            { title: 'Salesforce API 명', dataIndex: 'ps', key: 'ps', width: 280, render: (v: string) => <Tag color="orange">{v}</Tag> },
            { title: '용도', dataIndex: 'purpose', key: 'purpose' },
            { title: '대표 부여 대상', dataIndex: 'typicalUser', key: 'typicalUser', width: 220 },
          ]}
        />
        <Paragraph style={{ marginTop: 12 }} type="secondary">
          * Salesforce 표준 패키지 (force__ 네임스페이스) 인 <Text code>권한 세트 그룹</Text> 12개 (Sales, Field Service 등) 는 본 프로젝트 운영 권한 모델 외부이므로 본 표에서 제외했습니다.
        </Paragraph>

        <SalesforceMapping
          intro={
            <>
              본 인벤토리 49개 (핵심 18개 표기) 는 오뚜기 SF org 의 <Text strong>IsCustom = TRUE</Text> Permission Set 와 1:1 대응됩니다.
              API Name (예: <Text code>Acc_Permission</Text>) 은 SF API Name 그대로이며, 외부 시스템 (Heroku, SAP, BI) 의 권한 매핑 키로 사용됩니다.
            </>
          }
          rows={[
            { ours: 'XXX 전체 조회 패턴 다수', sfFeature: 'Object 단위 ViewAll Permission Set', note: 'SF 운영에서 "직급 카드(Profile) 는 owner-bound, 가산(Permission Set) 으로 ViewAll" 이 흔한 패턴.' },
            { ours: 'rehabilitation (한시 부여)', sfFeature: 'Time-bound Permission Set Assignment', sfPath: 'SetupAuditTrail + manual revoke', note: 'SF Enterprise+ 는 PermissionSet 부여에 만료일 지원. 본 시스템은 수동 회수로 운영.' },
            { ours: '본 시스템에 없음', sfFeature: 'Permission Set Group + Muting', sfPath: '설정 → 사용자 → 권한 세트 그룹', note: 'SF 는 권한 세트 묶음 + 일부 제외(Muting) 모델 제공. 본 시스템은 PSG 채택 안 함.' },
            { ours: 'API Name (예: Acc_Permission)', sfFeature: 'PermissionSet.Name (API Name)', note: 'SF API Name 을 외부 시스템 매핑 키로 그대로 사용. 변경 시 API contract 파괴 — 변경 금지.' },
            { ours: '한글 라벨 (예: 거래처 전체 조회)', sfFeature: 'PermissionSet.Label', note: 'SF Setup UI 표시명. 본 시스템 UI 도 동일 라벨 사용.' },
          ]}
        />
      </Card>

      <Card id="combinations" title="6. 프로파일 × 권한 세트 조합 케이스 (실제 사용 패턴 10종)" style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          showIcon
          message="실제 발생하는 모든 조합 패턴을 망라"
          description="(1) 프로파일 단독 충족 / (2) 프로파일 + 1개 가산 / (3) 프로파일 + 다중 가산 / (4) 한시적 가산 / (5) 가산 금지 정책 — 5가지 유형을 모두 포함합니다."
          style={{ marginBottom: 16 }}
        />
        <Table
          size="small"
          pagination={false}
          rowKey="user"
          dataSource={combinationCases}
          columns={[
            { title: '사용자 (직무)', dataIndex: 'user', key: 'user', width: 220, render: (v: string) => <Text strong>{v}</Text> },
            {
              title: '프로파일 (1개)',
              dataIndex: 'profile',
              key: 'profile',
              width: 160,
              render: (v: string) => <Tag color="blue">{v}</Tag>,
            },
            {
              title: '권한 세트 가산',
              dataIndex: 'sets',
              key: 'sets',
              width: 320,
              render: (v: string) => {
                if (v === '-') return <Text type="secondary">없음</Text>;
                return v.split(' + ').map((s) => <Tag key={s} color="orange" style={{ marginBottom: 4 }}>{s}</Tag>);
              },
            },
            { title: '실효 권한', dataIndex: 'effect', key: 'effect' },
            { title: '선택 이유', dataIndex: 'why', key: 'why' },
          ]}
        />

        <SalesforceMapping
          intro={
            <>
              "1 user : 1 Profile + N Permission Sets" 가산 모델 자체는 SF 와 동일. 본 조합 케이스는 오뚜기 운영의 실제 사용 패턴이며, SF Best Practice 의 "Profile 은 최소화, Permission Set 으로 차이 부여" 원칙을 따릅니다.
            </>
          }
          rows={[
            { ours: '신입 영업사원 (단독)', sfFeature: 'Profile-only User', note: 'SF 도 baseline Profile 만으로 충분한 경우 PermissionSet 미부여.' },
            { ours: '특판 영업 (1개 가산)', sfFeature: 'Profile + 1 PermissionSet', note: 'SF 최빈 패턴. PermissionSetAssignment row 1개 추가.' },
            { ours: '행사 기획 (다중 가산)', sfFeature: 'Profile + N PermissionSets', note: '직무 특화 PS 여러 개 합산. SF 에서 PermissionSetGroup 으로 묶는 게 권장이지만 본 시스템은 개별 부여.' },
            { ours: '인사 복직 처리 (한시 가산)', sfFeature: 'Time-limited Assignment', note: 'SF Enterprise+ 는 만료일 자동 회수. 본 시스템은 수동 회수 운영.' },
            { ours: '마케팅 인턴 (가산 금지 정책)', sfFeature: 'Governance Policy', note: 'SF 자체에는 "이 Profile 은 PS 가산 금지" 라는 강제 기능 없음. 감사/운영 정책으로 통제.' },
          ]}
        />
      </Card>

      <Card
        id="simulator"
        title={
          <>
            <ExperimentOutlined /> 7. 권한 시뮬레이터 (실시간 평가)
          </>
        }
        style={{ marginBottom: 16 }}
      >
        <PermissionSimulator />

        <SalesforceMapping
          intro={
            <>
              SF Setup 에는 본 시뮬레이터에 정확히 대응되는 단일 화면이 없습니다. 가장 유사한 것은 <Text strong>"Permission Set Group Comparison"</Text> (PSG 비교 도구) 이며, 실효 권한 확인에는 보통 SF Workbench / Schema Builder / 개발자 콘솔의 SOQL <Text code>SELECT ... FROM UserRecordAccess</Text> 같은 별도 도구가 필요합니다.
            </>
          }
          rows={[
            { ours: '권한 시뮬레이터 (실시간 평가)', sfFeature: 'Permission Set Group Comparison + UserRecordAccess SOQL', sfPath: '설정 → 사용자 → 권한 세트 그룹 → 비교', note: 'SF 는 PSG 단위 비교만 가능. 본 시뮬레이터는 "프로파일 + N개 PS" 전체 가산 합집합을 보여주므로 SF 보다 직관적.' },
            { ours: '평가 근거 테이블 (출처별 추적)', sfFeature: '~ Salesforce Optimizer 의 Permission Analysis', note: 'SF Optimizer 는 "이 사용자가 왜 이 권한을 가지나" 를 출처별로 보여줌. 본 시뮬레이터의 평가 근거와 동일한 콘셉트.' },
            { ours: '본 시뮬레이터는 표본 평가', sfFeature: 'Real User 평가 (Setup UI)', note: '본 시뮬레이터는 SF 레거시 추정 모델 기반. 실제 운영 사용자별 평가는 사원 현황 → 해당 사원 → 권한 섹션 (백엔드 SfPermissionResolver 호출).' },
          ]}
        />
      </Card>

      <Card id="workflow" title="8. 실무 워크플로우" style={{ marginBottom: 16 }}>
        <Title level={5}>8-1. 신규 사용자에게 권한 부여</Title>
        <Steps
          direction="vertical"
          size="small"
          current={-1}
          items={[
            { title: '사용자 관리 → 신규 등록', description: '사번 / 이름 / 이메일 입력' },
            { title: '프로파일 선택', description: '직무에 맞는 기본 프로파일 1개 (예: "5.영업사원")' },
            { title: '필요 시 권한 세트 가산', description: '사원 현황 → 해당 사원 → "권한" → "권한 세트 부여 추가"' },
            { title: '사용자 재로그인', description: '권한 변경은 다음 로그인 시점부터 적용됩니다' },
          ]}
        />

        <Divider />

        <Title level={5}>8-2. 권한 회수</Title>
        <Steps
          direction="vertical"
          size="small"
          current={-1}
          items={[
            { title: '사원 현황 → 해당 사원 진입', description: '"권한" 섹션 펼침' },
            { title: '회수할 권한 세트 우측 "회수" 버튼', description: '본인의 모든 데이터 수정 비트 회수는 시스템이 차단' },
            { title: '확인 모달에서 사유 입력 후 회수', description: '회수 이력은 감사 로그에 자동 기록' },
          ]}
        />

        <Divider />

        <Title level={5}>8-3. 프로파일 자체 변경</Title>
        <Alert
          type="warning"
          showIcon
          message="프로파일 변경은 권한 영향 범위가 큽니다"
          description="프로파일은 기본 권한이므로 변경 시 해당 사용자의 모든 엔티티 권한이 한 번에 바뀝니다. 가능하면 권한 세트 가산/회수로 해결하고, 프로파일 변경은 직무 자체가 바뀐 경우에만 사용하세요."
          style={{ marginTop: 12 }}
        />

        <SalesforceMapping
          intro={
            <>
              본 시스템의 부여/회수는 SF 의 PermissionSetAssignment CRUD 와 직접 대응합니다. SF Setup 에서 익숙한 워크플로우라면 본 시스템도 동일하게 동작합니다.
            </>
          }
          rows={[
            { ours: '권한 세트 부여', sfFeature: 'PermissionSetAssignment INSERT', sfPath: '설정 → 사용자 → 사용자 상세 → 권한 세트 할당', note: 'SF UI 의 "Edit Assignments" → 추가와 동일.' },
            { ours: '권한 세트 회수 (soft delete: is_active=false)', sfFeature: 'PermissionSetAssignment DELETE', sfPath: 'SF 는 hard delete', note: 'SF 는 row 자체 삭제. 본 시스템은 회수 이력 보존을 위해 soft delete + 감사 로그.' },
            { ours: '권한 변경 → 다음 로그인 시점 반영', sfFeature: 'SF 는 즉시 반영 (다음 요청)', note: 'SF 는 session 갱신 시 즉시. 본 시스템은 JWT 클레임에 박혀있어 재로그인 필요.' },
            { ours: 'self-revoke 가드 (본인 모든 데이터 수정 회수 차단)', sfFeature: 'SF 표준에는 없는 안전장치', note: 'SF 는 본인이 본인 권한 회수 가능 (사고 위험). 본 시스템은 명시적으로 차단.' },
            { ours: 'last-admin 가드 (사용자 관리 보유자 0명 방지)', sfFeature: 'SF 표준에는 없는 안전장치', note: '운영자 영구 잠금 사태 방지 — 본 시스템 자체 추가.' },
            { ours: '프로파일 변경 = 한 번에 baseline 교체', sfFeature: 'User.ProfileId UPDATE', sfPath: '설정 → 사용자 → 프로파일 선택', note: 'SF 와 동일. 영향 범위가 커서 신중 권장.' },
            { ours: '권한 메타 (PS/Profile 자체) 수정/삭제 불가', sfFeature: 'SF Setup 에서는 가능 (관리자 한정)', note: '본 시스템은 SF 마스터를 SoT 로 두고 정의 수정은 DB 마이그레이션 PR 로 통제. 자세한 우려는 별도 안내 참조.' },
          ]}
        />
      </Card>

      <Card id="scenarios" title="9. 시나리오별 빠른 참조" style={{ marginBottom: 16 }}>
        <Table
          size="small"
          pagination={false}
          rowKey="who"
          dataSource={scenarios}
          columns={[
            { title: '상황', dataIndex: 'who', key: 'who', width: 220, render: (v: string) => <Text strong>{v}</Text> },
            { title: '목표', dataIndex: 'goal', key: 'goal' },
            {
              title: '메뉴 경로',
              dataIndex: 'how',
              key: 'how',
              render: (v: string) => <Tag icon={<CheckCircleOutlined />} color="green">{v}</Tag>,
            },
          ]}
        />

        <SalesforceMapping
          intro={
            <>
              본 시나리오의 각 단계 메뉴 경로는 SF Setup 의 다음 화면들에 대응됩니다.
            </>
          }
          rows={[
            { ours: '사용자 관리 → 신규 등록', sfFeature: 'New User', sfPath: '설정 → 사용자 → 새 사용자' },
            { ours: '사원 현황 → 해당 사원 → 권한 섹션', sfFeature: 'User Detail → Permission Set Assignments', sfPath: '설정 → 사용자 → 사용자 상세 → 권한 세트 할당' },
            { ours: '권한 매트릭스', sfFeature: 'Object Manager → View Permissions', sfPath: '설정 → 객체 관리자 → 객체 → 권한 보기', note: 'SF 는 객체별 별도 화면. 본 시스템은 전 객체 한 화면.' },
            { ours: '권한 세트 관리 → 상세', sfFeature: 'PermissionSet Detail', sfPath: '설정 → 사용자 → 권한 세트 → 권한 세트 선택' },
            { ours: '실효 엔티티 매트릭스 진단 (403 트러블슈팅)', sfFeature: 'User → "Permissions" 탭 + UserRecordAccess SOQL', note: 'SF 도 동일 진단 흐름. 본 시스템은 한 화면에서 합집합 표시.' },
          ]}
        />
      </Card>

      <Card id="faq" title="10. 자주 묻는 질문" style={{ marginBottom: 16 }}>
        <Collapse
          defaultActiveKey={['q1']}
          items={[
            {
              key: 'q1',
              label: <span><QuestionCircleOutlined /> 프로파일과 권한 세트의 권한이 충돌하면?</span>,
              children: (
                <Paragraph>
                  충돌이라는 개념이 없습니다. 둘 다 <Text strong>가산 (둘 중 하나라도 주면 통과)</Text> 이라 어느 한쪽이라도 권한을 주면 통과합니다.
                  권한을 빼고 싶으면 부여하지 않으면 됩니다 — "비활성" 같은 부정 비트는 없습니다.
                </Paragraph>
              ),
            },
            {
              key: 'q2',
              label: <span><QuestionCircleOutlined /> 권한 변경이 즉시 반영되지 않습니다</span>,
              children: (
                <Paragraph>
                  권한은 JWT 토큰에 박혀있으므로 <Text strong>다음 로그인 시점</Text> 부터 반영됩니다.
                  즉시 반영이 필요하면 해당 사용자에게 로그아웃 후 재로그인을 요청하세요.
                  <br />또한 권한 매트릭스 화면은 운영 진단용 <Text strong>5분 캐시</Text> 입니다 — 부여/회수 직후엔 매트릭스가 잠시 이전 데이터로 보일 수 있습니다.
                </Paragraph>
              ),
            },
            {
              key: 'q3',
              label: <span><QuestionCircleOutlined /> 시스템 관리자 프로파일은 왜 엔티티 권한 컬럼이 비어있나요?</span>,
              children: (
                <Paragraph>
                  시스템 관리자 프로파일은 <Tag color="red">모든 데이터 수정</Tag> 비트를 가지고 있어
                  모든 엔티티의 모든 작업을 자동 통과합니다. 따라서 엔티티 권한 매트릭스를 별도로 채우지 않아도 동작합니다.
                </Paragraph>
              ),
            },
            {
              key: 'q4',
              label: <span><QuestionCircleOutlined /> 영업사원이 본인 거래처가 아닌 다른 거래처를 못 봅니다</span>,
              children: (
                <Paragraph>
                  정상 동작입니다. 영업사원 프로파일은 <Tag color="red">모든 데이터 조회</Tag> 비트가 없어 본인이 <Text strong>소유자</Text> 인
                  레코드만 볼 수 있습니다. 모든 거래처 조회가 필요하면 <Tag color="orange">거래처 전체 조회</Tag> 권한 세트를
                  가산하거나, 직무가 분석 담당이라면 프로파일 변경을 검토하세요.
                </Paragraph>
              ),
            },
            {
              key: 'q5',
              label: <span><QuestionCircleOutlined /> 권한 매트릭스에 권한 세트는 왜 안 보이나요?</span>,
              children: (
                <Paragraph>
                  본 매트릭스는 프로파일 단위 <Text strong>합집합</Text> 표시 (프로파일 + 해당 프로파일 표본 사용자의 권한 세트 영향 포함) 입니다.
                  권한 세트별 분해가 필요하면 <Text code>권한 세트 관리 → 해당 권한 세트 → 상세</Text> 화면의 엔티티 매트릭스를 사용하세요.
                </Paragraph>
              ),
            },
            {
              key: 'q6',
              label: <span><QuestionCircleOutlined /> 누가 무슨 권한 세트를 받았는지 한눈에 보고 싶습니다</span>,
              children: (
                <Paragraph>
                  <Text code>권한 세트 관리 → 해당 권한 세트 행 클릭</Text> 하면 상세 페이지 하단에 <Text strong>부여된 사용자 일람</Text> 이 표시됩니다.
                  반대로 <Text code>사원 현황 → 해당 사원 → 권한 섹션</Text> 에서 한 사용자의 부여된 권한 세트 일람을 볼 수도 있습니다.
                </Paragraph>
              ),
            },
          ]}
        />

        <SalesforceMapping
          title="SF 사용자가 자주 묻는 차이점"
          intro={
            <>
              SF 사용 경험이 있는 분이 본 시스템을 처음 쓸 때 가장 헷갈리는 부분을 정리했습니다.
            </>
          }
          rows={[
            { ours: '권한 변경이 다음 로그인부터 반영', sfFeature: 'SF 는 즉시 반영', note: 'SF 는 session 갱신 시. 본 시스템은 JWT 클레임에 박혀있어 재로그인 필요. 즉시 반영 필요 시 사용자에게 로그아웃 요청.' },
            { ours: '권한 매트릭스 5분 캐시', sfFeature: 'SF 는 실시간', note: 'SF Setup 의 View Permissions 는 캐시 없음. 본 시스템은 운영 부하 절감 목적.' },
            { ours: 'PermissionSetGroup 미사용', sfFeature: 'SF 권장 패턴', note: '본 시스템은 PSG 채택 안 함 — PS 를 개별 부여. 운영 PSG 12개는 force__ 네임스페이스 (외부).' },
            { ours: 'CustomPermission 미지원', sfFeature: 'SF 표준', note: 'SF 는 객체와 무관한 가상 자원 (Apex Page Access 등) 을 CustomPermission 으로 표현. 본 시스템은 향후 로드맵.' },
            { ours: 'Field-Level Security (FLS) 미지원', sfFeature: 'SF 표준', note: '본 시스템은 엔티티 단위 CRUD 만 운영.' },
            { ours: 'Sharing Rules / Role Hierarchy 미지원', sfFeature: 'SF 표준', note: '본 시스템은 Owner + 모든 데이터 조회 비트 2가지로 단순화. 조직 계층 공유는 도메인별 별도 처리.' },
            { ours: 'Profile/PermissionSet 정의 자체 수정/삭제 불가', sfFeature: 'SF Setup 에서 가능', note: 'SF 운영자는 자유 편집. 본 시스템은 SoT 통제를 위해 DB 마이그레이션 PR 채널로만 변경.' },
            { ours: 'self-revoke / last-admin 가드 자동', sfFeature: 'SF 표준에는 없음', note: 'SF 는 본인이 본인 권한 회수 가능 (사고 위험). 본 시스템 자체 추가 안전장치.' },
          ]}
        />
      </Card>

      <Alert
        type="success"
        showIcon
        icon={<BulbOutlined />}
        message="실무 팁"
        description="권한 변경 작업 전 권한 매트릭스 스냅샷을 확인하고, 변경 후 같은 매트릭스로 결과를 검증하는 워크플로우를 권장합니다. 권한 변경은 audit 로그에 자동 기록됩니다."
      />
    </div>
  );
}
