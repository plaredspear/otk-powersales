import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../providers/add_product_provider.dart';
import 'product_card_for_add.dart';

/// 제품 검색 탭
class SearchProductsTab extends ConsumerStatefulWidget {
  final ScrollController scrollController;

  const SearchProductsTab({
    super.key,
    required this.scrollController,
  });

  @override
  ConsumerState<SearchProductsTab> createState() => _SearchProductsTabState();
}

class _SearchProductsTabState extends ConsumerState<SearchProductsTab> {
  final _searchController = TextEditingController();
  Timer? _debounceTimer;

  @override
  void dispose() {
    _searchController.dispose();
    _debounceTimer?.cancel();
    super.dispose();
  }

  void _onSearchChanged(String query) {
    _debounceTimer?.cancel();
    _debounceTimer = Timer(const Duration(milliseconds: 500), () {
      ref.read(addProductProvider.notifier).searchProducts(query: query);
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(addProductProvider);
    final notifier = ref.read(addProductProvider.notifier);

    return Column(
      children: [
        // 검색 바
        Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: TextField(
            controller: _searchController,
            onChanged: _onSearchChanged,
            decoration: InputDecoration(
              hintText: '제품명 또는 제품코드를 입력하세요',
              hintStyle: AppTypography.bodyMedium.copyWith(
                color: AppColors.textTertiary,
              ),
              prefixIcon: const Icon(Icons.search),
              suffixIcon: _searchController.text.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.clear),
                      onPressed: () {
                        _searchController.clear();
                        notifier.searchProducts(query: '');
                      },
                    )
                  : null,
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
        // 검색 결과
        Expanded(
          child: _buildSearchResults(state, notifier),
        ),
      ],
    );
  }

  Widget _buildSearchResults(dynamic state, dynamic notifier) {
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (state.searchQuery.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.search,
              size: 48,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.md),
            Text(
              '제품을 검색하세요.',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      );
    }

    if (state.searchResults.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.search_off,
              size: 48,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.md),
            Text(
              '검색 결과가 없습니다.',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      controller: widget.scrollController,
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
      itemCount: state.searchResults.length,
      itemBuilder: (context, index) {
        final product = state.searchResults[index];
        return ProductCardForAdd(
          product: product,
          isSelected: state.isProductSelected(product.productCode),
          onSelectionChanged: (_) {
            ref
                .read(addProductProvider.notifier)
                .toggleProductSelection(product.productCode);
          },
          onFavoriteToggle: () {
            if (product.isFavorite) {
              ref
                  .read(addProductProvider.notifier)
                  .removeFromFavorites(product.productCode);
            } else {
              ref
                  .read(addProductProvider.notifier)
                  .addToFavorites(product.productCode);
            }
          },
          isFavoriteTab: false,
        );
      },
    );
  }
}
