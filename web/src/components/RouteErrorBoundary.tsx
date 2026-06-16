import { useEffect, useState } from 'react';
import { useRouteError } from 'react-router-dom';
import { Button, Result } from 'antd';
import PageSpinner from '@/components/common/PageSpinner';

const CHUNK_RELOAD_AT_KEY = 'otk:chunk-reload-at';
// 자동 reload 직후 같은 청크 에러가 재발하면(배포 미완료 등) 무한 reload 가 되므로,
// 직전 자동 reload 로부터 이 시간 이내라면 자동 reload 를 스킵하고 안내 화면을 보여준다.
// 이 시간이 지난 뒤의 청크 에러는 "또 다른 새 배포" 로 보고 자동 reload 를 다시 허용한다(자동 재무장).
const RELOAD_COOLDOWN_MS = 10_000;

/**
 * lazy import 한 페이지 청크가 사라졌을 때(새 빌드 배포 후 구버전 페이지에서 이동 시)
 * 던져지는 에러인지 판별한다. Vite 는 청크 로드 실패 시
 * "Failed to fetch dynamically imported module" / "Importing a module script failed"
 * 류의 메시지를 던진다.
 */
function isChunkLoadError(error: unknown): boolean {
  if (!error) return false;
  const message =
    error instanceof Error
      ? error.message
      : typeof error === 'string'
        ? error
        : String((error as { message?: unknown })?.message ?? '');
  return /Failed to fetch dynamically imported module|error loading dynamically imported module|Importing a module script failed|dynamically imported module/i.test(
    message,
  );
}

/** 직전 자동 reload 로부터 cooldown 이내인지. sessionStorage 의 timestamp 로 판단. */
function recentlyReloaded(): boolean {
  const at = Number(sessionStorage.getItem(CHUNK_RELOAD_AT_KEY) ?? 0);
  return at > 0 && Date.now() - at < RELOAD_COOLDOWN_MS;
}

export default function RouteErrorBoundary() {
  const error = useRouteError();
  const chunkError = isChunkLoadError(error);

  // 청크 에러이고, 직전에 자동 reload 한 적이 없거나 cooldown 이 지났다면 자동 1회 reload.
  // useState 초기화 함수로 마운트 시점에 1회만 평가하여 reload 여부를 고정한다.
  const [autoReloading] = useState(() => chunkError && !recentlyReloaded());

  useEffect(() => {
    if (!autoReloading) return;
    sessionStorage.setItem(CHUNK_RELOAD_AT_KEY, String(Date.now()));
    window.location.reload();
  }, [autoReloading]);

  // 자동 reload 가 트리거된 직후에는 빈 화면 대신 로딩 스피너를 보여준다.
  if (autoReloading) {
    return <PageSpinner />;
  }

  return (
    <Result
      status="warning"
      title={chunkError ? '최신 버전이 배포되었습니다' : '일시적인 오류가 발생했습니다'}
      subTitle={
        chunkError
          ? '화면을 최신 버전으로 갱신하지 못했습니다. 아래 버튼으로 새로고침 해주세요.'
          : '잠시 후 다시 시도해 주세요. 문제가 계속되면 새로고침 하거나 관리자에게 문의하세요.'
      }
      extra={
        <Button
          type="primary"
          onClick={() => {
            // 수동 새로고침은 cooldown 과 무관하게 항상 동작해야 하므로 가드를 비운다.
            sessionStorage.removeItem(CHUNK_RELOAD_AT_KEY);
            window.location.reload();
          }}
        >
          새로고침
        </Button>
      }
    />
  );
}
