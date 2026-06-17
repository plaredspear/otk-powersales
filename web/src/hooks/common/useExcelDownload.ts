import { useState, useCallback, useRef } from 'react';
import { message } from 'antd';
import { downloadExcel } from '@/lib/excelDownload';

interface DownloadOptions {
  method?: 'get' | 'post';
  params?: Record<string, unknown>;
  data?: unknown;
  /**
   * 다운로드 직전 검사. 0 또는 음수를 반환하면 "데이터 없음" 안내 후 중단,
   * `maxRows` 초과 시 안내 후 진행한다. 미지정 시 검사 생략.
   */
  totalCount?: number;
  /** 서버 export 상한. `totalCount` 가 이 값을 넘으면 잘림 안내. */
  maxRows?: number;
}

/**
 * 엑셀 다운로드 공통 훅 — loading 상태 / 에러 message / 최대 건수 안내를 캡슐화.
 *
 * 페이지는 `run(path, fallbackName, options)` 호출과 `downloading` 플래그(버튼 loading)만 사용한다.
 * 에러는 서버가 보낸 메시지(blob 본문 추출)를 우선 노출하고, 없으면 기본 문구로 폴백한다.
 *
 * @example
 *   const { run, downloading } = useExcelDownload();
 *   <Button loading={downloading} onClick={() =>
 *     run('/api/v1/admin/promotions/export', '행사마스터.xlsx', { params, totalCount, maxRows: 50000 })
 *   }>엑셀 다운로드</Button>
 */
export function useExcelDownload() {
  const [downloading, setDownloading] = useState(false);
  // 클릭~setDownloading 사이 짧은 윈도우의 연타 차단 (loading 플래그보다 즉시 반영).
  const inFlight = useRef(false);

  const run = useCallback(
    async (path: string, fallbackName: string, options?: DownloadOptions): Promise<void> => {
      if (inFlight.current) return;
      const { totalCount, maxRows, ...downloadOptions } = options ?? {};

      if (totalCount != null && totalCount <= 0) {
        message.warning('다운로드할 데이터가 없습니다');
        return;
      }
      if (totalCount != null && maxRows != null && totalCount > maxRows) {
        message.warning(
          `조회 결과가 ${totalCount.toLocaleString()}건입니다. 최대 ${maxRows.toLocaleString()}건까지만 다운로드됩니다.`,
        );
      }

      inFlight.current = true;
      setDownloading(true);
      try {
        await downloadExcel(path, fallbackName, downloadOptions);
      } catch (err) {
        message.error(err instanceof Error && err.message ? err.message : '엑셀 다운로드에 실패했습니다');
      } finally {
        inFlight.current = false;
        setDownloading(false);
      }
    },
    [],
  );

  return { run, downloading };
}
