import { Alert, Button, Card, Descriptions, Space, Statistic, Typography } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';
import {
  fetchUninstalledFemaleStaffSummary,
  UNINSTALLED_FEMALE_STAFF_EXPORT_PATH,
  type AppUninstalledFemaleStaffSummary,
} from '@/api/admin/appInstallStatus';

const { Paragraph, Text } = Typography;

const QUERY_KEY = ['admin', 'tools', 'app-install', 'uninstalled-female-staff'] as const;

/**
 * 개발자 도구 > 대시보드 > 기능 활성화 > 앱 미설치 여사원 (설치 안내용 한시 기능).
 *
 * 앱을 한 번도 사용한 흔적이 없는 여사원 인원을 보여주고, 설치 안내에 쓸 명단(사번/이름/지점명)을
 * 엑셀로 내려받는다. 조회 기준(판정식 / 모수 / 해석 주의) 을 화면에 명시해 운영자가 수치를 그대로
 * 신뢰하지 않도록 한다 — 서버에는 설치 여부가 아니라 앱이 통신한 흔적만 남기 때문이다.
 * 시스템 관리자 전용 (백엔드 가드).
 */
export default function AppInstallStatusCard() {
  const { data, isLoading, isError } = useQuery<AppUninstalledFemaleStaffSummary>({
    queryKey: QUERY_KEY,
    queryFn: fetchUninstalledFemaleStaffSummary,
  });

  const { run, downloading } = useExcelDownload();

  const uninstalledCount = data?.uninstalledCount ?? 0;
  const targetCount = data?.targetCount ?? 0;

  return (
    <Card size="small" title="앱 미설치 여사원" style={{ marginTop: 24 }}>
      <Paragraph type="secondary">
        설치 안내 대상을 뽑기 위한 <Text strong>한시 기능</Text>입니다. 서버에는 설치 여부가 아니라{' '}
        <Text strong>앱이 서버와 통신한 흔적</Text>만 남으므로, 아래 기준으로 미설치를 추정합니다.
      </Paragraph>

      {isError && (
        <Alert
          type="error"
          style={{ marginBottom: 16 }}
          message="앱 미설치 현황 조회에 실패했습니다. 시스템 관리자 권한이 필요합니다."
        />
      )}

      <Descriptions
        bordered
        size="small"
        column={1}
        title="조회 기준"
        style={{ marginBottom: 16 }}
        items={[
          {
            key: 'judgement',
            label: '미설치 판정',
            children:
              '마지막 앱 실행 기록(로그인·토큰 갱신 때마다 갱신)이 없고, 푸시 토큰(FCM)도 없는 사원. 두 조건을 모두 만족할 때만 미설치로 셉니다.',
          },
          {
            key: 'scope',
            label: '집계 모수',
            children:
              '여사원 현황 화면과 동일 — 재직 + 권한(여사원·조장) + 직무(판촉직·OSC직), 테스트·시스템 계정 제외. 여기에 앱 로그인 활성 + 사번 보유 사원만 남깁니다(안내해도 로그인할 수 없는 사원 제외).',
          },
          {
            key: 'caution',
            label: '해석 시 주의',
            children:
              '설치만 하고 로그인하지 않은 사원도 미설치에 포함됩니다. 앱 실행 기록 수집은 2026-06-12 부터라, 그 이전에만 앱을 쓰고 이후 한 번도 열지 않은 사원이 섞일 수 있습니다.',
          },
        ]}
      />

      <Space size="large" align="end" wrap>
        <Statistic title="미설치 추정" value={uninstalledCount} suffix="명" loading={isLoading} />
        <Statistic title="집계 모수" value={targetCount} suffix="명" loading={isLoading} />
        <Button
          icon={<DownloadOutlined />}
          loading={downloading}
          disabled={isLoading || isError}
          onClick={() =>
            run(UNINSTALLED_FEMALE_STAFF_EXPORT_PATH, '앱미설치여사원.xlsx', {
              totalCount: uninstalledCount,
            })
          }
        >
          명단 다운로드
        </Button>
      </Space>
    </Card>
  );
}
