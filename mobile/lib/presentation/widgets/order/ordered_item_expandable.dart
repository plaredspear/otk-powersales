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
                          color: AppColors.secondaryDark,
                        ),
                      ),
                      Icon(
                        isExpanded
                            ? Icons.keyboard_arrow_up
                            : Icons.keyboard_arrow_down,
                        color: AppColors.secondaryDark,
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
    // 취소/결품 제품은 짙은 회색 처리 + '[주문 취소]' 접두(빨간색) 공통.
    // (레거시 view.jsp:414-415 는 연한 회색 #CCC 였으나, 취소 항목 가독성 개선으로 짙은 회색 +
    //  접두만 빨간색으로 변경.)
    final isGrayed = item.isCancelled || item.isOutOfStock;
    final nameColor =
        isGrayed ? AppColors.textSecondary : AppColors.textPrimary;

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
                    if (isGrayed)
                      TextSpan(
                        text: '[주문 취소] ',
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.error,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    // 레거시 view.jsp:414 동등 — 제품명 (제품코드) 순서.
                    TextSpan(
                      text: '${item.productName} (${item.productCode})',
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
                '${_formatBoxes(item.totalQuantityBoxes)} BOX (${_formatPieces(item.totalQuantityPieces)}개)',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
                textAlign: TextAlign.right,
              ),
            ),
          ],
        ),
        if (item.isOutOfStock && item.outOfStockReason != null) ...[
          SizedBox(height: AppSpacing.xs),
          Text(
            '결품사유: ${item.outOfStockReason}',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ],
    );
  }

  // 레거시 view.jsp:420-438 동등 — 박스 수량은 소수 2자리까지(#,###.##).
  String _formatBoxes(double boxes) {
    return NumberFormat('#,##0.##').format(boxes);
  }

  String _formatPieces(int pieces) {
    return NumberFormat('#,###').format(pieces);
  }
}
