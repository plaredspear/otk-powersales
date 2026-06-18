import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';

class OrderedItemList extends StatelessWidget {
  final List<OrderedItem> items;

  const OrderedItemList({
    super.key,
    required this.items,
  });

  String _formatBoxes(double boxes) {
    if (boxes == boxes.toInt()) {
      return boxes.toInt().toString();
    }
    return boxes.toStringAsFixed(1);
  }

  String _formatPieces(int pieces) {
    return NumberFormat('#,###').format(pieces);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(
          color: AppColors.border,
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '주문한 제품 (${items.length})',
            style: AppTypography.headlineSmall.copyWith(
              color: AppColors.textPrimary,
              fontWeight: FontWeight.bold,
            ),
          ),
          SizedBox(height: AppSpacing.md),
          if (items.isEmpty)
            Center(
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: AppSpacing.xl),
                child: Text(
                  '주문한 제품이 없습니다',
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textTertiary,
                  ),
                ),
              ),
            )
          else
            ...items.asMap().entries.map((entry) {
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

  Widget _buildItemRow(OrderedItem item) {
    // 레거시 view.jsp:414 동등 — 취소/결품 제품은 회색 처리.
    final isGrayed = item.isCancelled || item.isOutOfStock;
    final nameColor =
        isGrayed ? AppColors.textTertiary : AppColors.textPrimary;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              flex: 3,
              child: RichText(
                text: TextSpan(
                  children: [
                    if (item.isCancelled)
                      TextSpan(
                        text: '[취소] ',
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.error,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    if (item.isOutOfStock)
                      TextSpan(
                        text: '[결품] ',
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.error,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    TextSpan(
                      text: '(${item.productCode}) ${item.productName}',
                      style: AppTypography.bodyMedium.copyWith(
                        color: nameColor,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            SizedBox(width: AppSpacing.md),
            Expanded(
              flex: 2,
              child: Text(
                '${_formatBoxes(item.totalQuantityBoxes)}박스 (${_formatPieces(item.totalQuantityPieces)}개)',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
                textAlign: TextAlign.right,
              ),
            ),
          ],
        ),
        // 결품 사유 (SAP DefaultReason) — 레거시 결품 표시 동등.
        if (item.isOutOfStock && item.outOfStockReason != null) ...[
          SizedBox(height: AppSpacing.xs),
          Text(
            '결품사유: ${item.outOfStockReason}',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.error,
            ),
          ),
        ],
      ],
    );
  }
}
