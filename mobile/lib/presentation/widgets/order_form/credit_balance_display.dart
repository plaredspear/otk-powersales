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
    // 레거시(write.jsp #loanInquiry): 거래처 선택 전에도 항상 노출되며
    // 미선택 시 "거래처를 선택하세요" 안내를 표시한다.
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.md,
      ),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '여신 잔액',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
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
            )
          else
            Text(
              '거래처를 선택하세요',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
        ],
      ),
    );
  }
}
