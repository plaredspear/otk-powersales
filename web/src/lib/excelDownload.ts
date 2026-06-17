import client from '@/api/client';
import type { AxiosError, AxiosRequestConfig } from 'axios';

const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

/**
 * 엑셀 export 서버 상한 — 백엔드 export 서비스의 `EXPORT_MAX_ROWS` 와 동일.
 * 페이징형 export 호출처가 `useExcelDownload().run(..., { totalCount, maxRows })` 의 잘림 경고에 사용한다.
 */
export const EXCEL_EXPORT_MAX_ROWS = 50_000;

/**
 * 응답 Content-Disposition 헤더에서 파일명 추출 (RFC 5987 `filename*=UTF-8''` 우선, quoted 형식 fallback).
 * 헤더가 없거나 파싱 실패 시 `fallbackName` 반환.
 */
function resolveFilename(contentDisposition: string | undefined, fallbackName: string): string {
  if (!contentDisposition) return fallbackName;
  const utfMatch = contentDisposition.match(/filename\*=UTF-8''([^;\n]+)/i);
  if (utfMatch) return decodeURIComponent(utfMatch[1]);
  const match = contentDisposition.match(/filename="?([^";\n]+)"?/);
  if (match) return decodeURIComponent(match[1]);
  return fallbackName;
}

/** blob 응답을 브라우저 다운로드로 트리거 (anchor click + objectURL 해제). */
function triggerDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

/**
 * 엑셀(xlsx) 파일을 서버에서 받아 브라우저 다운로드로 저장하는 공통 유틸.
 *
 * 모든 엑셀 export 호출처가 반복하던 boilerplate (responseType:'blob' → Content-Disposition 파싱 →
 * Blob 생성 → anchor click → objectURL 해제) 를 한 곳으로 통합한다.
 *
 * 파일명은 서버 Content-Disposition 헤더를 우선 사용하고, 없으면 `fallbackName` 으로 저장한다.
 *
 * @param path     export 엔드포인트 경로 (예: `/api/v1/admin/promotions/export`)
 * @param fallbackName 헤더에 파일명이 없을 때 사용할 기본 파일명 (확장자 포함)
 * @param options  HTTP method / 쿼리 파라미터 / POST body. 기본 GET.
 */
export async function downloadExcel(
  path: string,
  fallbackName: string,
  options?: {
    method?: 'get' | 'post';
    params?: Record<string, unknown>;
    data?: unknown;
  },
): Promise<void> {
  const method = options?.method ?? 'get';
  const config: AxiosRequestConfig = {
    method,
    url: path,
    params: options?.params,
    responseType: 'blob',
  };
  if (method === 'post') config.data = options?.data ?? {};

  try {
    const res = await client.request<Blob>(config);
    const contentDisposition = res.headers['content-disposition'] as string | undefined;
    const filename = resolveFilename(contentDisposition, fallbackName);
    const blob = new Blob([res.data], { type: XLSX_MIME });
    triggerDownload(blob, filename);
  } catch (err) {
    // blob 모드에서는 4xx/5xx 응답 body 도 Blob 으로 파싱되므로,
    // 서버가 보낸 실제 에러 메시지(JSON {message}) 를 텍스트로 풀어 던진다.
    // 파싱 성공 여부와 throw 를 분리 — JSON 이 아닌 평문/HTML 응답이면 SyntaxError 를
    // 사용자에게 노출하지 않고 원본 err 로 폴백한다.
    const blob = (err as AxiosError)?.response?.data;
    if (blob instanceof Blob) {
      const text = await blob.text().catch(() => '');
      let parsedMessage: string | undefined;
      if (text) {
        try {
          parsedMessage = (JSON.parse(text) as { message?: string }).message;
        } catch {
          // JSON 이 아니면 무시하고 원본 err 로 폴백
        }
      }
      if (parsedMessage) throw new Error(parsedMessage);
    }
    throw err;
  }
}
