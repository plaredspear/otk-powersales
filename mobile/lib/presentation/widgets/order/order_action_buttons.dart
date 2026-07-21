import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';

class OrderActionButtons extends StatelessWidget {
  final OrderDetail orderDetail;
  final bool showCancelButton;
  final VoidCallback? onCancel;

  const OrderActionButtons({
    super.key,
    required this.orderDetail,
    required this.showCancelButton,
    this.onCancel,
  });

  @override
  Widget build(BuildContext context) {
    if (!showCancelButton) {
      return const SizedBox.shrink();
    }

    return Padding(
      padding: EdgeInsets.symmetric(horizontal: AppSpacing.lg),
      child: Row(
        children: [
          Expanded(
            child: OutlinedButton(
              onPressed: onCancel,
              style: OutlinedButton.styleFrom(
                foregroundColor: AppColors.error,
                side: BorderSide(color: AppColors.error),
                shape: RoundedRectangleBorder(
                  borderRadius: AppSpacing.buttonBorderRadius,
                ),
                padding: EdgeInsets.symmetric(vertical: AppSpacing.md),
              ),
              child: Text(
                '주문 취소',
                style: AppTypography.labelLarge.copyWith(
                  color: AppColors.error,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
