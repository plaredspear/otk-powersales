import { useEffect, useMemo, useState } from 'react';
import { Select, Spin } from 'antd';
import { fetchEmployeesForScheduleLookup, type Employee } from '@/api/employee';

/**
 * 진열사원 스케줄 마스터 화면의 사원 검색 dropdown.
 *
 * SF TeamMemberSchedule__c.EmployeeId__c Lookup 정합 — Employee READ 권한 없이도
 * team_member_schedule 권한만으로 검색 가능한 `/api/v1/admin/employees/lookup-for-schedule`
 * 호출 (디바운스 300ms). 표시 형식: `사번 — 이름 (지점명)`. 선택 시 부모에
 * employeeCode 와 [Employee] 객체를 함께 전달한다.
 */
const SEARCH_DEBOUNCE_MS = 300;
const SEARCH_PAGE_SIZE = 20;

export interface EmployeeSelectProps {
  value?: string;
  onChange: (employeeCode: string | undefined, employee: Employee | undefined) => void;
  disabled?: boolean;
  placeholder?: string;
  /**
   * 편집 모드 초기 표시 label. 검색 전이라 options 가 비어 value(사번)만 표시되는 문제를
   * 막기 위해, 현재 value 에 대응하는 표시 텍스트(예: `이름(사번)`)를 주입한다.
   */
  initialLabel?: string;
}

interface EmployeeOption {
  value: string;
  label: string;
  employee: Employee;
}

export default function EmployeeSelect({ value, onChange, disabled, placeholder, initialLabel }: EmployeeSelectProps) {
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
    fetchEmployeesForScheduleLookup({ keyword: debouncedKeyword.trim(), page: 0, size: SEARCH_PAGE_SIZE })
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

  // 검색 결과에 현재 value 가 없으면 초기 label 합성 옵션을 추가해 코드 대신 이름을 표시.
  const mergedOptions = useMemo(() => {
    if (value && initialLabel && !options.some((opt) => opt.value === value)) {
      return [{ value, label: initialLabel } as EmployeeOption, ...options];
    }
    return options;
  }, [value, initialLabel, options]);

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
      options={mergedOptions}
      notFoundContent={notFoundContent}
      style={{ width: '100%' }}
    />
  );
}
