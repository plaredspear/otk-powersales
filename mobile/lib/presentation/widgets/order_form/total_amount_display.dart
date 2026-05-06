import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 총 주문금액 표시 (Spec #598 P3-M §2.5 — 한도 초과 inline 경고).
class TotalAmountDisplay extends StatelessWidget {
  final int totalAmount;

  /// 여신 잔액 (Spec #598 P3-M) — null 이면 한도 검증 비활성.
  final int? creditBalance;

  const TotalAmountDisplay({
    super.key,
    required this.totalAmount,
    this.creditBalance,
  });

  bool get _isExceeded =>
      creditBalance != null && totalAmount > creditBalance!;

  String _formatNumber(int value) {
    return value.toString().replaceAllMapped(
      RegExp(r'(\d)(?=(\d{3})+(?!\d))'),
      (m) => '${m[1]},',
    );
  }

  @override
  Widget build(BuildContext context) {
    final amountColor =
        _isExceeded ? AppColors.error : AppColors.secondary;

    return Container(
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(
            color: AppColors.divider,
            width: 1,
          ),
        ),
      ),
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('총 주문금액', style: AppTypography.headlineSmall),
              Text(
                '${_formatNumber(totalAmount)}원',
                style:
                    AppTypography.headlineLarge.copyWith(color: amountColor),
              ),
            ],
          ),
          if (_isExceeded) ...[
            const SizedBox(height: AppSpacing.xs),
            Align(
              alignment: Alignment.centerRight,
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.sm,
                  vertical: 2,
                ),
                decoration: BoxDecoration(
                  color: AppColors.error,
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(
                  '여신 한도 초과',
                  style: AppTypography.labelSmall.copyWith(
                    color: Colors.white,
                  ),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
