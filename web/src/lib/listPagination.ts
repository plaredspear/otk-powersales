import type { TablePaginationConfig } from 'antd';

/**
 * 목록 테이블 페이지네이션의 공통 페이지 사이즈 옵션.
 * 행사마스터 / 진열스케줄마스터 등 목록 페이지가 동일하게 사용한다.
 */
export const PAGE_SIZE_OPTIONS = [20, 50, 100];

export interface BuildListPaginationArgs {
  /** 0-indexed 현재 페이지 (백엔드 페이지 인덱스와 동일). */
  page: number;
  /** 현재 페이지 사이즈. */
  pageSize: number;
  /** 전체 건수. */
  total: number;
  /** 페이지 이동 시 호출. 0-indexed 페이지 번호를 전달한다. */
  onPageChange: (page: number) => void;
  /** 페이지 사이즈 변경 시 호출. 새 사이즈를 전달한다. */
  onSizeChange: (size: number) => void;
}

/**
 * 목록 테이블의 antd `pagination` 설정 객체를 생성하는 헬퍼.
 *
 * page 사이즈 옵션 / showSizeChanger / showTotal(총 N건) / 1-indexed 보정 /
 * "사이즈 변경 vs 페이지 이동" 분기를 한곳에 캡슐화한다.
 *
 * 상태 보관 방식(URL query vs useState)과 사이즈/페이지 변경 시의 부수효과
 * (선택 행 초기화 등)는 호출부가 onPageChange / onSizeChange 콜백 안에서 처리한다.
 *
 * - page 는 0-indexed 로 주고받고, UI 에는 1-indexed 로 노출한다.
 * - 사이즈가 바뀌면 onSizeChange 만, 같으면 onPageChange 만 호출된다.
 */
export function buildListPagination({
  page,
  pageSize,
  total,
  onPageChange,
  onSizeChange,
}: BuildListPaginationArgs): TablePaginationConfig {
  return {
    current: page + 1,
    pageSize,
    total,
    showSizeChanger: true,
    pageSizeOptions: PAGE_SIZE_OPTIONS,
    showTotal: (t) => `총 ${t}건`,
    onChange: (nextPage, size) => {
      if (size !== pageSize) {
        onSizeChange(size);
      } else {
        onPageChange(nextPage - 1);
      }
    },
  };
}
