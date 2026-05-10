import { useState } from 'react';
import { Button, Modal, notification, Tooltip } from 'antd';
import { useQueryClient } from '@tanstack/react-query';
import { isAxiosError } from 'axios';
import { useAccountDelete } from '@/hooks/account/useAccountDelete';
import { isApiErrorBody } from '@/api/types';
import type { Account } from '@/api/account';

export interface AccountDeleteActionProps {
  account: Account;
}

/**
 * 거래처 목록 row action — 삭제 버튼 + 확인 dialog + mutation. (Spec #642 P2-W)
 *
 * Backend `DELETE /api/v1/admin/accounts/{id}` (`ACCOUNT_DELETE` 권한 필요) 호출.
 * - SAP 동기 거래처(`externalKey != null`) 는 사전 차단 — 버튼 disabled + tooltip
 * - 정상 클릭 → confirm Modal → 200 시 notification.success + 목록 invalidate
 * - 4xx/5xx 응답 → 에러 코드별 notification 매핑 (스펙 §4)
 *
 * 권한 가시성(`ACCOUNT_DELETE` 부재 시 hidden)은 부모(목록 페이지)가 처리.
 */
export default function AccountDeleteAction({ account }: AccountDeleteActionProps) {
  const queryClient = useQueryClient();
  const mutation = useAccountDelete();
  const [open, setOpen] = useState(false);
  const isSapSynced = account.externalKey !== null;

  const handleConfirm = async () => {
    try {
      await mutation.mutateAsync(account.id);
      notification.success({
        message: '거래처가 삭제되었습니다.',
        description: account.name ?? undefined,
      });
      setOpen(false);
    } catch (err) {
      handleDeleteError(err, queryClient);
      setOpen(false);
    }
  };

  if (isSapSynced) {
    return (
      <Tooltip title="SAP 동기 거래처는 삭제할 수 없습니다.">
        <Button size="small" danger disabled>
          삭제
        </Button>
      </Tooltip>
    );
  }

  return (
    <>
      <Button size="small" danger onClick={() => setOpen(true)} loading={mutation.isPending}>
        삭제
      </Button>
      <Modal
        open={open}
        title="거래처를 삭제하시겠습니까?"
        okText="삭제"
        cancelText="취소"
        okType="danger"
        confirmLoading={mutation.isPending}
        onOk={handleConfirm}
        onCancel={() => {
          if (!mutation.isPending) setOpen(false);
        }}
        maskClosable={!mutation.isPending}
        closable={!mutation.isPending}
        destroyOnHidden
      >
        <div style={{ lineHeight: 1.8 }}>
          <div>거래처명 : {account.name ?? '-'}</div>
          <div>※ 삭제된 거래처는 목록에서 제외됩니다.</div>
          <div>※ SAP 동기 거래처(거래처코드 보유)는 삭제할 수 없습니다.</div>
        </div>
      </Modal>
    </>
  );
}

function handleDeleteError(
  err: unknown,
  queryClient: ReturnType<typeof useQueryClient>,
): void {
  if (isAxiosError(err)) {
    const status = err.response?.status;
    const body = err.response?.data;
    if (isApiErrorBody(body)) {
      const code = body.error!.code;
      const message = body.error!.message;
      switch (code) {
        case 'ACCOUNT_DELETE_BLOCKED_SAP_SYNCED':
          notification.error({ message: '거래처 삭제 실패', description: message });
          return;
        case 'ACCOUNT_NOT_FOUND':
          notification.error({
            message: '거래처 삭제 실패',
            description: '거래처를 찾을 수 없습니다. 목록을 새로고침해 주세요.',
          });
          // 다른 관리자가 먼저 삭제한 케이스 — 목록 invalidate 로 회복
          void queryClient.invalidateQueries({ queryKey: ['admin', 'accounts'] });
          return;
        default:
          notification.error({ message: '거래처 삭제 실패', description: message });
          return;
      }
    }
    if (status === 401) {
      // axios interceptor 가 로그인 리다이렉트 처리
      return;
    }
    if (status === 403) {
      notification.error({ message: '거래처 삭제 실패', description: '삭제 권한이 없습니다.' });
      return;
    }
    if (status && status >= 500) {
      notification.error({
        message: '거래처 삭제 실패',
        description: '삭제 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
      });
      return;
    }
  }
  notification.error({
    message: '거래처 삭제 실패',
    description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.',
  });
}
