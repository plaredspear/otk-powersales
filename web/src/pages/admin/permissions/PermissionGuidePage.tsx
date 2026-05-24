import { Alert, Anchor, Card, Collapse, Divider, Steps, Table, Tag, Typography } from 'antd';
import { BookOutlined, BulbOutlined, CheckCircleOutlined, ExclamationCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons';

const { Title, Paragraph, Text } = Typography;

interface BitRow {
  bit: string;
  meaning: string;
  example: string;
}

interface CrudRow {
  op: string;
  desc: string;
  example: string;
}

interface ScenarioRow {
  who: string;
  goal: string;
  how: string;
}

const systemBits: BitRow[] = [
  { bit: 'VIEW_ALL_DATA', meaning: '모든 데이터 조회 가능 (Owner / Sharing 무시)', example: '전사 매출 통계 분석 담당자' },
  { bit: 'MODIFY_ALL_DATA', meaning: '모든 데이터 수정/삭제 가능 (Owner / Sharing 무시)', example: '시스템 관리자' },
  { bit: 'VIEW_ALL_USERS', meaning: '모든 User 레코드 조회 가능 (조직 외부 포함)', example: '인사팀 조회 권한' },
  { bit: 'MANAGE_USERS', meaning: '사용자 계정 생성/비활성화, 권한 부여/회수', example: '운영팀 / 시스템 관리자' },
  { bit: 'API_ENABLED', meaning: 'REST API 직접 호출 가능 (모바일 앱 로그인 등)', example: '모든 영업사원' },
];

const crudOps: CrudRow[] = [
  { op: 'READ', desc: '조회', example: '거래처 목록 보기' },
  { op: 'CREATE', desc: '신규 생성', example: '신규 거래처 등록' },
  { op: 'EDIT', desc: '수정', example: '기존 거래처 정보 변경' },
  { op: 'DELETE', desc: '삭제', example: '거래처 레코드 삭제' },
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
    goal: '"5.영업사원" Profile 만 부여 → 본인 담당 거래처 CRUD',
    how: '사용자 관리 → 신규 등록 → Profile = "5.영업사원" 선택',
  },
  {
    who: '신입 영업사원이 본인 외 거래처도 봐야 함',
    goal: 'Profile 변경 없이 거래처 전체 조회 가능',
    how: '사원 현황 → 해당 사원 → SF 권한 → "Acc_Permission" PermissionSet 부여',
  },
  {
    who: '특정 조장에게 전문행사조 EDIT 권한',
    goal: 'Profile (6.조장) 그대로 두고 행사조 수정만 가산',
    how: 'PermissionSet 관리 → "ProfessionalPromotionTeam" 선택 → 사용자 추가 → 사번 선택',
  },
  {
    who: '영업지원실 신규 입사자',
    goal: '영업 전반 read + 일부 입력 (지원실 역할)',
    how: 'Profile = "9.Staff" + PermissionSet "SalesAssistant" 가산',
  },
  {
    who: '여사원 복직 처리',
    goal: '복직 처리 권한 한시적 부여',
    how: 'PermissionSet 관리 → "rehabilitation" 부여 → 복직 처리 완료 후 회수',
  },
  {
    who: '신규 마케팅(인턴) 입사',
    goal: '"11.마케팅(인턴)" Profile 만 부여 → 제한된 마케팅 read',
    how: '사용자 관리 → 신규 등록 → Profile = "11.마케팅(인턴)" 선택',
  },
  {
    who: '특정 영업사원이 클레임 전체 조회 필요',
    goal: '본인 담당 외 클레임도 read',
    how: 'PermissionSet "Claim_View_All" 가산 (담당 외 클레임 조회 가능)',
  },
  {
    who: '특정 운영자에게 업로드 파일 삭제 권한',
    goal: '파일 업로드는 다수 가능하지만 삭제는 한정',
    how: 'PermissionSet "Uploadfile_Create_Delete_Permission" 가산',
  },
  {
    who: '운영팀이 권한 검토',
    goal: 'Profile 별로 어떤 entity 의 어떤 CRUD 가 열려있는지 한눈에 확인',
    how: '권한 매트릭스 → entity 검색 / Profile 컬럼 비교',
  },
  {
    who: '직원이 특정 화면에 진입 못 함 (403)',
    goal: '어떤 권한이 누락되었는지 진단',
    how: '사원 현황 → 해당 직원 → "SF 권한" 섹션 → 실효 entity × CRUD 매트릭스 확인',
  },
  {
    who: '경영진 (CEO) 신규 추가',
    goal: '전체 데이터 read + 일부 write',
    how: 'Profile = "CEO" 선택 (VIEW_ALL_DATA 자동 포함)',
  },
  {
    who: '시스템 관리자 권한 위임',
    goal: '운영팀 일원에게 전체 권한 부여',
    how: 'Profile = "시스템 관리자" (MODIFY_ALL_DATA + MANAGE_USERS 자동 포함)',
  },
];

