import { Select } from 'antd';

export interface PresetOption<T extends string> {
  value: T;
  label: string;
}

interface PresetFilterSelectProps<T extends string> {
  options: PresetOption<T>[];
  value?: T;
  onChange: (value: T | undefined) => void;
  placeholder?: string;
  style?: React.CSSProperties;
  allowClear?: boolean;
}

/**
 * 페이지 List View 프리셋 필터를 위한 드롭다운 공용 컴포넌트.
 * 레거시 SF List View 드롭다운과 동등한 UX 를 제공.
 * 다른 페이지(여사원일정관리, 행사스케줄마스터 등) 에서도 재사용 가능.
 */
export function PresetFilterSelect<T extends string>({
  options,
  value,
  onChange,
  placeholder = '뷰 선택',
  style,
  allowClear = true,
}: PresetFilterSelectProps<T>) {
  return (
    <Select<T>
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      allowClear={allowClear}
      style={{ minWidth: 200, ...style }}
      options={options}
    />
  );
}
