import { Empty, Spin, Result, Button } from 'antd';
import type { ReactNode } from 'react';

/** 목록/상세 공통 로딩 상태. */
export function LoadingState({ tip = '불러오는 중...' }: { tip?: string }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: '48px 0' }}>
      <Spin tip={tip}>
        <div style={{ width: 1, height: 1 }} />
      </Spin>
    </div>
  );
}

/** 빈 결과 상태. */
export function EmptyState({ description = '데이터가 없습니다' }: { description?: ReactNode }) {
  return (
    <div style={{ padding: '48px 0' }}>
      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={description} />
    </div>
  );
}

/** 에러 상태 + 재시도. */
export function ErrorState({
  message = '데이터를 불러오지 못했습니다',
  onRetry,
}: {
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <Result
      status="warning"
      subTitle={message}
      extra={
        onRetry ? (
          <Button type="primary" onClick={onRetry}>
            다시 시도
          </Button>
        ) : undefined
      }
    />
  );
}

/**
 * query 결과를 상태별로 렌더하는 래퍼.
 * 로딩/에러/빈 결과를 일관 처리하고, 성공 시 children(data) 를 호출한다.
 */
export function QueryBoundary<T>({
  isLoading,
  isError,
  data,
  onRetry,
  isEmpty,
  emptyDescription,
  children,
}: {
  isLoading: boolean;
  isError: boolean;
  data: T | undefined;
  onRetry?: () => void;
  isEmpty?: (data: T) => boolean;
  emptyDescription?: ReactNode;
  children: (data: T) => ReactNode;
}) {
  if (isLoading) return <LoadingState />;
  if (isError || data === undefined) return <ErrorState onRetry={onRetry} />;
  if (isEmpty?.(data)) return <EmptyState description={emptyDescription} />;
  return <>{children(data)}</>;
}
