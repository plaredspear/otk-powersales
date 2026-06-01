import { useRef, useState } from 'react';
import { Table } from 'antd';
import type { TableProps } from 'antd';
import type { ColumnsType, ColumnType } from 'antd/es/table';
import { Resizable } from 'react-resizable';
import type { ResizeCallbackData } from 'react-resizable';
import './ResizableTable.css';

/**
 * 리사이즈 핸들이 부착된 테이블 헤더 셀.
 *
 * width 가 지정된 컬럼만 Resizable 로 감싸 좌우 드래그를 받는다. width 미지정 컬럼
 * (selection / expand 등 antd 내부 컬럼 포함) 은 그대로 <th> 렌더링하여 antd 자동 레이아웃을 유지.
 *
 * 성능: 드래그 중(onResize) 에는 부모 state 를 갱신하지 않는다 — 매 픽셀마다 테이블 전체가
 * 리렌더되는 것을 막기 위함. 드래그 중에는 핸들에 transform 가이드만 보여주고, 마우스를 놓는
 * 순간(onResizeStop) 에만 최종 width 를 부모로 1회 commit 한다.
 * 핸들 클릭은 정렬 등 헤더 onClick 으로 전파되지 않도록 stopPropagation.
 */
function ResizableHeaderCell(
  props: React.HTMLAttributes<HTMLTableCellElement> & {
    width?: number;
    minWidth?: number;
    onResizeStop?: (width: number) => void;
  },
) {
  const { width, minWidth = 60, onResizeStop, ...restProps } = props;

  // 드래그 중 핸들에만 반영하는 임시 오프셋. 종료 시 0 으로 되돌리고 부모에 최종 width 를 commit.
  const [dragOffset, setDragOffset] = useState(0);
  const draggingWidthRef = useRef(width ?? 0);

  if (!width) {
    return <th {...restProps} />;
  }

  return (
    <Resizable
      width={width}
      height={0}
      axis="x"
      minConstraints={[minWidth, 0]}
      handle={
        <span
          className="resizable-handle"
          // 드래그 중인 폭 변화량만큼 핸들을 따라 이동시켜 시각 가이드를 제공.
          style={dragOffset ? { transform: `translateX(${dragOffset}px)` } : undefined}
          onClick={(e) => e.stopPropagation()}
        />
      }
      onResize={(_e: React.SyntheticEvent, { size }: ResizeCallbackData) => {
        // 테이블 리렌더 없이 핸들 위치만 갱신 (가벼운 로컬 state).
        draggingWidthRef.current = size.width;
        setDragOffset(size.width - width);
      }}
      onResizeStop={() => {
        setDragOffset(0);
        onResizeStop?.(draggingWidthRef.current);
      }}
      draggableOpts={{ enableUserSelectHack: false }}
    >
      <th {...restProps} />
    </Resizable>
  );
}

export interface ResizableTableProps<T> extends Omit<TableProps<T>, 'columns'> {
  columns: ColumnsType<T>;
  /** 드래그로 줄일 수 있는 컬럼 최소 폭 (px). 기본 60 */
  minColumnWidth?: number;
}

/**
 * 컬럼 가로 폭을 드래그로 조절할 수 있는 antd Table 래퍼.
 *
 * antd <Table> 의 모든 props 를 그대로 위임하고, columns 의 width 만 내부 state 로 관리한다.
 * 헤더 우측 경계를 드래그하면 마우스를 놓는 순간 해당 컬럼 width 가 갱신되어, ellipsis 로 "..."
 * 축약된 셀 내용을 폭을 넓혀 확인할 수 있다. 정렬/필터/fixed 등 기존 컬럼 옵션은 모두 유지된다.
 *
 * 성능: 드래그 도중에는 헤더 핸들만 움직이고 테이블은 리렌더하지 않는다. width state 갱신은
 * 드래그 종료 시 1회뿐이라, 컬럼/행이 많은 페이지에서도 드래그가 부드럽다.
 *
 * 사용처는 antd Table 을 그대로 ResizableTable 로 치환하면 된다 (props 호환).
 */
export default function ResizableTable<T extends object>({
  columns,
  minColumnWidth = 60,
  components,
  ...rest
}: ResizableTableProps<T>) {
  // 컬럼 인덱스별 width override. 사용자가 드래그를 마친 컬럼만 항목이 채워진다.
  const [widthOverrides, setWidthOverrides] = useState<Record<number, number>>({});

  const handleResizeStop = (index: number) => (width: number) => {
    setWidthOverrides((prev) => ({ ...prev, [index]: width }));
  };

  const resizableColumns: ColumnsType<T> = columns.map((col, index) => {
    const baseWidth =
      widthOverrides[index] ?? (typeof col.width === 'number' ? col.width : undefined);

    return {
      ...col,
      width: baseWidth,
      onHeaderCell: (column) => ({
        width: (column as ColumnType<T>).width as number | undefined,
        minWidth: minColumnWidth,
        onResizeStop: handleResizeStop(index),
      }),
    } as ColumnType<T>;
  });

  // 컬럼 폭을 넓혔을 때 테이블이 잘리지 않도록 가로 스크롤 기본 활성화.
  // 호출부에서 scroll 을 지정하면 그 값을 우선.
  const scroll = rest.scroll ?? { x: 'max-content' };

  return (
    <div className="resizable-table">
      <Table<T>
        {...rest}
        scroll={scroll}
        columns={resizableColumns}
        components={{
          ...components,
          header: {
            ...components?.header,
            cell: ResizableHeaderCell,
          },
        }}
      />
    </div>
  );
}
