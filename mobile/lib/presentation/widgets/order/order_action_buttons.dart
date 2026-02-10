import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';

class OrderActionButtons extends StatelessWidget {
  final OrderDetail orderDetail;
  final bool showCancelButton;
  final bool showResendButton;
  final bool isResending;
  final VoidCallback? onCancel;
  final VoidCallback? onResend;

  const OrderActionButtons({
    super.key,
    required this.orderDetail,
    required this.showCancelButton,
    required this.showResendButton,
    required this.isResending,
    this.onCancel,
    this.onResend,
  });

  @override
  Widget build(BuildContext context) {
    if (!showCancelButton && !showResendButton) {
      return const SizedBox.shrink();
    }

    return Padding(
      padding: EdgeInsets.symmetric(horizontal: AppSpacing.lg),
      child: Row(
        children: [
          if (showCancelButton)
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
          if (showCancelButton && showResendButton)
            SizedBox(width: AppSpacing.md),
          if (showResendButton)
            Expanded(
              child: ElevatedButton(
                onPressed: isResending ? null : onResend,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: AppColors.onPrimary,
                  shape: RoundedRectangleBorder(
                    borderRadius: AppSpacing.buttonBorderRadius,
                  ),
                  padding: EdgeInsets.symmetric(vertical: AppSpacing.md),
                  // ignore: deprecated_member_use
                  disabledBackgroundColor: AppColors.primary.withOpacity(0.6),
                ),
                child: isResending
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          valueColor: AlwaysStoppedAnimation<Color>(
                            AppColors.onPrimary,
                          ),
                        ),
                      )
                    : Text(
                        '재전송',
                        style: AppTypography.labelLarge.copyWith(
                          color: AppColors.onPrimary,
                        ),
                      ),
              ),
            ),
        ],
      ),
    );
  }
}
