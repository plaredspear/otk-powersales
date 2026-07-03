import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import PasswordPolicyChecklist from './PasswordPolicyChecklist';

/** 규칙 라벨을 가진 li 요소의 아이콘 색상으로 valid 상태를 판정. */
function ruleColor(label: string): string {
  const li = screen.getByText(label).closest('li');
  return (li as HTMLElement).style.color;
}

const GREEN = 'rgb(82, 196, 26)';
const RED = 'rgb(255, 77, 79)';
const GREY = 'rgba(0, 0, 0, 0.45)';

describe('PasswordPolicyChecklist', () => {
  it('두 규칙(8자 이상 / 3종 조합)을 표시한다', () => {
    render(<PasswordPolicyChecklist password="" />);
    expect(screen.getByText('8자 이상')).toBeInTheDocument();
    expect(
      screen.getByText('영문 대/소문자·숫자·특수문자 중 3종 이상 조합'),
    ).toBeInTheDocument();
  });

  it('빈 입력 - 모두 회색(대기)', () => {
    render(<PasswordPolicyChecklist password="" />);
    expect(ruleColor('8자 이상')).toBe(GREY);
    expect(ruleColor('영문 대/소문자·숫자·특수문자 중 3종 이상 조합')).toBe(GREY);
  });

  it('모두 충족 (Abcd123!) - 모두 녹색', () => {
    render(<PasswordPolicyChecklist password="Abcd123!" />);
    expect(ruleColor('8자 이상')).toBe(GREEN);
    expect(ruleColor('영문 대/소문자·숫자·특수문자 중 3종 이상 조합')).toBe(GREEN);
  });

  it('길이 미달 + 종류 충족 (Abc12!x, 7자 3종) - 길이 빨강 / 종류 녹색', () => {
    render(<PasswordPolicyChecklist password="Abc12!x" />);
    expect(ruleColor('8자 이상')).toBe(RED);
    expect(ruleColor('영문 대/소문자·숫자·특수문자 중 3종 이상 조합')).toBe(GREEN);
  });

  it('길이 충족 + 종류 미달 (abcd1234, 8자 2종) - 길이 녹색 / 종류 빨강', () => {
    render(<PasswordPolicyChecklist password="abcd1234" />);
    expect(ruleColor('8자 이상')).toBe(GREEN);
    expect(ruleColor('영문 대/소문자·숫자·특수문자 중 3종 이상 조합')).toBe(RED);
  });
});
