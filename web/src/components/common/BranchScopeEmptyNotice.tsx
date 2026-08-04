import type { CSSProperties } from 'react';
import { Tag, Tooltip } from 'antd';
import { WarningOutlined } from '@ant-design/icons';

const DETAIL =
  '로그인 계정에 조회 가능한 지점이 없습니다. 조직 정보가 갱신 중이거나 소속 조직이 등록되지 않은 경우 발생합니다. 계속되면 관리자에게 문의해주세요.';

interface BranchScopeEmptyNoticeProps {
  style?: CSSProperties;
}

/**
 * 지점 셀렉터 옵션이 0건일 때의 명시 안내.
 *
 * 지점 셀렉터가 비면 backend 가 조회를 `NoAccess`(매칭 0건)로 막기 때문에 목록이 통째로 빈다.
 * 그런데 화면은 옵션이 없으면 Select 도 Tag 도 렌더하지 않아, **필터바에서 지점 UI 자체가 사라진 채
 * "조회 결과가 없습니다"만** 뜬다 — 데이터가 없는 것인지 권한/스코프 문제인지 구분할 단서가 없다.
 *
 * 2026-08-03 운영 마이그레이션 때 조직 캐시가 stale 되어 지점 사용자 전원이 이 상태였는데, 화면에
 * 아무 신호가 없어 원인 파악이 오래 걸렸다. 그 자리에 이 배지를 대신 세워 원인을 드러낸다.
 */
export default function BranchScopeEmptyNotice({ style }: BranchScopeEmptyNoticeProps) {
  return (
    <Tooltip title={DETAIL}>
      <Tag
        color="warning"
        icon={<WarningOutlined />}
        style={{ fontSize: 13, padding: '4px 10px', marginInlineEnd: 0, cursor: 'help', ...style }}
      >
        조회 가능한 지점 없음
      </Tag>
    </Tooltip>
  );
}
