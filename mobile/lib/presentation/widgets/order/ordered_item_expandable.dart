import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';

class OrderedItemExpandable extends StatelessWidget {
  final List<OrderedItem> items;
  final int itemCount;
  final bool isExpanded;
  final VoidCallback onToggle;

  const OrderedItemExpandable({
    super.key,
    required this.items,
    required this.itemCount,
    required this.isExpanded,
    required this.onToggle,
  });

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
          InkWell(
            onTap: onToggle,
            borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
            child: Padding(
              padding: EdgeInsets.symmetric(vertical: AppSpacing.xs),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      Text(
                        '주문한 제품',
                        style: AppTypography.headlineSmall.copyWith(
                          color: AppColors.textPrimary,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      SizedBox(width: AppSpacing.sm),
                      Text(
                        '$itemCount개',
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                  Row(
                    children: [
                      Text(
                        isExpanded ? '숨기기' : '제품 보기',
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.primary,
                        ),
                      ),
                      Icon(
                        isExpanded
                            ? Icons.keyboard_arrow_up
                            : Icons.keyboard_arrow_down,
                        color: AppColors.primary,
                        size: 20,
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          if (isExpanded) ...[
            SizedBox(height: AppSpacing.md),
            Divider(
              height: 1,
              color: AppColors.divider,
            ),
            SizedBox(height: AppSpacing.md),
            _buildItemList(),
          ],
        ],
      ),
    );
  }

  Widget _buildItemList() {
    if (items.isEmpty) {
      return Center(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: AppSpacing.xl),
          child: Text(
            '주문한 제품이 없습니다',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textTertiary,
            ),
          ),
        ),
      );
    }

    return Column(
      children: items.asMap().entries.map((entry) {
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
      }).toList(),
    );
  }

  Widget _buildItemRow(OrderedItem item) {
    return Row(
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
                TextSpan(
                  text: '(${item.productCode}) ${item.productName}',
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textPrimary,
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
    );
  }

  String _formatBoxes(double boxes) {
    if (boxes == boxes.toInt()) {
      return boxes.toInt().toString();
    }
    return boxes.toStringAsFixed(1);
  }

  String _formatPieces(int pieces) {
    return NumberFormat('#,###').format(pieces);
  }
}
