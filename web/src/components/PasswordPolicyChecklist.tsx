import { CheckCircleFilled, CloseCircleFilled, MinusCircleOutlined } from '@ant-design/icons';
import { getPasswordPolicyRules } from '@/lib/passwordPolicy';

interface PasswordPolicyChecklistProps {
  /** 검증 대상 비밀번호. */
  password: string;
}

/**
 * 비밀번호 정책 실시간 체크리스트 (mobile PasswordPolicyChecklist 위젯의 web 대응).
 *
 * 각 규칙(8자 이상 / 3종 이상 조합)을 입력에 따라 실시간으로 충족 여부 표시한다.
 * - 빈 입력: 회색 (대기)
 * - 충족: 녹색 ✓
 * - 미충족: 빨강 ✗
 */
export default function PasswordPolicyChecklist({ password }: PasswordPolicyChecklistProps) {
  const showStatus = password.length > 0;
  const rules = getPasswordPolicyRules(password);

  return (
    <ul style={{ listStyle: 'none', padding: 0, margin: '4px 0 0' }}>
      {rules.map((rule) => {
        let color = 'rgba(0, 0, 0, 0.45)';
        let icon = <MinusCircleOutlined />;
        if (showStatus && rule.isValid) {
          color = '#52c41a';
          icon = <CheckCircleFilled />;
        } else if (showStatus) {
          color = '#ff4d4f';
          icon = <CloseCircleFilled />;
        }
        return (
          <li
            key={rule.label}
            style={{ display: 'flex', alignItems: 'center', gap: 6, color, fontSize: 12, lineHeight: '20px' }}
          >
            {icon}
            <span>{rule.label}</span>
          </li>
        );
      })}
    </ul>
  );
}
