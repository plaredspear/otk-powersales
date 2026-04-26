import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 총 주문금액 표시
class TotalAmountDisplay extends StatelessWidget {
  final int totalAmount;

  const TotalAmountDisplay({
    super.key,
    required this.totalAmount,
  });

  String _formatNumber(int value) {
    return value.toString().replaceAllMapped(
      RegExp(r'(\d)(?=(\d{3})+(?!\d))'),
      (m) => '${m[1]},',
    );
  }

  @override
  Widget build(BuildContext context) {
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
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            '총 주문금액',
            style: AppTypography.headlineSmall,
          ),
          Text(
            '${_formatNumber(totalAmount)}원',
            style: AppTypography.headlineLarge.copyWith(
              color: AppColors.secondary,
            ),
          ),
        ],
      ),
    );
  }
}
