import { useState } from 'react';
import { Button, Space, Tooltip, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useActiveAgreementWord } from '@/hooks/agreementWord/useActiveAgreementWord';
import { usePermission } from '@/hooks/usePermission';
import AdminAgreementWordCreateModal from './AdminAgreementWordCreateModal';
import ActiveAgreementWordCard from './components/ActiveAgreementWordCard';

/**
 * 관리자 동의 약관 등록 페이지. (Spec #658 P2-W)
 *
 * 페이지 진입 시 활성 약관 1건 미리보기 + "신규 등록" 버튼 → Modal. 권한 가시성:
 * - `AGREEMENT_READ` 미보유 시 라우트 가드 (`PermissionRoute`) 가 ForbiddenResult 표시
 * - `AGREEMENT_WRITE` 미보유 시 등록 버튼 disabled (tooltip 안내)
 */
export default function AgreementWordsPage() {
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const { hasEntityPermission } = usePermission();
  const canWrite = hasEntityPermission('agreement_word', 'EDIT');
  const { data, isLoading } = useActiveAgreementWord();

  return (
    <>
      <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 16 }} align="center">
        <Typography.Title level={3} style={{ margin: 0 }}>
          동의 약관 등록
        </Typography.Title>
        <Tooltip title={canWrite ? undefined : '약관 등록 권한이 없습니다 (AGREEMENT_WRITE 필요).'}>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            disabled={!canWrite}
            onClick={() => setCreateModalOpen(true)}
          >
            신규 등록
          </Button>
        </Tooltip>
      </Space>

      <ActiveAgreementWordCard data={data} isLoading={isLoading} />

      <AdminAgreementWordCreateModal
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
      />
    </>
  );
}
