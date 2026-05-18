import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AttendInfoDeleteConfirmModal from './AttendInfoDeleteConfirmModal';
import type { AttendInfoListItem } from '@/api/attendInfo';

const sampleTarget: AttendInfoListItem = {
  id: 1,
  name: 'AI0001',
  employeeCode: '20120253',
  employeeName: '홍길동',
  employeeJobCode: '판촉직',
  attendType: '14',
  attendTypeName: '연차',
  startDate: '20260518',
  endDate: '20260522',
  status: 'N',
  createdAt: '2026-05-18T10:00:00',
  createdByName: null,
};

describe('AttendInfoDeleteConfirmModal', () => {
  it('대상 정보와 cascade 안내 텍스트를 노출한다', () => {
    render(
      <AttendInfoDeleteConfirmModal
        target={sampleTarget}
        loading={false}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByText('근태정보 삭제')).toBeInTheDocument();
    expect(screen.getByText('AI0001')).toBeInTheDocument();
    expect(screen.getByText(/홍길동/)).toBeInTheDocument();
    expect(screen.getByText(/20120253/)).toBeInTheDocument();
    expect(screen.getByText(/연차/)).toBeInTheDocument();
    expect(screen.getByText(/2026-05-18 ~ 2026-05-22/)).toBeInTheDocument();
    expect(screen.getByText('삭제 시 다음이 함께 처리됩니다')).toBeInTheDocument();
    expect(
      screen.getByText(/연결된 여사원 연차 일정이 자동 삭제됩니다/),
    ).toBeInTheDocument();
  });

  it('삭제 버튼 클릭 시 onConfirm 호출', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    render(
      <AttendInfoDeleteConfirmModal
        target={sampleTarget}
        loading={false}
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />,
    );
    await user.click(screen.getByRole('button', { name: '삭제' }));
    expect(onConfirm).toHaveBeenCalled();
  });

  it('취소 버튼 클릭 시 onCancel 호출', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(
      <AttendInfoDeleteConfirmModal
        target={sampleTarget}
        loading={false}
        onConfirm={vi.fn()}
        onCancel={onCancel}
      />,
    );
    await user.click(screen.getByRole('button', { name: '취소' }));
    expect(onCancel).toHaveBeenCalled();
  });
});