// SF 운영 IsCustom=TRUE Profile 인벤토리 (docs/plan/old_source_260516/aladdin_260516_prod/force-app/main/default/profiles/)
// PSG 12개는 force__ namespace (SF 표준 패키지) 라 운영 IsCustom=TRUE PS 49개와 교집합 0 — 본 표 제외
const legacyProfiles: LegacyProfileRow[] = [
  { num: '1', name: '본부장', role: '영업본부 총괄', scope: '본부 전체 데이터 + 승인 권한' },
  { num: '2', name: '사업부장', role: '사업부 총괄', scope: '사업부 하위 데이터 + 승인' },
  { num: '3', name: '영업부장', role: '영업부 총괄', scope: '영업부 하위 데이터 + 승인' },
  { num: '4', name: '지점장', role: '지점 단위 관리', scope: '지점 소속 사원/거래처 관리' },
  { num: '5', name: '영업사원', role: '담당 거래처 영업', scope: '본인 owner 데이터만 (VIEW_ALL 없음)' },
  { num: '5', name: '영업사원(로그인ip대역설정)', role: '영업사원 변형 (IP 제한)', scope: '5.영업사원 + 로그인 IP 화이트리스트' },
  { num: '6', name: '조장', role: '조 단위 관리', scope: '소속 조원 일정 + 본인 데이터' },
  { num: '6-1', name: '조장', role: '6.조장 변형', scope: '서브 조장 (일부 권한 차등)' },
  { num: '7', name: '영업사원 + 조장', role: '영업사원 + 조장 겸임', scope: '5 + 6 결합 (Profile 단일이라 별도 정의)' },
  { num: '8', name: '마케팅', role: '마케팅 일반', scope: '마케팅 데이터 + 일부 영업 read' },
  { num: '9', name: 'Staff', role: '지원 인력 (영업지원실 등)', scope: '제한된 read + Staff 가산 PS 필수' },
  { num: '10', name: '마케팅(super)', role: '마케팅 슈퍼유저', scope: '마케팅 전체 + 일부 운영' },
  { num: '11', name: '마케팅(인턴)', role: '마케팅 인턴', scope: '제한된 read only' },
  { num: '12', name: '마케팅(팀장이상)', role: '마케팅 팀장+', scope: '마케팅 전체 EDIT' },
  { num: '-', name: 'CEO', role: '경영진', scope: '전체 데이터 read' },
  { num: '-', name: 'Admin', role: '시스템 관리자', scope: 'MODIFY_ALL_DATA + MANAGE_USERS' },
  { num: '-', name: '공장관계자', role: '공장/생산 부서', scope: '공장 출하/생산 데이터' },
  { num: '-', name: '품질보증실', role: '품질 검사', scope: '품질/클레임 read + 일부 EDIT' },
];

