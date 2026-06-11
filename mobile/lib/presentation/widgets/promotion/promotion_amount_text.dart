import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';

/// 행사 금액 포맷 위젯 (만원 단위 변환)
class PromotionAmountText extends StatelessWidget {
  final int? amount;
  final TextStyle? style;

  const PromotionAmountText({super.key, this.amount, this.style});

  @override
  Widget build(BuildContext context) {
    return Text(
      formatAmount(amount),
      style: style ??
          AppTypography.bodySmall.copyWith(
            color: AppColors.textPrimary,
            fontWeight: FontWeight.w600,
          ),
    );
  }

  /// 금액을 원 단위로 포맷 (레거시 행사 상세 동일: 천단위 콤마 + "원")
  ///
  /// 예: 3000000 → "3,000,000원"
  /// null: "-"
  static String formatAmount(int? amount) {
    if (amount == null) return '-';
    return '${_numberWithCommas(amount)}원';
  }

  static String _numberWithCommas(int value) {
    return value.toString().replaceAllMapped(
          RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
          (match) => '${match[1]},',
        );
  }
}
