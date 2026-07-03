import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/password_validation.dart';

/// 비밀번호 정책 실시간 체크리스트.
///
/// 입력된 비밀번호가 백엔드 정책 규칙을 충족하는지 실시간으로 표시한다.
/// - 8자 이상
/// - 영문 대문자/소문자/숫자/특수문자 중 3종 이상 조합
///
/// 각 규칙은 충족 시 ✓ (녹색), 미충족 시 ✗ (빨강) 아이콘으로 표시한다. 빈 입력 상태에서는
/// 모두 회색 (대기 상태) 으로 표시한다.
class PasswordPolicyChecklist extends StatelessWidget {
  /// 검증 대상 비밀번호 (controller.text 등).
  final String password;

  const PasswordPolicyChecklist({
    super.key,
    required this.password,
  });

  @override
  Widget build(BuildContext context) {
    final showStatus = password.isNotEmpty;
    final validation = PasswordValidation.fromPassword(password);

    return Padding(
      padding: const EdgeInsets.only(left: AppSpacing.xs),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _PolicyRule(
            label: '8자 이상',
            isValid: validation.isLengthValid,
            showStatus: showStatus,
          ),
          const SizedBox(height: AppSpacing.xs),
          _PolicyRule(
            label: '영문 대/소문자·숫자·특수문자 중 3종 이상 조합',
            isValid: validation.hasEnoughCharacterTypes,
            showStatus: showStatus,
          ),
        ],
      ),
    );
  }
}

class _PolicyRule extends StatelessWidget {
  final String label;
  final bool isValid;
  final bool showStatus;

  const _PolicyRule({
    required this.label,
    required this.isValid,
    required this.showStatus,
  });

  @override
  Widget build(BuildContext context) {
    final Color color;
    final IconData icon;

    if (!showStatus) {
      color = AppColors.textTertiary;
      icon = Icons.circle_outlined;
    } else if (isValid) {
      color = AppColors.success;
      icon = Icons.check_circle;
    } else {
      color = AppColors.error;
      icon = Icons.cancel;
    }

    return Row(
      children: [
        Icon(icon, size: 16, color: color),
        const SizedBox(width: AppSpacing.xs),
        Text(
          label,
          style: AppTypography.bodySmall.copyWith(color: color),
        ),
      ],
    );
  }
}
