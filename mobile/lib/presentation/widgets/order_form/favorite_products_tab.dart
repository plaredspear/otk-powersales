import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../providers/add_product_provider.dart';
import 'product_card_for_add.dart';

/// 즐겨찾기 제품 탭
class FavoriteProductsTab extends ConsumerWidget {
  final ScrollController scrollController;

  const FavoriteProductsTab({
    super.key,
    required this.scrollController,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(addProductProvider);
    final notifier = ref.read(addProductProvider.notifier);

    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (state.favoriteProducts.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.star_border,
              size: 48,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.md),
            Text(
              '즐겨찾기 제품이 없습니다.',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              '제품 검색 탭에서 즐겨찾기를 추가하세요.',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      controller: scrollController,
      padding: AppSpacing.screenAll,
      itemCount: state.favoriteProducts.length,
      itemBuilder: (context, index) {
        final product = state.favoriteProducts[index];
        return ProductCardForAdd(
          product: product,
          isSelected: state.isProductSelected(product.productCode),
          onSelectionChanged: (_) {
            notifier.toggleProductSelection(product.productCode);
          },
          onFavoriteToggle: () {
            notifier.removeFromFavorites(product.productCode);
          },
          isFavoriteTab: true,
        );
      },
    );
  }
}
