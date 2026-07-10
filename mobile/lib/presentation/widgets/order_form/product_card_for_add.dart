import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/product_for_order.dart';

/// 제품 추가용 카드 위젯
///
/// 즐겨찾기/검색/주문이력 탭에서 공통으로 사용되는 제품 카드입니다.
/// 체크박스, 즐겨찾기 토글, 제품 정보를 표시합니다.
class ProductCardForAdd extends StatelessWidget {
  final ProductForOrder product;
  final bool isSelected;
  final ValueChanged<bool?> onSelectionChanged;
  final VoidCallback? onFavoriteToggle;
  final bool isFavoriteTab;
  final bool showFavoriteButton;

  const ProductCardForAdd({
    super.key,
    required this.product,
    required this.isSelected,
    required this.onSelectionChanged,
    this.onFavoriteToggle,
    this.isFavoriteTab = false,
    this.showFavoriteButton = true,
  });

  String _formatNumber(int value) {
    return value.toString().replaceAllMapped(
      RegExp(r'(\d)(?=(\d{3})+(?!\d))'),
      (m) => '${m[1]},',
    );
  }

  @override
  Widget build(BuildContext context) {
    // 전용상품(예외 코드 제외)은 주문서에 추가할 수 없으므로 선택을 막는다.
    final bool isBlocked = product.isExclusiveBlocked;

    return Card(
      margin: const EdgeInsets.only(bottom: AppSpacing.sm),
      color: isBlocked ? AppColors.surface : null,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        side: BorderSide(
          color: isSelected ? AppColors.primary : AppColors.border,
          width: isSelected ? 2 : 1,
        ),
      ),
      child: InkWell(
        // 전용상품은 선택 불가 — 인라인 안내로 사유를 표시한다.
        onTap: isBlocked ? null : () => onSelectionChanged(!isSelected),
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.md),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 체크박스 (전용상품은 선택 불가 → 차단 아이콘)
              if (isBlocked)
                const SizedBox(
                  width: 48,
                  height: 48,
                  child: Icon(Icons.block, color: AppColors.error),
                )
              else
                Checkbox(
                  value: isSelected,
                  onChanged: onSelectionChanged,
                  activeColor: AppColors.primary,
                ),
              // 제품 정보
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: Text(
                            product.productName,
                            style: AppTypography.labelLarge,
                          ),
                        ),
                        if (isBlocked) ...[
                          const SizedBox(width: AppSpacing.xs),
                          _ExclusiveBadge(),
                        ],
                      ],
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      '제품코드: ${product.productCode}',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      '바코드: ${product.barcode}',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    Row(
                      children: [
                        Text(
                          '${product.storageTypeIcon} ${product.storageType}',
                          style: AppTypography.bodySmall,
                        ),
                        const SizedBox(width: AppSpacing.md),
                        Text(
                          '소비기한 ${product.shelfLife}',
                          style: AppTypography.bodySmall.copyWith(
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    Row(
                      children: [
                        Text(
                          '단가: ${_formatNumber(product.unitPrice)}원',
                          style: AppTypography.bodySmall,
                        ),
                        const SizedBox(width: AppSpacing.md),
                        Text(
                          '1박스 = ${product.boxSize}개',
                          style: AppTypography.bodySmall.copyWith(
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ],
                    ),
                    if (isBlocked) ...[
                      const SizedBox(height: AppSpacing.xs),
                      Row(
                        children: [
                          const Icon(
                            Icons.info_outline,
                            size: 14,
                            color: AppColors.error,
                          ),
                          const SizedBox(width: AppSpacing.xs),
                          Expanded(
                            child: Text(
                              '전용상품은 주문이 불가능합니다.',
                              style: AppTypography.labelSmall.copyWith(
                                color: AppColors.error,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
              // 즐겨찾기 버튼
              if (showFavoriteButton && onFavoriteToggle != null)
                IconButton(
                  icon: Icon(
                    isFavoriteTab || product.isFavorite
                        ? Icons.star
                        : Icons.star_border,
                    color: isFavoriteTab || product.isFavorite
                        ? Colors.amber
                        : AppColors.textTertiary,
                  ),
                  onPressed: onFavoriteToggle,
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 전용상품 표시 뱃지
///
/// 전용상품(`producttype__c == '2'`)은 주문서에 추가할 수 없으므로,
/// 제품 목록에서 식별할 수 있도록 제품명 옆에 표시합니다.
class _ExclusiveBadge extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.xs,
        vertical: 2,
      ),
      decoration: BoxDecoration(
        color: AppColors.errorLight,
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
        border: Border.all(color: AppColors.error),
      ),
      child: Text(
        '전용상품',
        style: AppTypography.labelSmall.copyWith(
          color: AppColors.error,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}
