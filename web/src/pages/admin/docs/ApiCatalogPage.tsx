import { Alert, Button, Card, Col, Divider, Row, Space, Statistic, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ApiOutlined, ExportOutlined } from '@ant-design/icons';

const { Title, Paragraph, Text } = Typography;

/**
 * API 카탈로그 — REST 엔드포인트를 탐색하는 진입점.
 * 시스템 관리자(profile)만 접근 (라우터 가드).
 *
 * 엔드포인트 스펙의 단일 원천은 backend springdoc(OpenAPI) 이다. 본 페이지는 그 Swagger UI 로
 * 가는 안내 + 분포 요약만 제공한다 (스펙 자체를 복제하지 않아 항상 최신과 일치).
 *
 * 주의: Swagger UI / api-docs 는 dev·local 프로파일에서만 활성이고 prod 에서는 비활성이다
 * (application.yml 의 prod 프로파일: springdoc.api-docs.enabled=false, swagger-ui.enabled=false).
 *
 * 분포 수치는 작성 시점 backend/openapi.json(245 endpoints) 실측. 코드 변경 시 갱신 필요.
 */

interface GroupRow {
  key: string;
  group: string;
  displayName: string;
  prefix: string;
  count: number;
  docUrl: string;
  desc: string;
}

// backend OpenApiConfig.kt 의 GroupedOpenApi 정의 + openapi.json path 분포 실측
const GROUPS: GroupRow[] = [
  {
    key: 'admin',
    group: 'admin',
    displayName: 'Admin API',
    prefix: '/api/*/admin/**',
    count: 182,
    docUrl: '/swagger-ui.html?urls.primaryName=admin',
    desc: '웹 관리자 대시보드가 호출하는 백오피스 API. 가장 큰 그룹.',
  },
  {
    key: 'mobile',
    group: 'mobile',
    displayName: 'Mobile API',
    prefix: '/api/*/mobile/**',
    count: 48,
    docUrl: '/swagger-ui.html?urls.primaryName=mobile',
    desc: '영업 여사원 Flutter 앱이 호출하는 API.',
  },
  {
    key: 'sap',
    group: 'sap',
    displayName: 'SAP Inbound API',
    prefix: '/api/*/sap/**',
    count: 12,
    docUrl: '/swagger-ui.html?urls.primaryName=sap',
    desc: 'SAP(ERP) 이 마스터 데이터를 보내는 인바운드 수신 API.',
  },
];

const TOTAL = 245;
const SF_COUNT = 2;
const HEALTH_COUNT = 1;

const COLUMNS: ColumnsType<GroupRow> = [
  {
    title: '그룹',
    dataIndex: 'displayName',
    key: 'displayName',
    width: 170,
    render: (name: string, row) => (
      <Space direction="vertical" size={0}>
        <Text strong>{name}</Text>
        <Text code style={{ fontSize: 11 }}>
          {row.prefix}
        </Text>
      </Space>
    ),
  },
  {
    title: '엔드포인트',
    dataIndex: 'count',
    key: 'count',
    width: 100,
    render: (c: number) => <Tag color="blue">{c}개</Tag>,
  },
  { title: '설명', dataIndex: 'desc', key: 'desc' },
  {
    title: 'Swagger UI',
    key: 'action',
    width: 130,
    render: (_, row) => (
      <Button
        size="small"
        icon={<ExportOutlined />}
        href={row.docUrl}
        target="_blank"
        rel="noopener noreferrer"
      >
        열기
      </Button>
    ),
  },
];

export default function ApiCatalogPage() {
  return (
    <Typography>
      <Title level={2}>API 카탈로그</Title>
      <Paragraph type="secondary">
        REST 엔드포인트의 단일 원천은 백엔드의 <Text code>springdoc</Text>(OpenAPI) 문서입니다. 이 페이지는
        그 <Text strong>Swagger UI</Text> 로 가는 진입점과 분포 요약을 제공합니다. 스펙 자체를 복제하지
        않으므로 항상 코드의 최신 상태와 일치합니다.
      </Paragraph>

      <Alert
        type="warning"
        showIcon
        message="운영(prod) 환경에서는 Swagger UI 가 비활성입니다"
        description={
          <span>
            보안을 위해 prod 프로파일에서는 <Text code>springdoc.api-docs.enabled=false</Text> /{' '}
            <Text code>swagger-ui.enabled=false</Text> 로 설정되어 있습니다. 아래 링크는{' '}
            <Text strong>dev · local 환경</Text>에서만 동작합니다. 운영 API 계약 확인이 필요하면 저장소의{' '}
            <Text code>backend/openapi.json</Text>(빌드 산출물)을 참고하세요.
          </span>
        }
        style={{ margin: '12px 0 20px' }}
      />

      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic title="전체 엔드포인트" value={TOTAL} prefix={<ApiOutlined />} />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic title="Admin" value={182} />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic title="Mobile" value={48} />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small">
            <Statistic title="SAP" value={12} />
          </Card>
        </Col>
      </Row>

      <Title level={3}>그룹별 문서</Title>
      <Paragraph type="secondary">
        Swagger UI 우상단의 그룹 선택 박스로도 전환할 수 있습니다. 아래 "열기" 는 해당 그룹을 바로 엽니다.
      </Paragraph>
      <Table rowKey="key" columns={COLUMNS} dataSource={GROUPS} pagination={false} size="small" />

      <Paragraph type="secondary" style={{ marginTop: 12 }}>
        그룹에 포함되지 않는 엔드포인트로 Salesforce 연동 {SF_COUNT}개(<Text code>/api/*/sf/**</Text>),
        헬스체크 {HEALTH_COUNT}개(<Text code>/api/health</Text>) 가 있으며 전체 문서(<Text code>/swagger-ui.html</Text>)
        에서 확인할 수 있습니다.
      </Paragraph>

      <Divider />

      <Title level={3}>전체 Swagger UI</Title>
      <Space>
        <Button type="primary" icon={<ExportOutlined />} href="/swagger-ui.html" target="_blank" rel="noopener noreferrer">
          Swagger UI 전체 열기
        </Button>
        <Button icon={<ExportOutlined />} href="/v3/api-docs" target="_blank" rel="noopener noreferrer">
          OpenAPI JSON (/v3/api-docs)
        </Button>
      </Space>

      <Divider />

      <Title level={3}>OpenAPI 스펙 재생성</Title>
      <Paragraph>
        저장소에 커밋된 스펙 파일(<Text code>backend/openapi*.json</Text>)을 최신 코드로 다시 만들려면:
      </Paragraph>
      <Card size="small" style={{ background: '#fafafa' }}>
        <pre style={{ margin: 0, fontFamily: 'monospace', fontSize: 12 }}>
{`cd backend && ./gradlew generateOpenApiDocs`}
        </pre>
      </Card>

      <Divider />
      <Paragraph type="secondary" style={{ fontSize: 12 }}>
        엔드포인트 분포 수치는 작성 시점 실측이며 코드 변경에 따라 자동 갱신되지 않습니다. Swagger UI 링크와
        재생성 명령은 코드 구조가 바뀌지 않는 한 유효합니다. 데이터 흐름 관점의 안내는 데이터 흐름(DFD)
        페이지에서 다룹니다.
      </Paragraph>
    </Typography>
  );
}
