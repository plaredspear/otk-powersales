import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 여신 잔액 표시
class CreditBalanceDisplay extends StatelessWidget {
  final int? creditBalance;
  final bool isLoading;

  /// 여신조회 실패 여부 — true 면 실패 안내 + "다시 조회" 버튼을 노출한다.
  final bool isFailed;

  /// "다시 조회" 버튼 콜백 (실패 시에만 사용). null 이면 버튼 미노출.
  final VoidCallback? onRetry;

  const CreditBalanceDisplay({
    super.key,
    required this.creditBalance,
    required this.isLoading,
    this.isFailed = false,
    this.onRetry,
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
          _buildValue(),
        ],
      ),
    );
  }

  Widget _buildValue() {
    if (isLoading) {
      return Text(
        '조회 중...',
        style: AppTypography.headlineSmall.copyWith(
          color: AppColors.textSecondary,
        ),
      );
    }
    // 실패 — 조회중과 구분해 안내 + 재조회 버튼. creditBalance 가 null 이라도 "미선택" 으로
    // 잘못 표시하지 않는다.
    if (isFailed) {
      return Row(
        children: [
          Expanded(
            child: Text(
              '여신 조회 실패',
              style: AppTypography.bodyMedium.copyWith(color: AppColors.error),
            ),
          ),
          if (onRetry != null)
            TextButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh, size: 18),
              label: const Text('다시 조회'),
              style: TextButton.styleFrom(
                foregroundColor: AppColors.primary,
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.sm,
                ),
                minimumSize: Size.zero,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
            ),
        ],
      );
    }
    if (creditBalance != null) {
      return Text(
        '${_formatNumber(creditBalance!)}원',
        style: AppTypography.headlineSmall.copyWith(
          color: AppColors.secondary,
        ),
      );
    }
    return Text(
      '거래처를 선택하세요',
      style: AppTypography.bodyMedium.copyWith(
        color: AppColors.textTertiary,
      ),
    );
  }
}
