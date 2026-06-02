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

  it('드래그 중에는 세로 가이드 라인을 표시하고, 놓을 때 숨긴다', () => {
    const { container } = renderTable();
    const handle = container.querySelector('.resizable-handle') as HTMLElement;
    const guideLine = container.querySelector('.resizable-guide-line') as HTMLElement;
    expect(guideLine).not.toBeNull();

    // 드래그 시작 → 이동: 가이드 라인이 보인다 (opacity 1). 테이블 컬럼은 아직 미변경.
    fireEvent.mouseDown(handle, { clientX: 120 });
    fireEvent.mouseMove(handle, { clientX: 180 });
    expect(guideLine.style.opacity).toBe('1');
    // 드래그 중 body 에 리사이즈 클래스가 부여되어 커서/선택을 고정한다.
    expect(document.body.classList.contains('resizable-table-resizing')).toBe(true);

    // 드래그 종료: 가이드 라인은 숨겨지고 (opacity 0), body 클래스도 해제된다.
    fireEvent.mouseUp(handle, { clientX: 180 });
    expect(guideLine.style.opacity).toBe('0');
    expect(document.body.classList.contains('resizable-table-resizing')).toBe(false);
  });

  describe('그룹 헤더(children) 구조', () => {
    interface GroupRow {
      key: string;
      branch: string;
      a: string;
      b: string;
    }

    const GROUP_DATA: GroupRow[] = [{ key: '1', branch: '서울', a: '10', b: '20' }];

    const GROUP_COLUMNS: ColumnsType<GroupRow> = [
      { title: '지점', dataIndex: 'branch', width: 140 },
      {
        title: '실적',
        children: [
          { title: '당월', dataIndex: 'a', width: 100 },
          { title: '전월', dataIndex: 'b', width: 100 },
          // width 미지정 leaf — 핸들이 붙지 않아야 한다
          { title: '비고', key: 'note', render: () => <span>-</span> },
        ],
      },
    ];

    function renderGroupTable() {
      return render(
        <ResizableTable<GroupRow>
          rowKey="key"
          columns={GROUP_COLUMNS}
          dataSource={GROUP_DATA}
          pagination={false}
        />,
      );
    }

    it('그룹 헤더 안의 leaf 컬럼 중 width 가 지정된 것에만 핸들을 부착한다', () => {
      const { container } = renderGroupTable();
      // top-level 지점(140) + children 당월(100) + 전월(100) = 3개. 그룹 헤더 '실적' 과
      // width 미지정 leaf '비고' 는 핸들 없음.
      const handles = container.querySelectorAll('.resizable-handle');
      expect(handles.length).toBe(3);
    });

    it('그룹 헤더와 leaf 컬럼을 모두 렌더링한다', () => {
      renderGroupTable();
      expect(screen.getAllByText('실적').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('당월').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('전월').length).toBeGreaterThanOrEqual(1);
    });
  });
});
