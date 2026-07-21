import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../providers/add_product_provider.dart';
import '../../providers/product_add_provider.dart';
import '../common/single_select_sheet.dart';
import 'product_card_for_add.dart';

/// 제품 검색 탭
class SearchProductsTab extends ConsumerStatefulWidget {
  final ScrollController scrollController;

  /// 중/소분류 필터 노출 여부(전산매출 등 분류검색용).
  final bool showCategoryFilter;

  /// 바코드 없는 제품 선택 차단 여부(POS/전산 매출조회 등).
  final bool requireBarcode;

  /// 전용상품 선택 차단 여부(주문서 작성에서만 true).
  final bool blockExclusive;

  const SearchProductsTab({
    super.key,
    required this.scrollController,
    this.showCategoryFilter = false,
    this.requireBarcode = false,
    this.blockExclusive = false,
  });

  @override
  ConsumerState<SearchProductsTab> createState() => _SearchProductsTabState();
}

class _SearchProductsTabState extends ConsumerState<SearchProductsTab> {
  final _searchController = TextEditingController();
  Timer? _debounceTimer;

  @override
  void initState() {
    super.initState();
    if (widget.showCategoryFilter) {
      // 분류 드롭다운 소스 로드(기존 제품추가 카테고리 인프라 재사용).
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ref.read(productAddProvider.notifier).loadCategories();
      });
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    _debounceTimer?.cancel();
    super.dispose();
  }

  void _onSearchChanged(String query) {
    _debounceTimer?.cancel();
    _debounceTimer = Timer(const Duration(milliseconds: 500), () {
      _runSearch();
    });
  }

  /// 검색어 + 선택된 중/소분류를 조합해 검색을 실행한다.
  void _runSearch() {
    final categoryState =
        widget.showCategoryFilter ? ref.read(productAddProvider) : null;
    ref.read(addProductProvider.notifier).searchProducts(
          query: _searchController.text,
          categoryMid: categoryState?.selectedMiddle,
          categorySub: categoryState?.selectedSub,
        );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(addProductProvider);
    final notifier = ref.read(addProductProvider.notifier);

    return Column(
      children: [
        if (widget.showCategoryFilter) _buildCategoryFilter(),
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
                        _runSearch();
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

  /// 중분류 / 소분류 필터 행 (전산매출 등 분류검색용).
  Widget _buildCategoryFilter() {
    final categoryState = ref.watch(productAddProvider);
    final categoryNotifier = ref.read(productAddProvider.notifier);
    final middles = categoryState.categories.map((c) => c.middle).toList();
    final subs = categoryState.subsForSelectedMiddle;

    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: _CategoryFilterField(
                title: '중분류 선택',
                hint: '중분류 전체',
                searchHint: '분류 검색',
                value: categoryState.selectedMiddle,
                items: middles,
                onChanged: (v) {
                  categoryNotifier.selectMiddle(v);
                  _runSearch();
                },
              ),
            ),
            Container(width: 1, height: 52, color: AppColors.divider),
            Expanded(
              child: _CategoryFilterField(
                title: '소분류 선택',
                hint: '소분류 전체',
                value: categoryState.selectedSub,
                items: subs,
                onChanged: (v) {
                  categoryNotifier.selectSub(v);
                  _runSearch();
                },
              ),
            ),
          ],
        ),
        const Divider(height: 1, color: AppColors.divider),
      ],
    );
  }

  Widget _buildSearchResults(dynamic state, dynamic notifier) {
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    // 바코드 필수 화면(POS/전산 매출조회)에서는 바코드 없는 제품을 목록에서
    // 제외한다(레거시 productMapper `is not null` 정합 — 노출 후 차단이 아님).
    final List results = widget.requireBarcode
        ? (state.searchResults as List)
            .where((p) => (p.barcode as String).trim().isNotEmpty)
            .toList()
        : state.searchResults as List;

    // 검색어와 분류가 모두 없을 때만 "검색하세요" 안내를 노출한다.
    // 분류만 선택해도 검색이 실행되므로, 그 결과가 0건이면 "검색 결과 없음"으로 안내한다.
    final categoryState =
        widget.showCategoryFilter ? ref.read(productAddProvider) : null;
    final hasCategoryFilter = (categoryState?.selectedMiddle?.isNotEmpty ?? false) ||
        (categoryState?.selectedSub?.isNotEmpty ?? false);

    if (results.isEmpty && state.searchQuery.isEmpty && !hasCategoryFilter) {
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

    if (results.isEmpty) {
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
      itemCount: results.length,
      itemBuilder: (context, index) {
        final product = results[index];
        return ProductCardForAdd(
          product: product,
          isSelected: state.isProductSelected(product.productCode),
          blockExclusive: widget.blockExclusive,
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

/// 분류 필터 — 탭하면 바텀시트(SingleSelectSheet)로 단일 선택.
///
/// 첫 항목에 "전체"(value=null) sentinel 을 두어 필터 해제를 지원한다.
class _CategoryFilterField extends StatelessWidget {
  final String title;
  final String hint;
  final String? searchHint;
  final String? value;
  final List<String> items;
  final ValueChanged<String?> onChanged;

  const _CategoryFilterField({
    required this.title,
    required this.hint,
    required this.value,
    required this.items,
    required this.onChanged,
    this.searchHint,
  });

  Future<void> _open(BuildContext context) async {
    final result = await SingleSelectSheet.show<String?>(
      context,
      title: title,
      selectedValue: value,
      searchHint: searchHint,
      options: [
        SingleSelectOption<String?>(value: null, label: hint),
        ...items.map((e) => SingleSelectOption<String?>(value: e, label: e)),
      ],
    );
    if (result == null) return;
    onChanged(result.value);
  }

  @override
  Widget build(BuildContext context) {
    final hasValue = value != null;
    return InkWell(
      onTap: () => _open(context),
      child: Container(
        height: 52,
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
        alignment: Alignment.center,
        child: Row(
          children: [
            Expanded(
              child: Text(
                value ?? hint,
                overflow: TextOverflow.ellipsis,
                style: AppTypography.bodyMedium.copyWith(
                  color:
                      hasValue ? AppColors.textPrimary : AppColors.textTertiary,
                ),
              ),
            ),
            const Icon(
              Icons.keyboard_arrow_down,
              size: 22,
              color: AppColors.textSecondary,
            ),
          ],
        ),
      ),
    );
  }
}
