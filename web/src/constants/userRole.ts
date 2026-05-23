/**
 * SF DKRetail__Employee__c.DKRetail__AppAuthority__c picklist value 정의.
 *
 * spec #807: backend `UserRoleEnum` 폐기 후 Employee.role 은 SF picklist value (한글) String 그대로.
 * `restrictedPicklist=false` 이지만 운영 실측 picklist 4종만 사용.
 * picklist value 자체가 한글 label 이라 별도 label 매핑 불필요 (roleLabel 폐기).
 */
export type AppAuthority = '여사원' | '조장' | '지점장' | 'AccountViewAll';

export interface AppAuthorityOption {
  value: AppAuthority;
  label: string;
}

/** Employee 등록 / 검색 dropdown 옵션 — SF picklist 4종. */
export const APP_AUTHORITY_OPTIONS: AppAuthorityOption[] = [
  { value: '여사원', label: '여사원' },
  { value: '조장', label: '조장' },
  { value: '지점장', label: '지점장' },
  { value: 'AccountViewAll', label: 'AccountViewAll' },
];
