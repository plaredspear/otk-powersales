import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 여신 잔액 표시
class CreditBalanceDisplay extends StatelessWidget {
  final int? creditBalance;
  final bool isLoading;

  const CreditBalanceDisplay({
    super.key,
    required this.creditBalance,
    required this.isLoading,
  });

  String _formatNumber(int value) {
    return value.toString().replaceAllMapped(
      RegExp(r'(\d)(?=(\d{3})+(?!\d))'),
      (m) => '${m[1]},',
    );
  }

  @override
  Widget build(BuildContext context) {
    if (!isLoading && creditBalance == null) {
      return const SizedBox.shrink();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '여신 잔액',
          style: AppTypography.bodySmall,
        ),
        const SizedBox(height: AppSpacing.xs),
        if (isLoading)
          Text(
            '조회 중...',
            style: AppTypography.headlineSmall.copyWith(
              color: AppColors.textSecondary,
            ),
          )
        else if (creditBalance != null)
          Text(
            '${_formatNumber(creditBalance!)}원',
            style: AppTypography.headlineSmall.copyWith(
              color: AppColors.secondary,
            ),
          ),
      ],
    );
  }
}
