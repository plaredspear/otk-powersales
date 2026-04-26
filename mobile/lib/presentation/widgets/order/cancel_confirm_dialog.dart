import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 주문 취소 확인 다이얼로그
///
/// 선택된 제품 수를 표시하고, 사용자에게 취소 확인을 요청합니다.
/// "취소" / "주문 취소" 버튼을 제공합니다.
class CancelConfirmDialog extends StatelessWidget {
  /// 선택된 제품 수
  final int selectedCount;

  const CancelConfirmDialog({
    super.key,
    required this.selectedCount,
  });

  /// 다이얼로그 표시 헬퍼
  ///
  /// Returns: true이면 취소 확정, false/null이면 취소 취소
  static Future<bool?> show(
    BuildContext context, {
    required int selectedCount,
  }) {
    return showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (context) => CancelConfirmDialog(
        selectedCount: selectedCount,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      ),
      title: Text(
        '주문 취소',
        style: AppTypography.headlineSmall.copyWith(
          fontWeight: FontWeight.bold,
        ),
      ),
      content: Text(
        '선택한 $selectedCount개 제품의 주문을 취소하시겠습니까?',
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
            '주문 취소',
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
