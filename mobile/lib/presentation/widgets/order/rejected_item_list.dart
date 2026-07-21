import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';

class RejectedItemList extends StatelessWidget {
  final List<RejectedItem> rejectedItems;

  const RejectedItemList({
    super.key,
    required this.rejectedItems,
  });

  /// BOX 수량 포맷 — 소수 박스는 표시하되 정수는 후행 소수 억제 (`OrderedItemList._formatBoxes` 정합).
  String _formatBoxes(double boxes) => NumberFormat('#,##0.##').format(boxes);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(
          color: AppColors.error,
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
                  color: AppColors.error,
                  shape: BoxShape.circle,
                ),
              ),
              SizedBox(width: AppSpacing.sm),
              Text(
                '주문 반려 제품 (${rejectedItems.length})',
                style: AppTypography.headlineSmall.copyWith(
                  color: AppColors.error,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          SizedBox(height: AppSpacing.sm),
          Text(
            '반려제품처리에 시간이 걸릴 수 있습니다.',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          SizedBox(height: AppSpacing.md),
          Divider(
            height: 1,
            color: AppColors.divider,
          ),
          SizedBox(height: AppSpacing.md),
          ...rejectedItems.asMap().entries.map((entry) {
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

  Widget _buildItemRow(RejectedItem item) {
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
            color: AppColors.errorLight.withOpacity(0.1),
            borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          ),
          child: Text(
            '반려사유: ${item.rejectionReason}',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.error,
            ),
          ),
        ),
      ],
    );
  }
}
