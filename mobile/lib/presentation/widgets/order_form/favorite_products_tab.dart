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
  final bool requireBarcode;
  final bool blockExclusive;

  const FavoriteProductsTab({
    super.key,
    required this.scrollController,
    this.requireBarcode = false,
    this.blockExclusive = false,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(addProductProvider);
    final notifier = ref.read(addProductProvider.notifier);

    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    // 바코드 필수 화면(POS/전산 매출조회)에서는 바코드 없는 제품을 목록에서
    // 제외한다(레거시 productMapper `is not null` 정합 — 노출 후 차단이 아님).
    final products = requireBarcode
        ? state.favoriteProducts
            .where((p) => p.barcode.trim().isNotEmpty)
            .toList()
        : state.favoriteProducts;

    if (products.isEmpty) {
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

    // 전체 선택 대상 — 선택이 차단된 제품(전용상품)은 제외한다.
    final selectableCodes = products
        .where((p) => !(blockExclusive && p.isExclusiveBlocked))
        .map((p) => p.productCode)
        .toList();
    final allSelected = selectableCodes.isNotEmpty &&
        selectableCodes.every(state.isProductSelected);

    return Column(
      children: [
        // 전체 선택 (다건 선택 모드에서만 노출)
        if (state.multiSelect && selectableCodes.isNotEmpty)
          _SelectAllBar(
            isSelected: allSelected,
            count: selectableCodes.length,
            onChanged: (selected) {
              notifier.setSelectionForCodes(selectableCodes, selected);
            },
          ),
        Expanded(
          child: ListView.builder(
            controller: scrollController,
            padding: AppSpacing.screenAll,
            itemCount: products.length,
            itemBuilder: (context, index) {
              final product = products[index];
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
                blockExclusive: blockExclusive,
              );
            },
          ),
        ),
      ],
    );
  }
}

/// 즐겨찾기 전체 선택 바
class _SelectAllBar extends StatelessWidget {
  final bool isSelected;
  final int count;
  final ValueChanged<bool> onChanged;

  const _SelectAllBar({
    required this.isSelected,
    required this.count,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () => onChanged(!isSelected),
      child: Container(
        padding: const EdgeInsets.only(
          left: AppSpacing.sm,
          right: AppSpacing.lg,
        ),
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(color: AppColors.divider, width: 1),
          ),
        ),
        child: Row(
          children: [
            Checkbox(
              value: isSelected,
              onChanged: (value) => onChanged(value ?? false),
              activeColor: AppColors.primary,
            ),
            Text(
              '전체 선택',
              style: AppTypography.labelLarge,
            ),
            const Spacer(),
            Text(
              '$count개',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
