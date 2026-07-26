/**
 * 제품상태(SF `DKRetail__Product__c.DKRetail__ProductStatus__c`) 화면 표기 공용 정의.
 *
 * 백엔드가 저장값이 아니라 **화면 표시명**을 내려준다 (backend `ProductStatus` enum 의
 * displayName=저장값 / label=표시명 분리).
 *
 * | 저장값 | 표시명 |
 * |---|---|
 * | (없음/null) | 판매중 |
 * | 출고중지 | 단종 |
 *
 * 매핑 안내 tooltip 은 [ProductStatusInfoIcon] 컴포넌트가 담당한다.
 */

/** 표시명 → Tag 색상. 백엔드가 표시명만 내려주므로 저장값('출고중지')은 키가 아니다. */
export const PRODUCT_STATUS_TAG: Record<string, string> = {
  판매중: 'green',
  단종: 'red',
};
