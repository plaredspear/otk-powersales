import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 주문 재전송 확인 다이얼로그
///
/// 전송실패 주문 재전송 전 사용자 확인을 요청합니다 (레거시 `confirm('재전송 하시겠습니까?')` 정합).
/// "취소" / "재전송" 버튼을 제공합니다.
class ResendConfirmDialog extends StatelessWidget {
  const ResendConfirmDialog({super.key});

  /// 다이얼로그 표시 헬퍼
  ///
  /// Returns: true이면 재전송 확정, false/null이면 취소
  static Future<bool?> show(BuildContext context) {
    return showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (context) => const ResendConfirmDialog(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      ),
      title: Text(
        '주문 재전송',
        style: AppTypography.headlineSmall.copyWith(
          fontWeight: FontWeight.bold,
        ),
      ),
      content: Text(
        '재전송 하시겠습니까?',
        style: AppTypography.bodyMedium.copyWith(
          color: AppColors.textSecondary,
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: Text(
            '취소',
            style: AppTypography.labelLarge.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ),
        TextButton(
          onPressed: () => Navigator.of(context).pop(true),
          child: Text(
            '재전송',
            style: AppTypography.labelLarge.copyWith(
              color: AppColors.error,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
      ],
    );
  }
}
