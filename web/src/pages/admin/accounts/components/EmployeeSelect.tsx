import { useEffect, useMemo, useState } from 'react';
import { Select, Spin } from 'antd';
import { fetchEmployees, type Employee } from '@/api/employee';

/**
 * 영업사원 검색 dropdown (Spec #640 P2-W).
 *
 * `GET /api/v1/admin/employees` 의 keyword 검색을 활용한다 (디바운스 300ms).
 * 표시 형식: `사번 — 이름 (지점명)`. 선택 시 부모에 employeeCode 와
 * 자동 채움 미리보기에 필요한 [Employee] 객체를 함께 전달한다.
 */
const SEARCH_DEBOUNCE_MS = 300;
const SEARCH_PAGE_SIZE = 20;

export interface EmployeeSelectProps {
  value?: string;
  onChange: (employeeCode: string | undefined, employee: Employee | undefined) => void;
  disabled?: boolean;
  placeholder?: string;
}

interface EmployeeOption {
  value: string;
  label: string;
  employee: Employee;
}

export default function EmployeeSelect({ value, onChange, disabled, placeholder }: EmployeeSelectProps) {
  const [keyword, setKeyword] = useState('');
  const [debouncedKeyword, setDebouncedKeyword] = useState('');
  const [options, setOptions] = useState<EmployeeOption[]>([]);
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
    fetchEmployees({ keyword: debouncedKeyword.trim(), page: 0, size: SEARCH_PAGE_SIZE })
      .then((data) => {
        if (cancelled) return;
        setOptions(
          data.content.map((emp) => ({
            value: emp.employeeCode,
            label: `${emp.employeeCode} — ${emp.name}${emp.orgName ? ` (${emp.orgName})` : ''}`,
            employee: emp,
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
    if (debouncedKeyword.trim().length === 0) return '사번 또는 이름을 입력하세요';
    return '검색 결과 없음';
  }, [loading, debouncedKeyword]);

  return (
    <Select
      showSearch
      allowClear
      placeholder={placeholder ?? '사번 또는 이름으로 검색'}
      value={value}
      disabled={disabled}
      filterOption={false}
      onSearch={setKeyword}
      onChange={(next: string | undefined) => {
        const matched = options.find((opt) => opt.value === next);
        onChange(next, matched?.employee);
      }}
      options={options}
      notFoundContent={notFoundContent}
      style={{ width: '100%' }}
    />
  );
}
