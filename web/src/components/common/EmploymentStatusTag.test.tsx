import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import EmploymentStatusTag from './EmploymentStatusTag';
import { getEmploymentStatusColor } from '@/constants/employmentStatus';

describe('EmploymentStatusTag', () => {
  it('재직상태를 Tag 로 표시한다', () => {
    render(<EmploymentStatusTag status="재직" />);
    expect(screen.getByText('재직')).toBeInTheDocument();
  });

  it('값이 없으면 기본 대체 텍스트를 표시한다', () => {
    const { container } = render(<EmploymentStatusTag status={null} />);
    expect(container.textContent).toBe('-');
  });

  it('fallback 을 지정하면 그 값을 표시한다', () => {
    const { container } = render(<EmploymentStatusTag status={undefined} fallback="미상" />);
    expect(container.textContent).toBe('미상');
  });

  it("variant='plain' 은 Tag 없이 텍스트만 표시한다", () => {
    const { container } = render(<EmploymentStatusTag status="재직" variant="plain" />);
    expect(container.textContent).toBe('재직');
    expect(container.querySelector('.ant-tag')).toBeNull();
  });

  it("variant='tag' 는 Tag 엘리먼트를 렌더한다", () => {
    const { container } = render(<EmploymentStatusTag status="재직" variant="tag" />);
    expect(container.querySelector('.ant-tag')).not.toBeNull();
  });
});

describe('getEmploymentStatusColor', () => {
  // 통합 전에는 색상 맵이 3개 페이지에 복제되어 있었고 일부 사본에 아래 키가 누락되어
  // 무채색으로 표시되는 드리프트가 있었다. 값 도메인 3종의 합집합을 회귀 감시한다.
  it.each([
    ['재직', 'green'],
    ['휴직', 'orange'],
    ['퇴직', 'red'],
    ['퇴직(면직)', 'red'],
    ['퇴사', 'red'],
    ['퇴직예정', 'volcano'],
  ])('%s → %s', (status, expected) => {
    expect(getEmploymentStatusColor(status)).toBe(expected);
  });

  it('도메인 외 값은 default 로 폴백한다', () => {
    expect(getEmploymentStatusColor('알수없음')).toBe('default');
  });

  it('null / undefined 는 default 로 폴백한다', () => {
    expect(getEmploymentStatusColor(null)).toBe('default');
    expect(getEmploymentStatusColor(undefined)).toBe('default');
  });
});
