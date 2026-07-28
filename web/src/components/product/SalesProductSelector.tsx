import { Button, Select } from 'antd';
import SalesProductAdvancedSearchModal from '@/components/product/SalesProductAdvancedSearchModal';
import type { SalesProductSelectorState } from '@/hooks/sales/useSalesProductSelector';

interface Props {
  /** [useSalesProductSelector] 반환값 그대로. */
  selector: SalesProductSelectorState;
  /**
   * 드롭다운 폭(px). 화면마다 조회 조건 영역 폭이 달라 고정값을 받는다.
   *
   * 가변 폭(minWidth~maxWidth) + `maxTagCount="responsive"` 조합은 임계 폭 근처에서
   * 태그 펼침(폭 확장)↔접힘(폭 축소)이 무한 반복되며 화면이 깜빡이므로 고정값만 허용한다.
   */
  width: number;
}

/**
 * POS 매출 / 월 매출(전산실적) 조회 조건의 제품 선택 위젯 — 드롭다운 + 「고급 검색」 버튼 + 모달.
 *
 * 드롭다운은 상위 50건만 반환해 결과가 많은 키워드에서 뒤쪽 제품에 도달할 수 없다. 고급 검색은
 * 분류/상태 필터 + 페이징으로 전체 결과를 열람하게 하고, 선택 결과를 드롭다운 선택에 반영한다.
 * 두 진입점의 상태는 [useSalesProductSelector] 한곳에서 관리한다.
 */
export default function SalesProductSelector({ selector, width }: Props) {
  const { dropdown, advanced, selectedProducts } = selector;

  return (
    <>
      <div style={{ display: 'flex', gap: 8 }}>
        <Select
          mode="multiple"
          value={dropdown.value}
          onChange={dropdown.onChange}
          onSearch={dropdown.onSearch}
          options={dropdown.options}
          placeholder="검색 후 추가 (미선택 시 전체)"
          style={{ width, flex: '0 0 auto' }}
          maxTagCount="responsive"
          allowClear
          showSearch
          filterOption={false}
          loading={dropdown.loading}
          notFoundContent={
            dropdown.keyword ? '검색 결과 없음' : '제품명, 제품코드 또는 바코드를 입력하세요'
          }
        />
        <Button onClick={() => advanced.setOpen(true)}>고급 검색</Button>
      </div>

      <SalesProductAdvancedSearchModal
        open={advanced.open}
        onClose={() => advanced.setOpen(false)}
        onSelect={advanced.onSelect}
        initialKeyword={dropdown.keyword}
        initialSelectedIds={selectedProducts.map((p) => p.productId)}
      />
    </>
  );
}
