import { Alert, Descriptions, Skeleton, Tag, Typography } from 'antd';
import { useExternalApiIntegrationInfo } from '@/api/admin/externalApiIntegrationInfo';

const { Text } = Typography;

const METHOD_COLOR: Record<string, string> = {
  GET: 'green',
  POST: 'blue',
  PUT: 'orange',
  DELETE: 'red',
};

/**
 * 외부 API 연동 정보 표 (외부 API 테스트 통합 페이지).
 *
 * backend `GET /api/v1/admin/external-api/integration-info` 를 조회해 현재 환경에 주입된 실제
 * 외부 시스템 endpoint / HTTP method / 인증 방식을 노출한다. `apiKey` 로 해당 탭의 항목을 찾아
 * 표시하며, 전체 목록은 한 번만 조회하고 캐시한다(staleTime 무한).
 */
export default function IntegrationInfoDescriptions({ apiKey }: { apiKey: string }) {
  const { info, isLoading, isError } = useExternalApiIntegrationInfo(apiKey);

  if (isLoading) {
    return <Skeleton active paragraph={{ rows: 2 }} />;
  }
  if (isError) {
    return (
      <Alert
        type="error"
        showIcon
        message="연동 정보를 불러오지 못했습니다."
        description="권한(SYSTEM_ADMIN) 또는 네트워크를 확인하세요."
      />
    );
  }

  if (!info) {
    return (
      <Alert
        type="warning"
        showIcon
        message="등록된 연동 정보가 없습니다."
      />
    );
  }

  return (
    <Descriptions
      bordered
      size="small"
      column={1}
      styles={{ label: { width: 160, whiteSpace: 'nowrap' } }}
    >
      <Descriptions.Item label="외부 시스템">{info.externalSystem}</Descriptions.Item>
      <Descriptions.Item label="HTTP Method">
        <Tag color={METHOD_COLOR[info.httpMethod] ?? 'default'}>{info.httpMethod}</Tag>
      </Descriptions.Item>
      <Descriptions.Item label="Endpoint">
        <Text copyable code style={{ fontSize: 12 }}>
          {info.endpoint}
        </Text>
      </Descriptions.Item>
      <Descriptions.Item label="인증">{info.authType}</Descriptions.Item>
      <Descriptions.Item label="비고">{info.note}</Descriptions.Item>
    </Descriptions>
  );
}
