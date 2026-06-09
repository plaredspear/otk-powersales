import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/throttled_tap_mixin.dart';
import '../providers/product_search_provider.dart';
import '../providers/product_search_state.dart';
import '../screens/barcode_scanner_screen.dart';
import '../widgets/product_search/empty_search_guide.dart';
import '../widgets/product_search/product_card.dart';
import '../widgets/product_search/product_search_app_bar.dart';

/// 제품검색 화면
///
/// 검색어를 입력해 검색하고, 결과를 같은 화면에 그대로 표시한다.
/// (별도 결과 화면으로 전환하지 않는다 — 레거시 단일 화면 UX와 동일.)
class ProductSearchPage extends ConsumerStatefulWidget {
  /// 제품 선택 모드 — 카드 탭 시 선택한 제품을 호출부로 반환(pop)한다.
  /// 클레임/점검 등록 폼의 "제품 선택"에서 진입한 경우 true.
  final bool selectionMode;

  const ProductSearchPage({super.key, this.selectionMode = false});

  @override
  ConsumerState<ProductSearchPage> createState() => _ProductSearchPageState();
}

class _ProductSearchPageState extends ConsumerState<ProductSearchPage>
    with ThrottledTapMixin {
  final TextEditingController _searchController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    // 이전 검색어가 있으면 복원
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final state = ref.read(productSearchProvider);
      if (state.query.isNotEmpty) {
        _searchController.text = state.query;
      }
    });
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  /// 스크롤 감지 → 무한 스크롤 페이지네이션
  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      ref.read(productSearchProvider.notifier).loadNextPage();
    }
  }

  /// 검색 실행 — 결과는 같은 화면에 표시한다 (화면 전환 없음).
  Future<void> _onSearch() async {
    FocusScope.of(context).unfocus();
    await ref.read(productSearchProvider.notifier).search();
  }

  /// 바코드 스캔 — 카메라로 스캔한 바코드로 제품을 검색한다(검색 결과는 같은 화면에 표시).
  Future<void> _onBarcodeTap() async {
    final barcode = await BarcodeScannerScreen.show(context);
    if (barcode == null || !mounted) return;
    FocusScope.of(context).unfocus();
    await ref.read(productSearchProvider.notifier).searchByBarcode(barcode);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(productSearchProvider);
    final notifier = ref.read(productSearchProvider.notifier);

    // 에러 메시지 리스닝
    ref.listen(productSearchProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            duration: const Duration(seconds: 2),
          ),
        );
        notifier.clearError();
      }
    });

    final canSearch = state.canSearch;

    return Scaffold(
      appBar: ProductSearchAppBar(
        controller: _searchController,
        onBack: () {
          notifier.clearSearch();
          AppRouter.goBack(context);
        },
        onChanged: (value) => notifier.updateQuery(value),
        onSearch: canSearch ? _onSearch : null,
      ),
      body: _buildBody(state),
    );
  }

  Widget _buildBody(ProductSearchState state) {
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    // 검색 전 안내 / 검색 후 결과 없음
    if (!state.hasSearched) {
      return EmptySearchGuide(hasSearched: false, onBarcodeTap: _onBarcodeTap);
    }
    if (state.isEmpty) {
      return const EmptySearchGuide(hasSearched: true);
    }

    // 검색 결과
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 결과 건수 표시
        Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.lg,
            vertical: AppSpacing.sm,
          ),
          child: Text(
            '제품 (${state.totalElements})',
            style: AppTypography.labelLarge.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ),
        const Divider(height: 1, color: AppColors.divider),
        Expanded(child: _buildResultList(state)),
      ],
    );
  }

  Widget _buildResultList(ProductSearchState state) {
    return ListView.builder(
      controller: _scrollController,
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
      itemCount: state.products.length + (state.isLoadingMore ? 1 : 0),
      itemBuilder: (context, index) {
        // 로딩 인디케이터 (마지막 아이템)
        if (index == state.products.length) {
          return const Padding(
            padding: EdgeInsets.all(AppSpacing.md),
            child: Center(child: CircularProgressIndicator()),
          );
        }

        final product = state.products[index];
        return ProductCard(
          key: ValueKey(product.productCode),
          product: product,
          showActions: !widget.selectionMode,
          onTap: () => throttledTap(() {
            // 선택 모드: 고른 제품을 호출부로 반환
            if (widget.selectionMode) {
              Navigator.of(context).pop(product);
              return;
            }
            AppRouter.navigateTo(
              context,
              AppRouter.productDetail,
              arguments: product.productCode,
            );
          }),
          onClaimTap: widget.selectionMode
              ? null
              : () => throttledTap(
                  () => AppRouter.navigateTo(
                    context,
                    AppRouter.claimRegister,
                    arguments: (
                      productCode: product.productCode,
                      productName: product.productName,
                    ),
                  ),
                ),
          onOrderTap: widget.selectionMode
              ? null
              : () => throttledTap(
                  () => AppRouter.navigateTo(
                    context,
                    AppRouter.orderForm,
                    arguments: product.productCode,
                  ),
                ),
        );
      },
    );
  }
}
