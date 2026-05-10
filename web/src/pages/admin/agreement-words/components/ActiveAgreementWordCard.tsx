import { Card, Collapse, Empty, Skeleton, Typography } from 'antd';
import type { AdminAgreementWordActiveResponse } from '@/api/agreementWord';

interface Props {
  data: AdminAgreementWordActiveResponse | null | undefined;
  isLoading: boolean;
}

/**
 * 활성 약관 미리보기 카드. (Spec #658 P2-W §1.4)
 *
 * 활성 약관 1건 표시 — 부재 시 안내 문구. 약관 본문은 길어서 기본 접힘 (Antd Collapse).
 */
export default function ActiveAgreementWordCard({ data, isLoading }: Props) {
  if (isLoading) {
    return (
      <Card title="현재 활성 약관" style={{ marginBottom: 16 }}>
        <Skeleton active paragraph={{ rows: 3 }} />
      </Card>
    );
  }

  if (!data) {
    return (
      <Card title="현재 활성 약관" style={{ marginBottom: 16 }}>
        <Empty description="활성 약관 없음 — 신규 등록 시 cycle batch 가 도래일자에 활성 토글합니다." />
      </Card>
    );
  }

  return (
    <Card title="현재 활성 약관" style={{ marginBottom: 16 }}>
      <div style={{ lineHeight: 1.8 }}>
        <div>
          <Typography.Text strong>약관 이름: </Typography.Text>
          <Typography.Text>{data.name}</Typography.Text>
        </div>
        <div>
          <Typography.Text strong>활성 시작 일자: </Typography.Text>
          <Typography.Text>{data.activeDate ?? '-'}</Typography.Text>
        </div>
        <div>
          <Typography.Text strong>다음 시행 일자: </Typography.Text>
          <Typography.Text>{data.afterActiveDate ?? '-'}</Typography.Text>
        </div>
      </div>
      <Collapse
        ghost
        style={{ marginTop: 12 }}
        items={[
          {
            key: 'contents',
            label: '약관 본문 펼치기',
            children: (
              <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
                {data.contents}
              </Typography.Paragraph>
            ),
          },
        ]}
      />
    </Card>
  );
}
