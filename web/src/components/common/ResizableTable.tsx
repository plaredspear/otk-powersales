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
 * 성능: 드래그 중(onResize) 에는 React state 를 단 한 번도 갱신하지 않는다 — 매 mousemove 마다
 * 리렌더가 일어나지 않도록, 드래그 중 시각 가이드(핸들 transform) 는 ref 로 잡은 핸들 DOM 에
 * 직접 쓴다. width 는 마우스를 놓는 순간(onResizeStop) 에만 부모로 1회 commit 하여 테이블이
 * 그때 단 1회만 리렌더된다.
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

  // 드래그 중 시각 가이드를 직접 조작할 핸들 DOM 참조 + 드래그 진행 중 최종 width.
  const handleRef = useRef<HTMLSpanElement>(null);
  const draggingWidthRef = useRef(width ?? 0);

  // onResize: state 갱신 없이 핸들 DOM 의 transform 만 직접 갱신 → 드래그 중 리렌더 0회.
  // (메모이제이션은 React Compiler 가 자동 처리하므로 useCallback 불필요)
  const handleResize = (_e: React.SyntheticEvent, { size }: ResizeCallbackData) => {
    draggingWidthRef.current = size.width;
    if (handleRef.current && width) {
      handleRef.current.style.transform = `translateX(${size.width - width}px)`;
    }
  };

  // onResizeStop: 가이드 초기화 후 최종 width 를 부모로 1회 commit (테이블 리렌더 1회).
  const handleResizeStop = () => {
    if (handleRef.current) {
      handleRef.current.style.transform = '';
    }
    onResizeStop?.(draggingWidthRef.current);
  };

  if (!width) {
    return <th {...restProps} />;
  }

  return (
    <Resizable
      width={width}
      height={0}
      axis="x"
      minConstraints={[minWidth, 0]}
      // handle 을 함수로 제공해야 react-resizable 의 내부 ref(libRef, 드래그 계산용) 와
      // 시각 가이드 조작용 handleRef 를 둘 다 연결할 수 있다. element 로 주면 라이브러리가
      // cloneElement 로 자기 ref 만 덮어써 handleRef 가 무시된다.
      handle={(_handleAxis, libRef) => (
        <span
          ref={(node) => {
            // 라이브러리 내부 ref(드래그 계산용) 와 가이드 조작용 handleRef 를 둘 다 채운다.
            handleRef.current = node;
            (libRef as React.MutableRefObject<HTMLSpanElement | null>).current = node;
          }}
          className="resizable-handle"
          onClick={(e) => e.stopPropagation()}
        />
      )}
      onResize={handleResize}
      onResizeStop={handleResizeStop}
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
