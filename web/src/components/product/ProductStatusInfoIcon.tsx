import { Tooltip } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';

/**
 * 제품상태 컬럼/필드 헤더에 붙이는 info 아이콘 — UI 표기 ↔ 원본 값 매핑 안내.
 *
 * 백엔드가 저장값이 아니라 **화면 표시명**을 내려주므로(backend `ProductStatus` enum 의
 * displayName=저장값 / label=표시명 분리), 사용자가 원본 값과의 관계를 알 수 있게 한다.
 *
 * | 저장값 | 표시명 |
 * |---|---|
 * | (없음/null) | 판매중 |
 * | 출고중지 | 단종 |
 */
export default function ProductStatusInfoIcon() {
  return (
    <Tooltip
      title={
        <span>
          화면 표기는 원본 제품상태 값을 변환한 것입니다.
          <br />· 판매중 ← 값 없음 (미설정)
          <br />· 단종 ← 출고중지
        </span>
      }
    >
      <InfoCircleOutlined style={{ color: '#8c8c8c', cursor: 'help' }} />
    </Tooltip>
  );
}
