import { useState } from 'react';
import { Button, Card, Typography, App as AntdApp } from 'antd';
import { EnvironmentOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { fetchGpsTerms, recordGpsConsent } from '@/api/gps';
import { getCurrentPosition } from '@/lib/device';
import { useAuthStore } from '@/stores/authStore';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';

/** GPS 사용 동의 (레거시 home/gps). 약관 표시 → 동의 시 위치 권한 요청 + 서버 기록. */
export default function GpsConsentPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const applyAccessToken = useAuthStore((s) => s.applyAccessToken);
  const [loading, setLoading] = useState(false);

  const termsQuery = useQuery({ queryKey: ['gps-terms'], queryFn: fetchGpsTerms });

  const onAgree = async (terms: { agreementNumber: string | null }) => {
    setLoading(true);
    try {
      // 위치 권한 확보(레거시 동등) — 실패해도 동의 기록은 진행
      await getCurrentPosition().catch(() => undefined);
      const result = await recordGpsConsent(terms.agreementNumber ?? undefined);
      applyAccessToken(result.accessToken);
      message.success('GPS 사용에 동의했습니다');
      navigate('/', { replace: true });
    } catch (e) {
      message.error(e instanceof Error ? e.message : '동의 처리에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <DetailHeader title="GPS 사용 동의" />
      <QueryBoundary
        isLoading={termsQuery.isLoading}
        isError={termsQuery.isError}
        data={termsQuery.data}
        onRetry={termsQuery.refetch}
      >
        {(terms) => (
          <>
            <Card styles={{ body: { padding: 16 } }} style={{ marginBottom: 16 }}>
              <Typography.Title level={5}>
                <EnvironmentOutlined /> 위치 정보 수집 동의
              </Typography.Title>
              <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', fontSize: 14 }}>
                {terms.contents ?? '출근 등록 등 현장 업무를 위해 위치 정보를 수집합니다.'}
              </Typography.Paragraph>
            </Card>
            <Button type="primary" block size="large" loading={loading} onClick={() => onAgree(terms)}>
              동의하고 계속
            </Button>
          </>
        )}
      </QueryBoundary>
    </>
  );
}
