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

const ENTITIES = [
  { value: 'account', label: '거래처 (account)' },
  { value: 'employee', label: '사원 (employee)' },
  { value: 'promotion', label: '행사 (promotion)' },
  { value: 'team_member_schedule', label: '여사원일정 (team_member_schedule)' },
  { value: 'claim', label: '클레임 (claim)' },
  { value: 'monthly_sales_history', label: '월매출 (monthly_sales_history)' },
  { value: 'attendance_log', label: '근무 등록 (attendance_log)' },
  { value: 'notice_post', label: '공지사항 (notice_post)' },
  { value: 'product', label: '제품 (product)' },
  { value: 'upload_file', label: '업로드 파일 (upload_file)' },
  { value: 'user', label: '사용자 (user)' },
];

const OPERATIONS: Array<{ value: Op; label: string }> = [
  { value: 'READ', label: '조회' },
  { value: 'CREATE', label: '생성' },
  { value: 'EDIT', label: '수정' },
  { value: 'DELETE', label: '삭제' },
];

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
