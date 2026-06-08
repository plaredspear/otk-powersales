import { useMemo, useState } from 'react';
import { Alert, Card, Col, Divider, Input, Row, Segmented, Space, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import ResizableTable from '@/components/common/ResizableTable';

const { Title, Paragraph, Text } = Typography;

/**
 * 도메인 / 모듈 맵 — 어떤 기능이 어느 backend 도메인 패키지에 있고, 어느 화면과 연결되는지 매핑.
 * 시스템 관리자(profile)만 접근 (라우터 가드).
 *
 * 본 페이지의 서술은 정적(하드코딩) 콘텐츠다. 도메인 패키지가 추가/삭제되면 수동 갱신이 필요하다.
 * 작성 시점 기준: backend(com/otoki/powersales/*, com/otoki/orora) 코드 실측 + web menuConfig 교차.
 */

type DomainKind = 'shared' | 'admin' | 'infra';

interface DomainRow {
  key: string;
  pkg: string;
  name: string;
  responsibility: string;
  entities: string;
  webMenu: string;
  mobileArea: string;
  kind: DomainKind;
}

const KIND_LABEL: Record<DomainKind, { text: string; color: string }> = {
  shared: { text: '공용', color: 'blue' },
  admin: { text: 'Web 전용', color: 'green' },
  infra: { text: '인프라/연동', color: 'default' },
};

// backend 코드 실측 기준 도메인 인벤토리 (com/otoki/powersales/* + com/otoki/orora)
const DOMAINS: DomainRow[] = [
  // ── Mobile / Web 공용 도메인 ──
  { key: 'sales', pkg: 'sales', name: '매출/실적', responsibility: '일일 매출 기록, 월간 매출 집계 조회', entities: 'DailySalesHistory', webMenu: '매출/실적 > 월 매출', mobileArea: '일일매출 등록, 월/행사 매출 탭', kind: 'shared' },
  { key: 'schedule', pkg: 'schedule', name: '여사원 일정/근무', responsibility: '캘린더, 월간 통합 일정, 근무 정보(출근) 등록', entities: 'Appointment · AttendanceLog · AttendInfo', webMenu: '여사원 일정 > 일정관리', mobileArea: '일정 캘린더, 출근 등록', kind: 'shared' },
  { key: 'promotion', pkg: 'promotion', name: '행사 관리', responsibility: '판촉 행사 마스터, 전문행사조, 진열 스케줄', entities: 'Promotion · ProfessionalPromotionTeamMaster', webMenu: '행사/배치 > 행사마스터', mobileArea: '행사 목록/상세, 행사 매출', kind: 'shared' },
  { key: 'order', pkg: 'order', name: '주문 관리', responsibility: '발주 요청, 거래처 주문 조회, 여신(선금) 조회', entities: 'ErpOrder · ErpOrderProduct · OrderRequest', webMenu: '—', mobileArea: '주문 목록/등록, 거래처 주문 상세', kind: 'shared' },
  { key: 'inspection', pkg: 'inspection', name: '현장 점검', responsibility: '현장점검 테마 정의 및 점검 기록 관리', entities: 'InspectionTheme', webMenu: '현장점검/이슈 > 현장점검', mobileArea: '점검 목록/상세', kind: 'shared' },
  { key: 'safetycheck', pkg: 'safetycheck', name: '안전 점검', responsibility: '현장 안전 점검 항목 정의 및 제출 기록', entities: 'SafetyCheckItem · SafetyCheckSubmission', webMenu: '현장점검/이슈 > 안전점검', mobileArea: '안전점검, 점검 현황', kind: 'shared' },
  { key: 'productexpiration', pkg: 'productexpiration', name: '유통기한 관리', responsibility: '현장 제품 유통기한 등록/조회/삭제', entities: 'ProductExpiration', webMenu: '현장점검/이슈 > 유통기한 관리', mobileArea: '유통기한 목록/등록', kind: 'shared' },
  { key: 'claim', pkg: 'claim', name: '제품 클레임', responsibility: '제품 불량 클레임 등록/조회/수정', entities: 'Claim', webMenu: '현장점검/이슈 > 제품 클레임', mobileArea: '클레임 목록/등록', kind: 'shared' },
  { key: 'suggestion', pkg: 'suggestion', name: '물류 클레임', responsibility: '물류 문제 및 개선 제안 등록/상태 관리', entities: 'Suggestion · SuggestionActionStatus', webMenu: '현장점검/이슈 > 물류 클레임', mobileArea: '물류 클레임 등록', kind: 'shared' },
  { key: 'product', pkg: 'product', name: '제품 마스터', responsibility: '제품 기본정보, 바코드, 즐겨찾기', entities: 'Product · FavoriteProduct · NewProduct', webMenu: '기준정보 > 제품', mobileArea: '제품 검색/결과', kind: 'shared' },
  { key: 'notice', pkg: 'notice', name: '공지사항', responsibility: '공지사항 작성/배포 (카테고리/대상 분류)', entities: 'Notice', webMenu: '알림/교육 > 공지사항', mobileArea: '공지 목록/상세', kind: 'shared' },
  { key: 'education', pkg: 'education', name: '교육', responsibility: '여사원 대상 온라인 교육 콘텐츠 관리', entities: 'EducationPost · EducationCode', webMenu: '알림/교육 > 교육', mobileArea: '교육 목록/메인', kind: 'shared' },
  { key: 'leave', pkg: 'leave', name: '휴무 관리', responsibility: '공휴일, 대체휴무, 연차 관리', entities: 'HolidayMaster · AlternativeHoliday', webMenu: '인사/근무 > 휴무관리', mobileArea: '대체휴무 신청/이력', kind: 'shared' },

  // ── Web 관리자 전용 ──
  { key: 'employee', pkg: 'employee', name: '여사원 정보', responsibility: '여사원 프로필·인사 정보 조회/관리', entities: 'Employee · EmployeeInfo · Group', webMenu: '인사/근무 > 여사원 현황 / 기준정보 > 사원', mobileArea: '—', kind: 'admin' },
  { key: 'account', pkg: 'account', name: '거래처 관리', responsibility: '거래처(고객사) 마스터 CRUD 및 분류, Naver 지오코딩', entities: 'Account · AccountCategoryMaster', webMenu: '기준정보 > 거래처', mobileArea: '(거래처 조회는 일부 영업 화면에서 사용)', kind: 'admin' },
  { key: 'organization', pkg: 'organization', name: '조직 마스터', responsibility: '조직 구조, 비용 센터 매핑 (Redis 24h 캐시)', entities: 'Organization', webMenu: '기준정보 > 조직마스터', mobileArea: '—', kind: 'admin' },
  { key: 'admin', pkg: 'admin', name: '관리자 기능 모음', responsibility: '권한·사용자·역할·운영도구 등 백오피스 전반', entities: '(하위 도메인 연계)', webMenu: '시스템 / 개발자 도구 메뉴 전체', mobileArea: '—', kind: 'admin' },

  // ── 인증 / 인프라 / 외부 연동 ──
  { key: 'auth', pkg: 'auth', name: '인증/권한', responsibility: '로그인, JWT 발급, SF Profile/PermissionSet 권한 산출', entities: 'Profile · AppAuthority', webMenu: '시스템 > 사용자/프로파일 관리', mobileArea: '(로그인)', kind: 'infra' },
  { key: 'user', pkg: 'user', name: '사용자', responsibility: 'User 엔티티 관리, 프로비저닝 이벤트 발행', entities: 'User', webMenu: '시스템 > 사용자 관리', mobileArea: '—', kind: 'infra' },
  { key: 'agreement', pkg: 'agreement', name: '약관/동의', responsibility: '모바일 앱 약관 순환(cycle)·버전 관리', entities: 'AgreementWord · AgreementHistory', webMenu: '모바일앱 > 동의 약관 등록', mobileArea: '(약관 동의)', kind: 'infra' },
  { key: 'sap', pkg: 'sap', name: 'SAP 연동', responsibility: 'SAP OAuth + Inbound(마스터 수신) / Outbound(Outbox 전송)', entities: 'SapOutbox', webMenu: '개발자 도구 > SAP 연동', mobileArea: '—', kind: 'infra' },
  { key: 'orora', pkg: 'orora', name: 'Orora 연동', responsibility: '영업시스템(Orora) 매출 데이터 조회·적재', entities: 'OroraDailySalesHistory · OroraMonthlySalesHistory', webMenu: '—', mobileArea: '—', kind: 'infra' },
  { key: 'sf', pkg: 'sf', name: 'Salesforce 연동', responsibility: 'SF OAuth, 권한/데이터 인바운드 동기, 헬스체크', entities: '(연동 전용)', webMenu: '개발자 도구 > SF Migration', mobileArea: '—', kind: 'infra' },
  { key: 'sfmigration', pkg: 'sfmigration', name: 'SF 마이그레이션', responsibility: 'SF 데이터 복제(Stage 1) → FK 정합(Stage 2)', entities: '(임시 단계 도구)', webMenu: '개발자 도구 > SF Migration', mobileArea: '—', kind: 'infra' },
  { key: 'draft', pkg: 'draft', name: '임시 데이터', responsibility: '레거시 마이그레이션용 임시 적재(미지급 claim/onsite)', entities: 'TmpClaim · TmpOnsite', webMenu: '—', mobileArea: '—', kind: 'infra' },
  { key: 'batch', pkg: 'batch', name: '배치', responsibility: 'SAP 전송·매출 집계·약관 순환·지오코딩 등 정기 작업 진입점 (ShedLock)', entities: '(서비스 위임)', webMenu: '개발자 도구 > 스케줄 잡 실행 이력', mobileArea: '—', kind: 'infra' },
  { key: 'common', pkg: 'common', name: '공통 인프라', responsibility: '스토리지(S3/로컬), 보안, Naver, 공통 엔티티 기저', entities: 'AuditedEntity 등', webMenu: '(전반에 사용)', mobileArea: '(전반에 사용)', kind: 'infra' },
];

const COLUMNS: ColumnsType<DomainRow> = [
  {
    title: '도메인',
    dataIndex: 'name',
    key: 'name',
    width: 150,
    render: (name: string, row) => (
      <Space direction="vertical" size={0}>
        <Text strong>{name}</Text>
        <Text code style={{ fontSize: 11 }}>
          {row.pkg}
        </Text>
      </Space>
    ),
  },
  {
    title: '분류',
    dataIndex: 'kind',
    key: 'kind',
    width: 90,
    render: (kind: DomainKind) => <Tag color={KIND_LABEL[kind].color}>{KIND_LABEL[kind].text}</Tag>,
  },
  { title: '책임', dataIndex: 'responsibility', key: 'responsibility' },
  {
    title: '주요 엔티티',
    dataIndex: 'entities',
    key: 'entities',
    width: 180,
    render: (v: string) => <Text type="secondary" style={{ fontSize: 12 }}>{v}</Text>,
  },
  {
    title: '연결 Web 화면',
    dataIndex: 'webMenu',
    key: 'webMenu',
    width: 180,
    render: (v: string) => (v === '—' ? <Text type="secondary">—</Text> : v),
  },
  {
    title: 'Mobile 영역',
    dataIndex: 'mobileArea',
    key: 'mobileArea',
    width: 180,
    render: (v: string) => (v === '—' ? <Text type="secondary">—</Text> : <Text type="secondary" style={{ fontSize: 12 }}>{v}</Text>),
  },
];

const FILTER_OPTIONS = [
  { label: '전체', value: 'all' },
  { label: `공용 (${DOMAINS.filter((d) => d.kind === 'shared').length})`, value: 'shared' },
  { label: `Web 전용 (${DOMAINS.filter((d) => d.kind === 'admin').length})`, value: 'admin' },
  { label: `인프라/연동 (${DOMAINS.filter((d) => d.kind === 'infra').length})`, value: 'infra' },
];

export default function DomainMapPage() {
  const [filter, setFilter] = useState<string>('all');
  const [keyword, setKeyword] = useState('');

  const rows = useMemo(() => {
    const kw = keyword.trim().toLowerCase();
    return DOMAINS.filter((d) => {
      if (filter !== 'all' && d.kind !== filter) return false;
      if (!kw) return true;
      return (
        d.name.toLowerCase().includes(kw) ||
        d.pkg.toLowerCase().includes(kw) ||
        d.responsibility.toLowerCase().includes(kw) ||
        d.entities.toLowerCase().includes(kw) ||
        d.webMenu.toLowerCase().includes(kw)
      );
    });
  }, [filter, keyword]);

  return (
    <Typography>
      <Title level={2}>도메인 / 모듈 맵</Title>
      <Paragraph type="secondary">
        backend 의 도메인 패키지(<Text code>com.otoki.powersales.*</Text>) 가 각각 어떤 책임을 지고, 어느
        화면과 연결되는지 정리한 지도입니다. "이 기능은 어느 코드에 있지?" 를 빠르게 찾는 용도입니다.
      </Paragraph>

      <Alert
        type="info"
        showIcon
        message="API 경로 규칙"
        description={
          <span>
            엔드포인트는 호출 주체별로 prefix 가 나뉩니다 — Mobile <Text code>/api/v1/mobile/…</Text> ·
            Web 관리자 <Text code>/api/v1/admin/…</Text> · SAP 연동 <Text code>/api/v1/sap/…</Text> ·
            Salesforce <Text code>/api/v1/sf/…</Text>. 같은 도메인이라도 모바일용·관리자용 서비스가 별도로
            존재합니다 (예: <Text code>MobilePromotionService</Text> vs <Text code>AdminPromotionService</Text>).
          </span>
        }
        style={{ margin: '12px 0 20px' }}
      />

      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} md={8}>
          <Card size="small">
            <Space direction="vertical" size={2}>
              <Text strong>13 공용 도메인</Text>
              <Text type="secondary" style={{ fontSize: 12 }}>모바일 앱 + 웹 관리자가 함께 쓰는 핵심 업무</Text>
            </Space>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card size="small">
            <Space direction="vertical" size={2}>
              <Text strong>4 Web 전용 도메인</Text>
              <Text type="secondary" style={{ fontSize: 12 }}>관리자만 다루는 마스터/백오피스</Text>
            </Space>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card size="small">
            <Space direction="vertical" size={2}>
              <Text strong>9 인프라/연동 도메인</Text>
              <Text type="secondary" style={{ fontSize: 12 }}>인증·외부 연동·배치 등 화면 뒤의 기반</Text>
            </Space>
          </Card>
        </Col>
      </Row>

      <Space wrap style={{ marginBottom: 16 }}>
        <Segmented options={FILTER_OPTIONS} value={filter} onChange={(v) => setFilter(v as string)} />
        <Input.Search
          placeholder="도메인·기능·엔티티 검색"
          allowClear
          style={{ width: 260 }}
          onChange={(e) => setKeyword(e.target.value)}
        />
      </Space>

      <ResizableTable
        rowKey="key"
        columns={COLUMNS}
        dataSource={rows}
        pagination={false}
        size="small"
        scroll={{ x: 1100 }}
      />

      <Divider />
      <Paragraph type="secondary" style={{ fontSize: 12 }}>
        이 표는 코드 변경에 따라 자동 갱신되지 않습니다. 도메인 패키지를 추가/삭제하면 본
        페이지(DomainMapPage.tsx)도 함께 갱신해 주세요. 엔드포인트 단위의 상세는 API 카탈로그(준비 중)에서
        다룰 예정입니다.
      </Paragraph>
    </Typography>
  );
}
