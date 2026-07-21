import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/product_for_order.dart';
import '../../providers/add_product_provider.dart';
import 'product_card_for_add.dart';

/// 즐겨찾기 제품 탭
class FavoriteProductsTab extends ConsumerStatefulWidget {
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
  ConsumerState<FavoriteProductsTab> createState() =>
      _FavoriteProductsTabState();
}

class _FavoriteProductsTabState extends ConsumerState<FavoriteProductsTab> {
  final _filterController = TextEditingController();
  String _keyword = '';

  @override
  void dispose() {
    _filterController.dispose();
    super.dispose();
  }

  /// 즐겨찾기 목록을 제품명/제품코드/바코드 키워드로 클라이언트 필터링한다.
  List<ProductForOrder> _applyKeyword(List<ProductForOrder> products) {
    final keyword = _keyword.trim().toLowerCase();
    if (keyword.isEmpty) return products;
    return products.where((p) {
      return p.productName.toLowerCase().contains(keyword) ||
          p.productCode.toLowerCase().contains(keyword) ||
          p.barcode.toLowerCase().contains(keyword);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(addProductProvider);
    final notifier = ref.read(addProductProvider.notifier);

    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    // 바코드 필수 화면(POS/전산 매출조회)에서는 바코드 없는 제품을 목록에서
    // 제외한다(레거시 productMapper `is not null` 정합 — 노출 후 차단이 아님).
    final favorites = widget.requireBarcode
        ? state.favoriteProducts
            .where((p) => p.barcode.trim().isNotEmpty)
            .toList()
        : state.favoriteProducts;

    if (favorites.isEmpty) {
      return _EmptyState(
        icon: Icons.star_border,
        title: '즐겨찾기 제품이 없습니다.',
        subtitle: '제품 검색 탭에서 즐겨찾기를 추가하세요.',
      );
    }

    final products = _applyKeyword(favorites);

    // 전체 선택 대상 — 필터된 목록 중 선택이 차단된 제품(전용상품)은 제외한다.
    final selectableCodes = products
        .where((p) => !(widget.blockExclusive && p.isExclusiveBlocked))
        .map((p) => p.productCode)
        .toList();
    final allSelected = selectableCodes.isNotEmpty &&
        selectableCodes.every(state.isProductSelected);

    return Column(
      children: [
        // 즐겨찾기 목록 내 키워드 필터
        Padding(
          padding: const EdgeInsets.fromLTRB(
            AppSpacing.lg,
            AppSpacing.md,
            AppSpacing.lg,
            AppSpacing.sm,
          ),
          child: TextField(
            controller: _filterController,
            onChanged: (value) => setState(() => _keyword = value),
            decoration: InputDecoration(
              hintText: '즐겨찾기 내 제품명·코드·바코드 검색',
              hintStyle: AppTypography.bodyMedium.copyWith(
                color: AppColors.textTertiary,
              ),
              prefixIcon: const Icon(Icons.search),
              suffixIcon: _keyword.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.clear),
                      onPressed: () {
                        _filterController.clear();
                        setState(() => _keyword = '');
                      },
                    )
                  : null,
              isDense: true,
              contentPadding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.md,
                vertical: AppSpacing.md,
              ),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                borderSide: BorderSide(color: AppColors.border),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                borderSide: BorderSide(color: AppColors.border),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                borderSide: BorderSide(color: AppColors.primary, width: 2),
              ),
            ),
          ),
        ),
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
          child: products.isEmpty
              ? _EmptyState(
                  icon: Icons.search_off,
                  title: '검색 결과가 없습니다.',
                )
              : ListView.builder(
                  controller: widget.scrollController,
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
                      blockExclusive: widget.blockExclusive,
                    );
                  },
                ),
        ),
      ],
    );
  }
}

/// 즐겨찾기 탭 빈 상태 (즐겨찾기 없음 / 검색 결과 없음 공용).
class _EmptyState extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;

  const _EmptyState({
    required this.icon,
    required this.title,
    this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 48, color: AppColors.textTertiary),
          const SizedBox(height: AppSpacing.md),
          Text(
            title,
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          if (subtitle != null) ...[
            const SizedBox(height: AppSpacing.xs),
            Text(
              subtitle!,
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
          ],
        ],
      ),
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