// SF 운영 IsCustom=TRUE PermissionSet 인벤토리 (운영자에게 의미 있는 18개)
const legacyPS: LegacyPsRow[] = [
  { ps: 'Acc_Permission', label: '거래처 View All', purpose: '본인 담당 외 거래처도 조회', typicalUser: '지점장 / 부장급 / 특판 담당' },
  { ps: 'Sales_User', label: 'Sales User', purpose: 'SF Sales 표준 권한 묶음', typicalUser: '영업 전반' },
  { ps: 'Activity_View_All', label: '현장점검결과 View All', purpose: '담당 외 현장점검 결과 조회', typicalUser: '점검 총괄' },
  { ps: 'Object_View_All', label: 'Object View All', purpose: '특정 객체 전체 read', typicalUser: '데이터 분석' },
  { ps: 'ProfessionalPromotionTeam', label: '전문행사조 수정 권한', purpose: '전문행사조 마스터 EDIT', typicalUser: '행사 기획 담당' },
  { ps: 'SalesAssistant', label: '영업지원실', purpose: '영업지원실 추가 권한', typicalUser: '9.Staff + 지원실 인력' },
  { ps: 'SalesSupport', label: '9.Staff_지원', purpose: '9.Staff Profile 보강', typicalUser: '9.Staff 전원' },
  { ps: 'notice', label: '공지사항 (회사, 교육)', purpose: '공지/교육 EDIT', typicalUser: '운영 / 교육 담당' },
  { ps: 'Promotion_Master_View_All', label: '행사마스터 View All', purpose: '담당 외 행사 마스터 read', typicalUser: '본부장 / 행사 총괄' },
  { ps: 'Employee_View_All', label: '사원 View All', purpose: '담당 외 사원 정보 read', typicalUser: '인사 / 지점장' },
  { ps: 'Claim_View_All', label: '클레임 View All', purpose: '담당 외 클레임 read', typicalUser: '품질보증실 / 본부' },
  { ps: 'Uploadfile_Create_Delete_Permission', label: '업로드파일 생성/삭제', purpose: '파일 삭제 권한 한정', typicalUser: '운영 / 파일 관리자' },
  { ps: 'View_All_Edit_All', label: '현장점검등록 View/Edit All', purpose: '점검 전체 EDIT', typicalUser: '점검 책임자' },
  { ps: 'View_ALL_EVENT', label: '행사사원 View All', purpose: '담당 외 행사 배치 read', typicalUser: '행사 총괄' },
  { ps: 'View_All_TeamMemberSchedule', label: '여사원일정 View All', purpose: '담당 외 일정 read', typicalUser: '본부 / 행사 총괄' },
  { ps: 'rehabilitation', label: '여사원 복직 권한', purpose: '복직 처리 한시 권한', typicalUser: '인사 (복직 처리 시점에만)' },
  { ps: 'SalesProgressViewAll', label: '거래처목표마스터 전체 조회', purpose: '목표 진척 분석', typicalUser: '경영 분석 / 본부장' },
  { ps: 'SalesDiary_View_All', label: '영업일지 View All', purpose: '담당 외 영업일지 read', typicalUser: '지점장 / 본부' },
];

// Profile × PermissionSet 조합 케이스 (가능한 모든 조합 패턴 망라)
const combinationCases: CombinationRow[] = [
  {
    user: '홍영업 (신입 영업사원)',
    profile: '5.영업사원',
    sets: '-',
    effect: '본인 owner 거래처/매출만 CRUD',
    why: 'Profile 만으로 baseline 충족 — 가산 불요',
  },
  {
    user: '김특판 (특판 영업)',
    profile: '5.영업사원',
    sets: 'Acc_Permission',
    effect: 'baseline + 전체 거래처 조회',
    why: '담당 외 거래처 조회 필요 → 1개 PS 가산',
  },
  {
    user: '박조장 (영업 + 조장)',
    profile: '7.영업사원+조장',
    sets: 'View_All_TeamMemberSchedule',
    effect: 'baseline + 본인 조 외 일정 read',
    why: '조 간 일정 조정 필요',
  },
  {
    user: '이지원 (영업지원실)',
    profile: '9.Staff',
    sets: 'SalesAssistant + SalesSupport',
    effect: 'baseline + 영업 지원 보조 + 9.Staff 보강',
    why: '9.Staff Profile 자체가 최소 권한 → 가산 필수',
  },
  {
    user: '정행사 (행사 기획)',
    profile: '5.영업사원',
    sets: 'ProfessionalPromotionTeam + Promotion_Master_View_All + View_ALL_EVENT',
    effect: 'baseline + 전문행사조 EDIT + 행사 마스터 read + 행사 배치 read',
    why: '직무 특화 다중 가산',
  },
  {
    user: '최품질 (품질보증실)',
    profile: '품질보증실',
    sets: 'Claim_View_All',
    effect: '품질 baseline + 전체 클레임 조회',
    why: '품질 분석 시 담당 외 클레임도 필요',
  },
  {
    user: '강분석 (경영 분석)',
    profile: 'CEO',
    sets: 'SalesProgressViewAll + SalesDiary_View_All',
    effect: 'CEO read + 목표 진척 + 영업일지 분석',
    why: 'VIEW_ALL_DATA 있어도 일부 entity 는 PS 가산 필요',
  },
  {
    user: '윤복직 (인사 복직 처리)',
    profile: '5.영업사원',
    sets: 'rehabilitation (1주일 한정)',
    effect: 'baseline + 복직 처리 권한',
    why: '한시 권한 — 처리 완료 후 회수',
  },
  {
    user: '서운영 (운영팀)',
    profile: 'Admin',
    sets: '-',
    effect: '전체 entity CRUD + MODIFY_ALL_DATA + MANAGE_USERS',
    why: 'Admin Profile 단일로 충족 — 가산 의미 없음',
  },
  {
    user: '한인턴 (마케팅 인턴)',
    profile: '11.마케팅(인턴)',
    sets: '-',
    effect: '제한된 read only',
    why: '인턴은 가산 금지 (감사 정책)',
  },
];

