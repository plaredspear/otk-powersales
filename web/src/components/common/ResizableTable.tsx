import { useRef, useState } from 'react';
import { Table } from 'antd';
import type { TableProps } from 'antd';
import type { ColumnsType, ColumnType } from 'antd/es/table';
import { Resizable } from 'react-resizable';
import type { ResizeCallbackData } from 'react-resizable';
import './ResizableTable.css';

/**
 * 드래그 중 세로 가이드 라인을 제어하기 위한 콜백 묶음.
 * ResizableTable 이 ref 로 잡은 가이드 라인 DOM 을 직접 조작하는 핸들러를 헤더 셀에 내려준다.
 */
interface ResizeGuide {
  /** 드래그 시작 — 핸들 우측 경계의 화면 X 좌표(px, 테이블 컨테이너 기준)를 받아 가이드를 표시. */
  start: (anchorX: number) => void;
  /** 드래그 중 — 시작점 대비 가로 이동량(px)만큼 가이드를 좌우로 이동. */
  move: (deltaX: number) => void;
  /** 드래그 종료 — 가이드 숨김. */
  stop: () => void;
}

/**
 * 리사이즈 핸들이 부착된 테이블 헤더 셀.
 *
 * width 가 지정된 컬럼만 Resizable 로 감싸 좌우 드래그를 받는다. width 미지정 컬럼
 * (selection / expand 등 antd 내부 컬럼 포함) 은 그대로 <th> 렌더링하여 antd 자동 레이아웃을 유지.
 *
 * 성능: 드래그 중(onResize) 에는 React state 를 단 한 번도 갱신하지 않는다 — 매 mousemove 마다
 * 리렌더가 일어나지 않도록, 드래그 중 시각 가이드(테이블 전체 높이 세로 라인) 는 ref 로 잡은
 * 가이드 DOM 에 직접 쓴다. width 는 마우스를 놓는 순간(onResizeStop) 에만 부모로 1회 commit 하여
 * 테이블이 그때 단 1회만 리렌더된다.
 * 핸들 클릭은 정렬 등 헤더 onClick 으로 전파되지 않도록 stopPropagation.
 */
