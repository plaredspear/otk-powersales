import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';

/// 결품 제품 목록 (2026-07-23 사용자 결정 — 반려 섹션처럼 별도 영역)
///
/// SAP `DefaultReason` 코드가 결품셋({F1,L1,L2,L3})으로 분류된 제품을 "주문 반려 제품" 영역과 동일한
/// 레이아웃으로 표시한다. "주문한 제품" 목록에서는 제외되며(백엔드), 반려(빨강)와 구분되도록 회색 계열로
/// 표시한다. 마감 전후 모두 노출(반려 섹션 정책과 동일).
class OutOfStockItemList extends StatelessWidget {
  final List<OutOfStockItem> outOfStockItems;

  const OutOfStockItemList({
    super.key,
    required this.outOfStockItems,
  });

  /// BOX 수량 포맷 — `RejectedItemList._formatBoxes` 정합.
  String _formatBoxes(double boxes) => NumberFormat('#,##0.##').format(boxes);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(
          color: AppColors.textTertiary,
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 8,
                height: 8,
                decoration: BoxDecoration(
                  color: AppColors.textTertiary,
                  shape: BoxShape.circle,
                ),
              ),
              SizedBox(width: AppSpacing.sm),
              Text(
                '결품 제품 (${outOfStockItems.length})',
                style: AppTypography.headlineSmall.copyWith(
                  color: AppColors.textSecondary,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          SizedBox(height: AppSpacing.md),
          Divider(
            height: 1,
            color: AppColors.divider,
          ),
          SizedBox(height: AppSpacing.md),
          ...outOfStockItems.asMap().entries.map((entry) {
            final index = entry.key;
            final item = entry.value;
            return Column(
              children: [
                if (index > 0)
                  Divider(
                    height: AppSpacing.lg,
                    color: AppColors.divider,
                  ),
                _buildItemRow(item),
              ],
            );
          }),
        ],
      ),
    );
  }

  Widget _buildItemRow(OutOfStockItem item) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              flex: 3,
              child: Text(
                '${item.productName} (${item.productCode})',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
            ),
            SizedBox(width: AppSpacing.md),
            Expanded(
              flex: 2,
              child: Text(
                '${_formatBoxes(item.orderQuantityBoxes)} BOX',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
                textAlign: TextAlign.right,
              ),
            ),
          ],
        ),
        SizedBox(height: AppSpacing.xs),
        Container(
          width: double.infinity,
          padding: EdgeInsets.all(AppSpacing.sm),
          decoration: BoxDecoration(
            // ignore: deprecated_member_use
            color: AppColors.textTertiary.withOpacity(0.1),
            borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          ),
          child: Text(
            '결품사유: ${item.reason}',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ),
      ],
    );
  }
}
