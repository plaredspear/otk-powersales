import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import PermissionMatrixEditor, { type PermissionBit } from './PermissionMatrixEditor';

const OBJECT_BITS: PermissionBit[] = ['allowRead', 'allowCreate', 'allowEdit', 'allowDelete', 'viewAllRecords', 'modifyAllRecords'];

describe('PermissionMatrixEditor', () => {
  it('자원 목록을 렌더링한다 (Object 6비트)', () => {
    render(
      <PermissionMatrixEditor
        resources={[
          { name: 'Account', label: 'account' },
          { name: 'DKRetail__Claim__c', label: 'claim' },
        ]}
        bits={OBJECT_BITS}
        value={{}}
        onChange={() => {}}
      />,
    );

    expect(screen.getByText('Account')).toBeInTheDocument();
    expect(screen.getByText('DKRetail__Claim__c')).toBeInTheDocument();
    expect(screen.getAllByText('READ').length).toBeGreaterThan(0);
    expect(screen.getAllByText('VIEW_ALL').length).toBeGreaterThan(0);
  });

  it('체크박스 토글 시 onChange 가 새 value 와 함께 호출된다', () => {
    const onChange = vi.fn();
    render(
      <PermissionMatrixEditor
        resources={[{ name: 'Account' }]}
        bits={OBJECT_BITS}
        value={{}}
        onChange={onChange}
      />,
    );

    const checkboxes = screen.getAllByRole('checkbox');
    // 첫 데이터 row 의 첫 번째 비트 (allowRead) 토글
    fireEvent.click(checkboxes[0]);

    expect(onChange).toHaveBeenCalledWith({ Account: { allowRead: true } });
  });

  it('모든 비트 해제 시 자원 키 자체가 value 에서 제거된다', () => {
    const onChange = vi.fn();
    render(
      <PermissionMatrixEditor
        resources={[{ name: 'Account' }]}
        bits={OBJECT_BITS}
        value={{ Account: { allowRead: true } }}
        onChange={onChange}
      />,
    );

    const checkboxes = screen.getAllByRole('checkbox');
    fireEvent.click(checkboxes[0]); // allowRead 해제

    expect(onChange).toHaveBeenCalledWith({});
  });

  it('baseline 과 다른 row 는 changed 표시 (className 적용)', () => {
    render(
      <PermissionMatrixEditor
        resources={[
          { name: 'Account' },
          { name: 'Claim' },
        ]}
        bits={OBJECT_BITS}
        value={{ Account: { allowRead: true } }}
        baselineValue={{}}
        onChange={() => {}}
      />,
    );

    const changedRows = document.querySelectorAll('.permission-matrix-row-changed');
    expect(changedRows.length).toBe(1);
  });
});
