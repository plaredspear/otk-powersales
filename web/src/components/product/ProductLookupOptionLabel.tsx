import { Tag } from 'antd';
import { PRODUCT_STATUS_TAG } from './productStatus';

interface Props {
  name?: string | null;
  productCode?: string | null;
  productStatus?: string | null;
}

/**
 * 제품 lookup 드롭다운 옵션 라벨 — 좌측 `제품명 (제품코드)` / 우측 제품상태 Tag.
 *
 * 상태 표기는 기준정보 제품 목록/상세와 동일한 색상 Tag 를 쓴다 (판매중=green / 단종=red).
 * 백엔드가 화면 표시명을 내려주므로 값을 그대로 표시한다.
 *
 * 제품명이 길면 이름만 말줄임하고 Tag 는 밀려나지 않는다 — 이름 영역에 `flex: 1` + `minWidth: 0`
 * (flex item 기본 min-width:auto 를 풀어야 ellipsis 가 걸린다), Tag 에 `flexShrink: 0`.
 */
export default function ProductLookupOptionLabel({ name, productCode, productStatus }: Props) {
  return (
    <span style={{ display: 'flex', alignItems: 'center', gap: 6, width: '100%' }}>
      <span
        style={{
          flex: 1,
          minWidth: 0,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}
        title={`${name ?? ''} (${productCode ?? ''})`}
      >
        {name ?? ''} ({productCode ?? ''})
      </span>
      {productStatus && (
        <Tag
          color={PRODUCT_STATUS_TAG[productStatus] ?? undefined}
          style={{ marginInlineEnd: 0, flexShrink: 0 }}
        >
          {productStatus}
        </Tag>
      )}
    </span>
  );
}
