import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../common/synced_text_field.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_draft.dart';
import '../../../domain/entities/validation_error.dart';

/// 주문서 제품 카드
class OrderProductCard extends StatelessWidget {
  /// 라인 순번 (레거시 "N. (코드) 제품명" 표기용, 0-based).
  final int index;
  final OrderDraftItem item;
  final ValidationError? validationError;
  final ValueChanged<bool?> onSelectionChanged;
  final Function(double boxes, int pieces) onQuantityChanged;

  const OrderProductCard({
    super.key,
    required this.index,
    required this.item,
    required this.validationError,
    required this.onSelectionChanged,
    required this.onQuantityChanged,
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
    // 레거시 write.jsp: 총 EA = 박스 × 1박스당 EA + 낱개.
    final totalEach =
        (item.quantityBoxes * item.boxSize).round() + item.quantityPieces;

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
          Padding(
            padding: const EdgeInsets.all(AppSpacing.md),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 레거시: "N. (제품코드) 제품명" + 우측 체크박스 (라인별 X 버튼 없음).
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.only(top: AppSpacing.sm),
                        child: Text(
                          '${index + 1}. (${item.productCode}) ${item.productName}',
                          style: AppTypography.headlineSmall,
                        ),
                      ),
                    ),
                    const SizedBox(width: AppSpacing.sm),
                    Checkbox(
                      value: item.isSelected,
                      onChanged: onSelectionChanged,
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.sm),
                // 레거시: [박스] [낱개(개)] 입력 + 총 EA 표시.
                Row(
                  children: [
                    Expanded(
                      child: SyncedTextField(
                        // 레거시 write.jsp: 박스 입력칸(.unitQty)은 parseInt 로
                        // 정수만 유효. 표시/입력 모두 정수 단위로 맞춘다.
                        value: item.quantityBoxes > 0
                            ? item.quantityBoxes.toInt().toString()
                            : '',
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        decoration: InputDecoration(
                          isDense: true,
                          hintText: '0',
                          suffixText: '박스',
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
                          final boxes = (int.tryParse(value) ?? 0).toDouble();
                          onQuantityChanged(boxes, item.quantityPieces);
                        },
                      ),
                    ),
                    const SizedBox(width: AppSpacing.sm),
                    Expanded(
                      child: SyncedTextField(
                        value: item.quantityPieces > 0
                            ? item.quantityPieces.toString()
                            : '',
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        decoration: InputDecoration(
                          isDense: true,
                          hintText: '0',
                          suffixText: '개',
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
                    const SizedBox(width: AppSpacing.md),
                    Text(
                      '총 ${_formatNumber(totalEach)}개',
                      style: AppTypography.bodyMedium.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.sm),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      '1박스 = ${item.boxSize}개',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                    Text(
                      '소계: ${_formatNumber(item.totalPrice)}원',
                      style: AppTypography.labelLarge,
                    ),
                  ],
                ),
              ],
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
