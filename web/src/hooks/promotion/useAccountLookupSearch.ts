import { useCallback } from 'react';
import { fetchAccountsForPromotionLookup, type Account } from '@/api/account';
import { useLookupSearch, type LookupSearchResult } from '@/hooks/common/useLookupSearch';

/**
 * 드롭다운 옵션 표시 + 선택 후 거래상태 표기에 필요한 최소 필드.
 *
 * `accountStatusName` 은 선택 즉시 "거래상태" 를 보여주기 위해 함께 보관한다
 * (폐업/출고중지 거래처에 행사를 등록하려는 상황을 사용자가 알아채도록).
 */
export interface AccountLookupOption {
  id: number;
  name: string | null;
  externalKey: string | null;
  accountStatusName: string | null;
}

/**
 * 행사마스터 거래처 lookup 검색.
 *
 * 조회/매핑만 지정하고 debounce·요청 순번·키워드 보관은 [useLookupSearch] 에 위임한다
 * (대표제품 lookup 과 동일 구조).
 */
export function useAccountLookupSearch(
  { size = 20 }: { size?: number } = {},
): LookupSearchResult<AccountLookupOption> {
  const toItem = useCallback(
    (a: Account): AccountLookupOption | null =>
      a.id != null && a.name != null
        ? {
            id: a.id,
            name: a.name,
            externalKey: a.externalKey,
            accountStatusName: a.accountStatusName,
          }
        : null,
    [],
  );

  return useLookupSearch({ fetchPage: fetchAccountsForPromotionLookup, toItem, size });
}
