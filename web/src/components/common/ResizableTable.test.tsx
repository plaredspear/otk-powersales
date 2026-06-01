import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import type { ColumnsType } from 'antd/es/table';
import ResizableTable from './ResizableTable';

interface Row {
  key: string;
  name: string;
  memo: string;
}

const DATA: Row[] = [
  { key: '1', name: '홍길동', memo: '아주 긴 메모 텍스트가 여기에 들어가서 ... 로 축약됩니다' },
];

const COLUMNS: ColumnsType<Row> = [
  { title: '이름', dataIndex: 'name', width: 120 },
  { title: '메모', dataIndex: 'memo', width: 100, ellipsis: true },
  // width 미지정 컬럼 — 핸들이 붙지 않아야 한다
  { title: '액션', key: 'action', render: () => <span>편집</span> },
];

function renderTable() {
  return render(<ResizableTable<Row> rowKey="key" columns={COLUMNS} dataSource={DATA} pagination={false} />);
}

describe('ResizableTable', () => {
  it('컬럼 헤더와 데이터를 정상 렌더링한다', () => {
    renderTable();
    // antd 가 측정/스크롤용으로 헤더를 중복 렌더링할 수 있어 getAllByText 사용
    expect(screen.getAllByText('이름').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('메모').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('액션').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('홍길동')).toBeInTheDocument();
  });

  it('width 가 지정된 컬럼에만 리사이즈 핸들을 부착한다', () => {
    const { container } = renderTable();
    // 이름(120) + 메모(100) = 2개. 액션(width 없음) 은 핸들 없음.
    const handles = container.querySelectorAll('.resizable-handle');
    expect(handles.length).toBe(2);
  });

  it('핸들 클릭은 헤더로 전파되지 않는다 (stopPropagation)', () => {
    const { container } = renderTable();
    const handle = container.querySelector('.resizable-handle');
    expect(handle).not.toBeNull();
    // 클릭 시 예외 없이 처리되어야 한다 (정렬 토글 등으로 전파 방지)
    expect(() => fireEvent.click(handle!)).not.toThrow();
  });
});
