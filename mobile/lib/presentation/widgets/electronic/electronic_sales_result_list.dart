import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/electronic_sales.dart';

/// 전산매출 조회 결과 카드 리스트.
///
/// 레거시 `abcmain.jsp` 의 `#barcode-result-list` 카드 정합 — 제품 1건당
/// 제품명 / 제품코드 / 바코드 / 납품 수량 / 금액 을 표시한다.
class ElectronicSalesResultList extends StatelessWidget {
  final List<ElectronicSales> salesList;

  const ElectronicSalesResultList({super.key, required this.salesList});

  static final _numberFormat = NumberFormat('#,###');

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      padding: const EdgeInsets.all(AppSpacing.lg),
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: salesList.length,
      separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.md),
      itemBuilder: (context, index) => _SalesCard(sales: salesList[index]),
    );
  }
}

class _SalesCard extends StatelessWidget {
  final ElectronicSales sales;

  const _SalesCard({required this.sales});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(color: AppColors.divider),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            sales.productName,
            style: AppTypography.bodyLarge
                .copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: AppSpacing.sm),
          _row('제품코드', sales.productCode),
          _row('바코드', sales.barcode),
          _row('납품 수량', '${ElectronicSalesResultList._numberFormat.format(sales.quantity)}개'),
          _row(
            '금액',
            '${ElectronicSalesResultList._numberFormat.format(sales.amount)}원',
            valueStyle: AppTypography.bodyMedium.copyWith(
              fontWeight: FontWeight.w700,
              color: AppColors.legacyDanger,
            ),
          ),
        ],
      ),
    );
  }

  Widget _row(String label, String value, {TextStyle? valueStyle}) {
    return Padding(
      padding: const EdgeInsets.only(top: AppSpacing.xs),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 72,
            child: Text(
              label,
              style: AppTypography.bodySmall
                  .copyWith(color: AppColors.textSecondary),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: valueStyle ?? AppTypography.bodyMedium,
            ),
          ),
        ],
      ),
    );
  }
}
