import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_draft.dart';
import '../../../domain/entities/validation_error.dart';

/// 주문서 제품 카드
class OrderProductCard extends StatelessWidget {
  final OrderDraftItem item;
  final ValidationError? validationError;
  final ValueChanged<bool?> onSelectionChanged;
  final Function(double boxes, int pieces) onQuantityChanged;
  final VoidCallback onRemove;

  const OrderProductCard({
    super.key,
    required this.item,
    required this.validationError,
    required this.onSelectionChanged,
    required this.onQuantityChanged,
    required this.onRemove,
  });

  String _formatNumber(int value) {
    return value.toString().replaceAllMapped(
      RegExp(r'(\d)(?=(\d{3})+(?!\d))'),
      (m) => '${m[1]},',
    );
  }

  @override
  Widget build(BuildContext context) {
    final hasError = validationError != null;

    return Card(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        side: hasError
            ? BorderSide(color: AppColors.error, width: 1)
            : BorderSide(color: AppColors.border, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CheckboxListTile(
            value: item.isSelected,
            onChanged: onSelectionChanged,
            controlAffinity: ListTileControlAffinity.leading,
            contentPadding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.md,
              vertical: AppSpacing.sm,
            ),
            title: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.productName,
                  style: AppTypography.headlineSmall,
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  '제품코드: ${item.productCode}',
                  style: AppTypography.bodySmall,
                ),
                const SizedBox(height: AppSpacing.sm),
                Row(
                  children: [
                    Text(
                      '박스',
                      style: AppTypography.bodyMedium,
                    ),
                    const SizedBox(width: AppSpacing.sm),
                    SizedBox(
                      width: 80,
                      child: TextField(
                        controller: TextEditingController(
                          text: item.quantityBoxes > 0
                              ? item.quantityBoxes.toString()
                              : '',
                        ),
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                        inputFormatters: [
                          FilteringTextInputFormatter.allow(
                            RegExp(r'^\d*\.?\d*'),
                          ),
                        ],
                        decoration: InputDecoration(
                          isDense: true,
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: AppSpacing.sm,
                            vertical: AppSpacing.sm,
                          ),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(
                              AppSpacing.radiusSm,
                            ),
                          ),
                        ),
                        onChanged: (value) {
                          final boxes = double.tryParse(value) ?? 0.0;
                          onQuantityChanged(boxes, item.quantityPieces);
                        },
                      ),
                    ),
                    const SizedBox(width: AppSpacing.sm),
                    Text(
                      '개',
                      style: AppTypography.bodyMedium,
                    ),
                    const SizedBox(width: AppSpacing.md),
                    Text(
                      '낱개',
                      style: AppTypography.bodyMedium,
                    ),
                    const SizedBox(width: AppSpacing.sm),
                    SizedBox(
                      width: 80,
                      child: TextField(
                        controller: TextEditingController(
                          text: item.quantityPieces > 0
                              ? item.quantityPieces.toString()
                              : '',
                        ),
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        decoration: InputDecoration(
                          isDense: true,
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: AppSpacing.sm,
                            vertical: AppSpacing.sm,
                          ),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(
                              AppSpacing.radiusSm,
                            ),
                          ),
                        ),
                        onChanged: (value) {
                          final pieces = int.tryParse(value) ?? 0;
                          onQuantityChanged(item.quantityBoxes, pieces);
                        },
                      ),
                    ),
                    const SizedBox(width: AppSpacing.sm),
                    Text(
                      '개',
                      style: AppTypography.bodyMedium,
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  '1박스 = ${item.boxSize}개',
                  style: AppTypography.bodySmall,
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  '소계: ${_formatNumber(item.totalPrice)}원',
                  style: AppTypography.labelLarge,
                ),
              ],
            ),
            secondary: IconButton(
              icon: const Icon(Icons.close),
              onPressed: onRemove,
              color: AppColors.textSecondary,
            ),
          ),
          if (hasError)
            Container(
              width: double.infinity,
              padding: AppSpacing.cardPadding,
              decoration: BoxDecoration(
                color: AppColors.errorLight,
                borderRadius: const BorderRadius.only(
                  bottomLeft: Radius.circular(AppSpacing.radiusMd),
                  bottomRight: Radius.circular(AppSpacing.radiusMd),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (validationError!.minOrderQuantity != null)
                    Text(
                      '최소 주문수량: ${validationError!.minOrderQuantity}박스',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.error,
                      ),
                    ),
                  if (validationError!.supplyQuantity != null)
                    Text(
                      '공급수량: ${validationError!.supplyQuantity}개',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.error,
                      ),
                    ),
                  if (validationError!.dcQuantity != null)
                    Text(
                      'DC수량: ${validationError!.dcQuantity}개',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.error,
                      ),
                    ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    validationError!.message,
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.error,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
