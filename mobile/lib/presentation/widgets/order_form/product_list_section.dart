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
        // 레거시 write.jsp: "제품 *" 라벨 우측에 바코드 / +추가 버튼 배치.
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
                ],
              ),
            ),
            const Spacer(),
            OutlinedButton.icon(
              onPressed: onBarcodeScan,
              icon: const Icon(Icons.qr_code_scanner, size: 18),
              label: const Text('바코드'),
              style: OutlinedButton.styleFrom(
                // Row 안 무한폭 제약 크래시 방지 (전역 테마 덮어쓰기).
                minimumSize: Size.zero,
                foregroundColor: AppColors.textPrimary,
                side: BorderSide(color: AppColors.textSecondary),
                shape: const StadiumBorder(),
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md,
                ),
                visualDensity: VisualDensity.compact,
              ),
            ),
            const SizedBox(width: AppSpacing.sm),
            OutlinedButton.icon(
              onPressed: onAddProduct,
              icon: const Icon(Icons.add, size: 18),
              label: const Text('추가'),
              style: OutlinedButton.styleFrom(
                // Row 안 무한폭 제약 크래시 방지 (전역 테마 덮어쓰기).
                minimumSize: Size.zero,
                foregroundColor: AppColors.textPrimary,
                side: BorderSide(color: AppColors.textSecondary),
                shape: const StadiumBorder(),
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md,
                ),
                visualDensity: VisualDensity.compact,
              ),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.sm),
        // 레거시 write.jsp: 100개 권장 안내 (빨강, 2줄).
        Text(
          '품목 추가는 100개 이하로 하시는 것을 권장합니다.',
          style: AppTypography.bodySmall.copyWith(color: AppColors.error),
        ),
        Text(
          '주문 품목이 100개를 초과하는 경우 분할하여 주문요청 부탁드립니다.',
          style: AppTypography.bodySmall.copyWith(color: AppColors.error),
        ),
        const SizedBox(height: AppSpacing.lg),
        // 레거시 write.jsp: 선택 삭제(좌, 빨강 버튼) / 전체 선택(우, 체크박스).
        Row(
          children: [
            ElevatedButton(
              onPressed: hasSelectedItems ? onRemoveSelected : null,
              style: ElevatedButton.styleFrom(
                // Row 안 무한폭 제약 크래시 방지 (전역 테마 덮어쓰기).
                minimumSize: Size.zero,
                backgroundColor: AppColors.error,
                foregroundColor: AppColors.white,
                // ignore: deprecated_member_use
                disabledBackgroundColor: AppColors.error.withOpacity(0.4),
                disabledForegroundColor: AppColors.white,
                elevation: 0,
                shape: const StadiumBorder(),
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.lg,
                ),
                visualDensity: VisualDensity.compact,
              ),
              child: const Text('선택 삭제'),
            ),
            const Spacer(),
            GestureDetector(
              onTap: onToggleSelectAll,
              child: Text(
                '전체 선택',
                style: AppTypography.bodyMedium,
              ),
            ),
            Checkbox(
              value: allItemsSelected,
              onChanged: (value) => onToggleSelectAll(),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.sm),
        ListView.builder(
          controller: scrollController,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: items.length,
          itemBuilder: (context, index) {
            final item = items[index];
            final error = validationErrors[item.productCode];

            return OrderProductCard(
              index: index,
              item: item,
              validationError: error,
              onSelectionChanged: (selected) {
                onToggleSelection(item.productCode);
              },
              onQuantityChanged: (boxes, pieces) {
                onQuantityChanged(item.productCode, boxes, pieces);
              },
            );
          },
        ),
      ],
    );
  }
}
