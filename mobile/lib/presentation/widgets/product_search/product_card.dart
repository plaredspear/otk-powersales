import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/product.dart';

/// 제품 카드 위젯
///
/// 검색 결과에서 각 제품 정보를 표시하는 카드입니다.
/// 제품명, 제품코드, 바코드, 보관조건, 클레임/주문서 등록 버튼을 포함합니다.
class ProductCard extends StatelessWidget {
  /// 제품 정보
  final Product product;

  /// 클레임 등록 버튼 탭 콜백
  final VoidCallback? onClaimTap;

  /// 주문서 등록 버튼 탭 콜백
  final VoidCallback? onOrderTap;

  const ProductCard({
    super.key,
    required this.product,
    this.onClaimTap,
    this.onOrderTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.xs,
      ),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(color: AppColors.border, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 제품 정보 영역
          Padding(
            padding: const EdgeInsets.all(AppSpacing.md),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 제품명
                Text(
                  product.productName,
                  style: AppTypography.headlineSmall,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: AppSpacing.xs),

                // 제품코드
                _buildInfoRow('제품코드', product.productCode),
                const SizedBox(height: AppSpacing.xxs),

                // 바코드
                _buildInfoRow('바코드', product.barcode),
                const SizedBox(height: AppSpacing.xxs),

                // 보관조건 | 유통기한
                _buildInfoRow(
                  '보관',
                  '${product.storageType} | ${product.shelfLife}',
                ),
              ],
            ),
          ),

          // 구분선
          const Divider(height: 1, color: AppColors.divider),

          // 하단 액션 버튼 영역
          Row(
            children: [
              // 클레임 등록 버튼
              Expanded(
                child: InkWell(
                  onTap: onClaimTap,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      vertical: AppSpacing.sm,
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.report_problem_outlined,
                          size: 18,
                          color: onClaimTap != null
                              ? AppColors.textSecondary
                              : AppColors.textTertiary,
                        ),
                        const SizedBox(width: AppSpacing.xxs),
                        Text(
                          '클레임 등록',
                          style: AppTypography.labelMedium.copyWith(
                            color: onClaimTap != null
                                ? AppColors.textSecondary
                                : AppColors.textTertiary,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),

              // 세로 구분선
              Container(
                width: 1,
                height: 20,
                color: AppColors.divider,
              ),

              // 주문서 등록 버튼
              Expanded(
                child: InkWell(
                  onTap: onOrderTap,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      vertical: AppSpacing.sm,
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.shopping_cart_outlined,
                          size: 18,
                          color: onOrderTap != null
                              ? AppColors.textSecondary
                              : AppColors.textTertiary,
                        ),
                        const SizedBox(width: AppSpacing.xxs),
                        Text(
                          '주문서 등록',
                          style: AppTypography.labelMedium.copyWith(
                            color: onOrderTap != null
                                ? AppColors.textSecondary
                                : AppColors.textTertiary,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Row(
      children: [
        Text(
          label,
          style: AppTypography.bodySmall.copyWith(
            color: AppColors.textTertiary,
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        Expanded(
          child: Text(
            value,
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ),
      ],
    );
  }
}
