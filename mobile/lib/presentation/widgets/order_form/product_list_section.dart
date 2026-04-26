import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_draft.dart';
import '../../../domain/entities/validation_error.dart';
import 'order_product_card.dart';

/// 제품 목록 섹션
class ProductListSection extends StatelessWidget {
  final List<OrderDraftItem> items;
  final Map<String, ValidationError> validationErrors;
  final bool allItemsSelected;
  final ValueChanged<String> onToggleSelection;
  final VoidCallback onToggleSelectAll;
  final VoidCallback onAddProduct;
  final VoidCallback onBarcodeScan;
  final VoidCallback onRemoveSelected;
  final Function(String productCode, double boxes, int pieces) onQuantityChanged;
  final ScrollController? scrollController;

  const ProductListSection({
    super.key,
    required this.items,
    required this.validationErrors,
    required this.allItemsSelected,
    required this.onToggleSelection,
    required this.onToggleSelectAll,
    required this.onAddProduct,
    required this.onBarcodeScan,
    required this.onRemoveSelected,
    required this.onQuantityChanged,
    this.scrollController,
  });

  @override
  Widget build(BuildContext context) {
    final hasSelectedItems = items.any((item) => item.isSelected);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            RichText(
              text: TextSpan(
                text: '제품 ',
                style: AppTypography.headlineSmall.copyWith(
                  color: AppColors.textPrimary,
                ),
                children: [
                  TextSpan(
                    text: '*',
                    style: TextStyle(
                      color: AppColors.error,
                    ),
                  ),
                  TextSpan(
                    text: ' (수량: ${items.length}개)',
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
            const Spacer(),
          ],
        ),
        const SizedBox(height: AppSpacing.md),
        ListView.builder(
          controller: scrollController,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: items.length,
          itemBuilder: (context, index) {
            final item = items[index];
            final error = validationErrors[item.productCode];

            return OrderProductCard(
              item: item,
              validationError: error,
              onSelectionChanged: (selected) {
                onToggleSelection(item.productCode);
              },
              onQuantityChanged: (boxes, pieces) {
                onQuantityChanged(item.productCode, boxes, pieces);
              },
              onRemove: () {
                onToggleSelection(item.productCode);
                onRemoveSelected();
              },
            );
          },
        ),
        const Divider(height: AppSpacing.lg),
        Row(
          children: [
            Checkbox(
              value: allItemsSelected,
              onChanged: (value) => onToggleSelectAll(),
            ),
            GestureDetector(
              onTap: onToggleSelectAll,
              child: Text(
                '전체 선택 / 해제',
                style: AppTypography.bodyMedium,
              ),
            ),
            const Spacer(),
            TextButton(
              onPressed: hasSelectedItems ? onRemoveSelected : null,
              child: Text(
                '선택 삭제',
                style: AppTypography.labelMedium.copyWith(
                  color: hasSelectedItems
                      ? AppColors.error
                      : AppColors.textSecondary,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.md),
        Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: onBarcodeScan,
                icon: const Icon(Icons.qr_code_scanner),
                label: const Text('바코드'),
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size(0, 48),
                  side: BorderSide(color: AppColors.border),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  ),
                ),
              ),
            ),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              flex: 2,
              child: ElevatedButton.icon(
                onPressed: onAddProduct,
                icon: const Icon(Icons.add),
                label: const Text('추가'),
                style: ElevatedButton.styleFrom(
                  minimumSize: const Size(0, 48),
                  backgroundColor: AppColors.primary,
                  foregroundColor: AppColors.onPrimary,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  ),
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}
