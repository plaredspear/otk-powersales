import { Button, Tooltip } from 'antd';
import type { ButtonProps } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';

interface RefreshButtonProps extends Omit<ButtonProps, 'onClick' | 'loading' | 'icon'> {
  /** useQuery 가 반환하는 refetch. 버튼 클릭 시 호출되어 해당 쿼리만 다시 fetch 한다. */
  onRefresh: () => void;
  /** useQuery 의 isFetching. 진행 중이면 버튼에 스피너를 띄우고 중복 클릭을 막는다. */
  refreshing?: boolean;
  /** hover tooltip. 기본 "새로고침". */
  tooltip?: string;
}

/**
 * 테이블 등 API 데이터 영역에 다는 새로고침 버튼.
 *
 * TanStack Query 의 refetch 를 위임받아 호출하는 단순 래퍼다. 페이지는 데이터 훅이 반환하는
 * `refetch` 와 `isFetching` 을 그대로 넘기기만 하면 된다 — 누르면 그 쿼리 하나만 다시 fetch 하고,
 * fetch 중에는 아이콘이 스피너로 바뀌며 중복 클릭이 막힌다.
 *
 * 사용 예:
 *   const { data, isLoading, refetch, isFetching } = useNotices(...);
 *   <RefreshButton onRefresh={refetch} refreshing={isFetching} />
 */
export default function RefreshButton({
  onRefresh,
  refreshing = false,
  tooltip = '새로고침',
  ...rest
}: RefreshButtonProps) {
  return (
    <Tooltip title={tooltip}>
      <Button icon={<ReloadOutlined />} loading={refreshing} onClick={() => onRefresh()} {...rest} />
    </Tooltip>
  );
}