export default function PermissionGuidePage() {
  return (
    <div style={{ padding: 16, maxWidth: 1200 }}>
      <Title level={3}>
        <BookOutlined /> 권한 시스템 사용 가이드
      </Title>
      <Paragraph type="secondary">
        본 시스템의 권한 모델은 <Text strong>Salesforce</Text> 의 Profile + Permission Set 구조를 그대로 사용합니다.
        본 가이드는 개념 → 실무 시나리오 → FAQ 순서로 설명합니다.
      </Paragraph>

      <Anchor
        affix
        targetOffset={80}
        items={[
          { key: 'concept', href: '#concept', title: '1. 개념 — Profile vs PermissionSet' },
          { key: 'bits', href: '#bits', title: '2. 시스템 비트 5종' },
          { key: 'matrix', href: '#matrix', title: '3. Entity × CRUD 매트릭스' },
          { key: 'inventory-profile', href: '#inventory-profile', title: '4. Profile 인벤토리 (운영 실측)' },
          { key: 'inventory-ps', href: '#inventory-ps', title: '5. PermissionSet 인벤토리 (운영 실측)' },
          { key: 'combinations', href: '#combinations', title: '6. Profile × PermissionSet 조합 케이스' },
          { key: 'workflow', href: '#workflow', title: '7. 실무 워크플로우' },
          { key: 'scenarios', href: '#scenarios', title: '8. 시나리오별 빠른 참조' },
          { key: 'faq', href: '#faq', title: '9. FAQ' },
        ]}
        style={{ position: 'fixed', right: 24, top: 100, width: 280 }}
      />

      <Divider />

      <Card id="concept" title="1. 개념 — Profile vs PermissionSet" style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          showIcon
          message="핵심 한 줄"
          description={
            <>
              <Text strong>Profile</Text> = 사용자 1명에 정확히 1개 (baseline). <Text strong>PermissionSet</Text> = N개 가산 (옵션).
              실효 권한 = <Tag color="purple">Profile ∪ 모든 PermissionSet</Tag> 의 합집합 (OR).
            </>
          }
          style={{ marginBottom: 16 }}
        />

        <Title level={5}>비유</Title>
        <Paragraph>
          <Text strong>Profile</Text> 은 "직급 카드" 입니다. 입사 시 1장만 받습니다. 이 카드 하나로 평소 업무에 필요한 권한이 다 들어있습니다.
          <br />
          <Text strong>PermissionSet</Text> 은 "프로젝트 임시 출입증" 입니다. 직급 카드는 그대로 두고, 한시적으로 추가 권한이 필요할 때
          1장씩 더 발급해서 가산합니다.
        </Paragraph>

        <Title level={5}>왜 분리하나</Title>
        <Paragraph>
          모든 권한을 Profile 만으로 관리하면 "5.영업사원_거래처편집가능" / "5.영업사원_거래처편집가능_행사조회가능" ... 처럼
          조합 수만큼 Profile 이 늘어납니다. PermissionSet 으로 가산하면 Profile 은 5~10개로 유지하고 가산 권한만 사용자별로 다르게 줄 수 있습니다.
        </Paragraph>

        <Title level={5}>예시</Title>
        <Table
          size="small"
          pagination={false}
          dataSource={[
            { key: 1, user: '홍영업 (영업사원)', profile: '5.영업사원', sets: '-', effect: '거래처 READ/EDIT, 매출 READ' },
            { key: 2, user: '김특판 (특판 담당)', profile: '5.영업사원', sets: 'PromotionEdit', effect: '거래처 READ/EDIT + 행사 EDIT 추가' },
            { key: 3, user: '박관리 (운영)', profile: '시스템 관리자', sets: '-', effect: '전체 entity CRUD + MODIFY_ALL_DATA' },
          ]}
          columns={[
            { title: '사용자', dataIndex: 'user', key: 'user' },
            { title: 'Profile (1개)', dataIndex: 'profile', key: 'profile', render: (v: string) => <Tag color="blue">{v}</Tag> },
            { title: 'PermissionSet (N개)', dataIndex: 'sets', key: 'sets', render: (v: string) => v === '-' ? <Text type="secondary">-</Text> : <Tag color="orange">{v}</Tag> },
            { title: '실효 권한', dataIndex: 'effect', key: 'effect' },
          ]}
        />
      </Card>

      <Card id="bits" title="2. 시스템 비트 5종" style={{ marginBottom: 16 }}>
        <Paragraph>
          Profile 과 PermissionSet 모두 동일한 <Text strong>5개 시스템 비트</Text> 를 가집니다. 이 비트는 entity 단위가 아니라
          <Text strong> 시스템 전체 권한</Text> 입니다.
        </Paragraph>
        <Table
          size="small"
          pagination={false}
          rowKey="bit"
          dataSource={systemBits}
          columns={[
            { title: '비트', dataIndex: 'bit', key: 'bit', render: (v: string) => <Tag color="red">{v}</Tag>, width: 180 },
            { title: '의미', dataIndex: 'meaning', key: 'meaning' },
            { title: '대표 예시', dataIndex: 'example', key: 'example' },
          ]}
        />
        <Alert
          type="warning"
          showIcon
          icon={<ExclamationCircleOutlined />}
          message="MODIFY_ALL_DATA 는 가장 강력한 권한"
          description="MODIFY_ALL_DATA 를 가진 사용자는 Owner / Sharing Rule / Object Permission 을 모두 우회합니다. 시스템 관리자 + 운영팀에만 부여하세요. 본인이 본인의 MODIFY_ALL_DATA 부여를 회수하는 것은 시스템이 차단합니다 (자기 자신 잠금 방지)."
          style={{ marginTop: 12 }}
        />
      </Card>

      <Card id="matrix" title="3. Entity × CRUD 매트릭스" style={{ marginBottom: 16 }}>
        <Paragraph>
          시스템 비트와 별도로, <Text strong>각 entity (테이블)</Text> 마다 CRUD 4종을 개별 부여합니다.
        </Paragraph>
        <Table
          size="small"
          pagination={false}
          rowKey="op"
          dataSource={crudOps}
          columns={[
            { title: 'Operation', dataIndex: 'op', key: 'op', render: (v: string) => <Tag color="cyan">{v}</Tag>, width: 100 },
            { title: '의미', dataIndex: 'desc', key: 'desc', width: 120 },
            { title: '예시 (거래처 entity)', dataIndex: 'example', key: 'example' },
          ]}
        />
        <Divider />
        <Title level={5}>실효 권한 계산 공식</Title>
        <Paragraph>
          <Text code>실효(entity, op) = Profile.bit(MODIFY_ALL_DATA 등 우회) OR Profile.entityCRUD OR ∪ PermissionSets.entityCRUD</Text>
        </Paragraph>
        <Alert
          type="info"
          showIcon
          message="MODIFY_ALL_DATA 우회 규칙"
          description="MODIFY_ALL_DATA 를 가진 Profile/PermissionSet 은 모든 entity 의 모든 CRUD 를 자동 통과합니다. 따라서 시스템 관리자 Profile 의 entity CRUD 컬럼은 비어있어도 정상 동작합니다."
          style={{ marginTop: 12 }}
        />
      </Card>

      <Card id="inventory-profile" title="4. Profile 인벤토리 (오뚜기 운영 실측 — IsCustom=TRUE 18개)" style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          showIcon
          message="본 표는 Salesforce 레거시 운영 환경의 실제 Profile 목록입니다"
          description="신규 사용자에게 어떤 Profile 을 줄지 결정할 때 본 표에서 직무에 맞는 baseline 을 1개만 선택하세요. 모든 직급은 숫자 prefix (1~12) + 직무명 형태로 SF 운영에 들어있습니다."
          style={{ marginBottom: 16 }}
        />
        <Table
          size="small"
          pagination={false}
          rowKey={(r: LegacyProfileRow) => `${r.num}-${r.name}`}
          dataSource={legacyProfiles}
          columns={[
            { title: '#', dataIndex: 'num', key: 'num', width: 60, render: (v: string) => <Tag color="blue">{v}</Tag> },
            { title: 'Profile 명', dataIndex: 'name', key: 'name', width: 220, render: (v: string) => <Text strong>{v}</Text> },
            { title: '역할', dataIndex: 'role', key: 'role', width: 240 },
            { title: '데이터 범위 / 특징', dataIndex: 'scope', key: 'scope' },
          ]}
        />
        <Alert
          type="warning"
          showIcon
          message="Profile 변형 케이스 주의"
          description={
            <>
              <Text strong>5.영업사원</Text> / <Text strong>5.영업사원(로그인ip대역설정)</Text> 처럼 같은 직급이지만 보안 정책이 다른 변형이 존재합니다.
              <br />
              <Text strong>6.조장</Text> / <Text strong>6-1.조장</Text> / <Text strong>6.조장_test</Text> 도 유사 변형이며 운영에 들어있는 그대로 유지되어 있습니다.
              <br />
              <Text strong>7.영업사원+조장</Text> 은 Profile 이 1인 1개 원칙이라 5+6 결합이 필요한 사용자를 위한 별도 Profile 로 정의되었습니다.
            </>
          }
          style={{ marginTop: 12 }}
        />
      </Card>

      <Card id="inventory-ps" title="5. PermissionSet 인벤토리 (오뚜기 운영 실측 — IsCustom=TRUE 49개 중 핵심 18개)" style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          showIcon
          message="가산 권한은 직무 특화 PermissionSet 으로 부여합니다"
          description="대부분의 PS 는 'XXX_View_All' 패턴으로 '담당자 범위(owner) 를 넘어서 전체를 보고싶다' 는 요구에 대응합니다. EDIT/DELETE 권한 PS 는 신중히 부여하세요."
          style={{ marginBottom: 16 }}
        />
        <Table
          size="small"
          pagination={false}
          rowKey="ps"
          dataSource={legacyPS}
          columns={[
            { title: 'API 명', dataIndex: 'ps', key: 'ps', width: 280, render: (v: string) => <Tag color="orange">{v}</Tag> },
            { title: '라벨 (한글)', dataIndex: 'label', key: 'label', width: 220, render: (v: string) => <Text strong>{v}</Text> },
            { title: '용도', dataIndex: 'purpose', key: 'purpose' },
            { title: '대표 부여 대상', dataIndex: 'typicalUser', key: 'typicalUser', width: 220 },
          ]}
        />
        <Paragraph style={{ marginTop: 12 }} type="secondary">
          * SF 표준 패키지 (force__ namespace) 인 <Text code>PermissionSetGroup</Text> 12개 (Sales, Field Service 등) 는 본 프로젝트 운영 권한 모델 외부 이므로 본 표에서 제외했습니다.
        </Paragraph>
      </Card>

      <Card id="combinations" title="6. Profile × PermissionSet 조합 케이스 (실제 사용 패턴 10종)" style={{ marginBottom: 16 }}>
        <Alert
          type="info"
          showIcon
          message="실제 발생하는 모든 조합 패턴을 망라"
          description="(1) Profile 단독 충족 / (2) Profile + 1개 가산 / (3) Profile + 다중 가산 / (4) 한시적 가산 / (5) 가산 금지 정책 — 5가지 유형을 모두 포함합니다."
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
              title: 'Profile (1개)',
              dataIndex: 'profile',
              key: 'profile',
              width: 150,
              render: (v: string) => <Tag color="blue">{v}</Tag>,
            },
            {
              title: 'PermissionSet 가산',
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

      <Card id="workflow" title="7. 실무 워크플로우" style={{ marginBottom: 16 }}>
        <Title level={5}>4-1. 신규 사용자에게 권한 부여</Title>
        <Steps
          direction="vertical"
          size="small"
          current={-1}
          items={[
            { title: '사용자 관리 → 신규 등록', description: '사번 / 이름 / 이메일 입력' },
            { title: 'Profile 선택', description: '직무에 맞는 baseline Profile 1개 (예: "5.영업사원")' },
            { title: '필요 시 PermissionSet 가산', description: '사원 현황 → 해당 사원 → "SF 권한" → "PermissionSet 부여 추가"' },
            { title: '사용자 재로그인', description: '권한 변경은 다음 로그인 시점부터 적용됩니다' },
          ]}
        />

        <Divider />

        <Title level={5}>4-2. 권한 회수</Title>
        <Steps
          direction="vertical"
          size="small"
          current={-1}
          items={[
            { title: '사원 현황 → 해당 사원 진입', description: '"SF 권한" 섹션 펼침' },
            { title: '회수할 PermissionSet 우측 "회수" 버튼', description: '본인의 MODIFY_ALL_DATA 회수는 시스템이 차단' },
            { title: '확인 모달에서 사유 입력 후 회수', description: '회수 이력은 audit 로그에 자동 기록' },
          ]}
        />

        <Divider />

        <Title level={5}>4-3. Profile 자체 변경</Title>
        <Alert
          type="warning"
          showIcon
          message="Profile 변경은 권한 영향 범위가 큽니다"
          description="Profile 은 baseline 이므로 변경 시 해당 사용자의 모든 entity 권한이 한 번에 바뀝니다. 가능하면 PermissionSet 가산/회수로 해결하고, Profile 변경은 직무 자체가 바뀐 경우에만 사용하세요."
          style={{ marginTop: 12 }}
        />
      </Card>

      <Card id="scenarios" title="8. 시나리오별 빠른 참조" style={{ marginBottom: 16 }}>
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

      <Card id="faq" title="9. FAQ" style={{ marginBottom: 16 }}>
        <Collapse
          defaultActiveKey={['q1']}
          items={[
            {
              key: 'q1',
              label: <span><QuestionCircleOutlined /> Profile 과 PermissionSet 권한이 충돌하면?</span>,
              children: (
                <Paragraph>
                  충돌이라는 개념이 없습니다. 둘 다 <Text strong>가산 (OR)</Text> 이라 어느 한쪽이라도 권한을 주면 통과합니다.
                  권한을 빼고 싶으면 부여하지 않으면 됩니다 — "비활성" 같은 negative 비트는 없습니다.
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
                  <br />또한 권한 매트릭스 화면은 운영 진단용 <Text strong>5분 캐시</Text> 입니다 — 부여/회수 직후엔 매트릭스가 잠시 stale 할 수 있습니다.
                </Paragraph>
              ),
            },
            {
              key: 'q3',
              label: <span><QuestionCircleOutlined /> 시스템 관리자 Profile 은 왜 entity CRUD 가 비어있나요?</span>,
              children: (
                <Paragraph>
                  시스템 관리자 Profile 은 <Tag color="red">MODIFY_ALL_DATA</Tag> 비트를 가지고 있어
                  모든 entity 의 모든 CRUD 를 자동 통과합니다. 따라서 entity CRUD 매트릭스를 별도로 채우지 않아도 동작합니다.
                </Paragraph>
              ),
            },
            {
              key: 'q4',
              label: <span><QuestionCircleOutlined /> 영업사원이 본인 거래처가 아닌 다른 거래처를 못 봅니다</span>,
              children: (
                <Paragraph>
                  정상 동작입니다. 영업사원 Profile 은 <Tag color="red">VIEW_ALL_DATA</Tag> 가 없어 본인이 <Text strong>Owner</Text> 인
                  레코드만 볼 수 있습니다. 모든 거래처 조회가 필요하면 <Tag color="orange">AccountViewAll</Tag> PermissionSet 을
                  가산하거나, 직무가 분석 담당이라면 Profile 변경을 검토하세요.
                </Paragraph>
              ),
            },
            {
              key: 'q5',
              label: <span><QuestionCircleOutlined /> 권한 매트릭스에 PermissionSet 은 왜 안 보이나요?</span>,
              children: (
                <Paragraph>
                  본 매트릭스는 Profile 단위 <Text strong>합집합</Text> 표시 (Profile + 해당 Profile 표본 user 의 PermissionSet 영향 포함) 입니다.
                  PermissionSet 별 분해가 필요하면 <Text code>PermissionSet 관리 → 해당 PS → 상세</Text> 화면의 entity 매트릭스를 사용하세요.
                </Paragraph>
              ),
            },
            {
              key: 'q6',
              label: <span><QuestionCircleOutlined /> 누가 무슨 PermissionSet 을 받았는지 한눈에 보고 싶습니다</span>,
              children: (
                <Paragraph>
                  <Text code>PermissionSet 관리 → 해당 PS 행 클릭</Text> 하면 상세 페이지 하단에 <Text strong>부여된 사용자 일람</Text> 이 표시됩니다.
                  반대로 <Text code>사원 현황 → 해당 사원 → SF 권한 섹션</Text> 에서 한 사용자의 부여 PS 일람을 볼 수도 있습니다.
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
