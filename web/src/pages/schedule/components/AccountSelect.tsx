import { useEffect, useMemo, useState } from 'react';
import { Select, Spin } from 'antd';
import { fetchAccounts, type Account } from '@/api/account';

const SEARCH_DEBOUNCE_MS = 300;
const SEARCH_PAGE_SIZE = 20;

export interface AccountSelectProps {
  value?: string;
  onChange: (accountCode: string | undefined, account: Account | undefined) => void;
  disabled?: boolean;
  placeholder?: string;
}

interface AccountOption {
  value: string;
  label: string;
  account: Account;
}

/**
 * 거래처 검색 dropdown (UC-02 단건 등록 — 진열사원스케줄 마스터 폼).
 *
 * `GET /api/v1/admin/accounts` 의 keyword 검색 (거래처명) 활용. 디바운스 300ms.
 * 표시 형식: `거래처코드 — 거래처명 (지점명)`. 선택 값은 externalKey (거래처코드).
 */
export default function AccountSelect({ value, onChange, disabled, placeholder }: AccountSelectProps) {
  const [keyword, setKeyword] = useState('');
  const [debouncedKeyword, setDebouncedKeyword] = useState('');
  const [options, setOptions] = useState<AccountOption[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const handle = setTimeout(() => setDebouncedKeyword(keyword), SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(handle);
  }, [keyword]);

  useEffect(() => {
    if (debouncedKeyword.trim().length === 0) {
      setOptions([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    fetchAccounts({ keyword: debouncedKeyword.trim(), page: 0, size: SEARCH_PAGE_SIZE })
      .then((data) => {
        if (cancelled) return;
        setOptions(
          data.content
            .filter((acc) => acc.externalKey != null)
            .map((acc) => ({
              value: acc.externalKey!,
              label: `${acc.externalKey} — ${acc.name ?? ''}${acc.branchName ? ` (${acc.branchName})` : ''}`,
              account: acc,
            })),
        );
      })
      .catch(() => {
        if (!cancelled) setOptions([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [debouncedKeyword]);

  const notFoundContent = useMemo(() => {
    if (loading) return <Spin size="small" />;
    if (debouncedKeyword.trim().length === 0) return '거래처명을 입력하세요';
    return '검색 결과 없음';
  }, [loading, debouncedKeyword]);

  return (
    <Select
      showSearch
      allowClear
      placeholder={placeholder ?? '거래처명으로 검색'}
      value={value}
      disabled={disabled}
      filterOption={false}
      onSearch={setKeyword}
      onChange={(next: string | undefined) => {
        const matched = options.find((opt) => opt.value === next);
        onChange(next, matched?.account);
      }}
      options={options}
      notFoundContent={notFoundContent}
      style={{ width: '100%' }}
    />
  );
}