function ResizableHeaderCell(
  props: React.HTMLAttributes<HTMLTableCellElement> & {
    width?: number;
    minWidth?: number;
    onResizeStop?: (width: number) => void;
    guide?: ResizeGuide;
  },
) {
  const { width, minWidth = 60, onResizeStop, guide, ...restProps } = props;

  // 드래그 중 핸들 위치 추적을 위한 핸들 DOM 참조 + 드래그 진행 중 최종 width.
  const handleRef = useRef<HTMLSpanElement>(null);
  const draggingWidthRef = useRef(width ?? 0);

  // onResizeStart: 핸들의 현재 우측 경계 X 좌표를 기준점으로 가이드 라인을 띄운다.
  const handleResizeStart = () => {
    draggingWidthRef.current = width ?? 0;
    if (handleRef.current && guide) {
      const rect = handleRef.current.getBoundingClientRect();
      // 핸들은 셀 우측 경계에 걸쳐 있으므로 중앙(rect.left + width/2)을 컬럼 경계로 본다.
      guide.start(rect.left + rect.width / 2);
    }
  };

  // onResize: state 갱신 없이 가이드 라인만 직접 이동 → 드래그 중 리렌더 0회.
  // (메모이제이션은 React Compiler 가 자동 처리하므로 useCallback 불필요)
  const handleResize = (_e: React.SyntheticEvent, { size }: ResizeCallbackData) => {
    draggingWidthRef.current = size.width;
    if (width) {
      guide?.move(size.width - width);
    }
  };

  // onResizeStop: 가이드 숨김 후 최종 width 를 부모로 1회 commit (테이블 리렌더 1회).
  const handleResizeStop = () => {
    guide?.stop();
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
      // 위치 추적용 handleRef 를 둘 다 연결할 수 있다. element 로 주면 라이브러리가
      // cloneElement 로 자기 ref 만 덮어써 handleRef 가 무시된다.
      handle={(_handleAxis, libRef) => (
        <span
          ref={(node) => {
            // 라이브러리 내부 ref(드래그 계산용) 와 위치 추적용 handleRef 를 둘 다 채운다.
            handleRef.current = node;
            (libRef as React.MutableRefObject<HTMLSpanElement | null>).current = node;
          }}
          className="resizable-handle"
          onClick={(e) => e.stopPropagation()}
        />
      )}
      onResizeStart={handleResizeStart}
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
function ResizableTable<T extends object>({
  columns,
  minColumnWidth = 60,
  components,
  size = 'small',
  ...rest
}: ResizableTableProps<T>) {
  // 컬럼 인덱스별 width override. 사용자가 드래그를 마친 컬럼만 항목이 채워진다.
  const [widthOverrides, setWidthOverrides] = useState<Record<number, number>>({});

  // 테이블 컨테이너 + 드래그 중 세로 가이드 라인 DOM 참조. 가이드는 드래그 중에만
  // ref 로 직접 조작하므로 state/리렌더가 일어나지 않는다. 드래그 시작 시점의 기준 X 좌표를
  // 보관해 두고 move 의 delta 를 더해 라인 위치를 갱신한다.
  const containerRef = useRef<HTMLDivElement>(null);
  const guideRef = useRef<HTMLDivElement>(null);
  const guideAnchorRef = useRef(0);

  // 헤더 셀에 내려주는 가이드 제어 콜백. 좌표는 모두 화면(viewport) 기준 px 이며,
  // 컨테이너 기준 상대 좌표로 변환하여 가이드 라인의 left 에 직접 쓴다.
  const guide: ResizeGuide = {
    start: (anchorX) => {
      const container = containerRef.current;
      const line = guideRef.current;
      if (!container || !line) return;
      // 컨테이너 상대 좌표 = 화면 X − 컨테이너 화면 left. 컨테이너는 스크롤되지 않으므로
      // (가로 스크롤은 내부 antd div 가 담당) 이 값이 화면에 보이는 핸들 위치와 항상 일치한다.
      guideAnchorRef.current = anchorX - container.getBoundingClientRect().left;
      line.style.left = `${guideAnchorRef.current}px`;
      line.style.opacity = '1';
      // 드래그 중 텍스트 선택/커서 흔들림 방지.
      document.body.classList.add('resizable-table-resizing');
    },
    move: (deltaX) => {
      const line = guideRef.current;
      if (!line) return;
      line.style.left = `${guideAnchorRef.current + deltaX}px`;
    },
    stop: () => {
      if (guideRef.current) {
        guideRef.current.style.opacity = '0';
      }
      document.body.classList.remove('resizable-table-resizing');
    },
  };

  const handleResizeStop = (index: number) => (width: number) => {
    setWidthOverrides((prev) => ({ ...prev, [index]: width }));
  };

  const resizableColumns: ColumnsType<T> = columns.map((col, index) => {
    const baseWidth =
      widthOverrides[index] ?? (typeof col.width === 'number' ? col.width : undefined);

    // width 가 지정된 컬럼은 폭을 줄였을 때 셀 내용이 줄바꿈되지 않고 "..." 로 축약되도록
    // ellipsis 를 기본 적용한다 (리사이즈로 폭을 넓혀 가린 내용을 확인하는 게 이 컴포넌트의 의도).
    // 호출부가 ellipsis 를 명시한 경우 그 값을 우선한다.
    const colType = col as ColumnType<T>;
    const ellipsis = colType.ellipsis ?? (baseWidth !== undefined ? true : undefined);

    return {
      ...col,
      width: baseWidth,
      ellipsis,
      onHeaderCell: (column) => ({
        width: (column as ColumnType<T>).width as number | undefined,
        minWidth: minColumnWidth,
        onResizeStop: handleResizeStop(index),
        guide,
      }),
    } as ColumnType<T>;
  });

  // scroll.x 를 'max-content'(내용 기준) 로 두면 antd 가 각 컬럼을 셀 내용의 자연 폭 이상으로
  // 유지해, 지정 width 보다 좁혀도 내용 폭까지만 줄고 ellipsis 가 발동하지 않는다.
  // 대신 모든 컬럼 width 의 합(고정 px) 을 scroll.x 로 주면 antd 가 table-layout: fixed 로 전환되어
  // 각 컬럼이 지정 width 를 그대로 따르고, 내용이 넘치면 "..." 로 축약된다 (드래그로 줄인 폭도 반영).
  // width 미지정 컬럼이 섞이면 합산이 부정확하므로, 그런 경우엔 'max-content' 로 안전하게 폴백.
  const totalWidth = resizableColumns.reduce(
    (sum, col) => {
      if (sum === null) return null;
      const w = (col as ColumnType<T>).width;
      return typeof w === 'number' ? sum + w : null;
    },
    0 as number | null,
  );
  // 호출부에서 scroll 을 지정하면 그 값을 우선.
  const scroll = rest.scroll ?? { x: totalWidth ?? 'max-content' };

  return (
    <div className="resizable-table" ref={containerRef}>
      {/* 드래그 중에만 보이는 테이블 전체 높이 세로 가이드 라인. 평상시 opacity 0. */}
      <div className="resizable-guide-line" ref={guideRef} aria-hidden="true" />
      <Table<T>
        {...rest}
        size={size}
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

// antd Table 의 정적 서브컴포넌트(Summary / Column / ColumnGroup) 를 그대로 노출하여,
// 기존 `Table.Summary` 등을 쓰던 사용처를 `ResizableTable.Summary` 로 그대로 치환할 수 있게 한다.
// generic 함수 컴포넌트에 정적 프로퍼티를 부착하므로 타입을 명시적으로 합성한다.
type ResizableTableComponent = typeof ResizableTable & {
  Summary: typeof Table.Summary;
  Column: typeof Table.Column;
  ColumnGroup: typeof Table.ColumnGroup;
};

const ResizableTableWithStatics = ResizableTable as ResizableTableComponent;
ResizableTableWithStatics.Summary = Table.Summary;
ResizableTableWithStatics.Column = Table.Column;
ResizableTableWithStatics.ColumnGroup = Table.ColumnGroup;

export default ResizableTableWithStatics;
