import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';

/// 미납 제품 목록 (신규 정책, 2026-07-20 사용자 결정 — SF 레거시엔 없던 섹션)
///
/// SAP 주문번호가 있는 라인 중 `LineItemStatus` != "OK" 인 제품을 제품 표시 UI 최상단에
/// 모아 표시합니다. 반려(빨강)와 구분되도록 주황(warning) 계열로 표시합니다.
class UnfulfilledItemList extends StatelessWidget {
  final List<UnfulfilledItem> unfulfilledItems;

  const UnfulfilledItemList({
    super.key,
    required this.unfulfilledItems,
  });

  /// BOX 수량 포맷 — 소수 박스는 표시하되 정수는 후행 소수 억제 (`RejectedItemList._formatBoxes` 정합).
  String _formatBoxes(double boxes) => NumberFormat('#,##0.##').format(boxes);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(
          color: AppColors.warning,
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
                  color: AppColors.warning,
                  shape: BoxShape.circle,
                ),
              ),
              SizedBox(width: AppSpacing.sm),
              Text(
                '미납 제품 (${unfulfilledItems.length})',
                style: AppTypography.headlineSmall.copyWith(
                  color: AppColors.warning,
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
          ...unfulfilledItems.asMap().entries.map((entry) {
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

  Widget _buildItemRow(UnfulfilledItem item) {
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
            color: AppColors.warning.withOpacity(0.1),
            borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          ),
          child: Text(
            '미납사유: ${item.reason}',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.warning,
            ),
          ),
        ),
      ],
    );
  }
}
