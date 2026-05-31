import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/throttled_tap_mixin.dart';
import '../providers/product_search_provider.dart';
import '../providers/product_search_state.dart';
import '../widgets/menu/full_menu_drawer.dart';
import '../widgets/product_search/empty_search_guide.dart';
import '../widgets/product_search/product_card.dart';

/// 제품검색 결과 화면
///
/// 검색 결과 목록을 표시하는 화면입니다.
/// 무한 스크롤로 추가 결과를 로드하고,
/// 각 제품 카드에서 클레임/주문서 등록으로 이동할 수 있습니다.
class ProductSearchResultPage extends ConsumerStatefulWidget {
  /// 제품 선택 모드 — 카드 탭 시 선택한 제품을 호출부로 반환(pop)한다.
  /// 클레임/점검 등록 폼의 "제품 선택"에서 진입한 경우 true.
  final bool selectionMode;

  const ProductSearchResultPage({super.key, this.selectionMode = false});

  @override
  ConsumerState<ProductSearchResultPage> createState() =>
      _ProductSearchResultPageState();
}

class _ProductSearchResultPageState
    extends ConsumerState<ProductSearchResultPage>
    with ThrottledTapMixin {
  final ScrollController _scrollController = ScrollController();
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  /// 스크롤 감지 → 무한 스크롤 페이지네이션
  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      ref.read(productSearchProvider.notifier).loadNextPage();
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(productSearchProvider);

    return Scaffold(
      key: _scaffoldKey,
      endDrawer: const FullMenuDrawer(),
      appBar: AppBar(
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios, size: 20),
          onPressed: () => AppRouter.goBack(context),
        ),
        title: Text(state.query, style: AppTypography.headlineMedium),
      ),
      body: Column(
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

          // 구분선
          const Divider(height: 1, color: AppColors.divider),

          // 결과 리스트
          Expanded(child: _buildBody(state)),
        ],
      ),
      // 하단 네비게이션 버튼
      bottomNavigationBar: _buildBottomNav(context),
    );
  }

  Widget _buildBody(ProductSearchState state) {
    if (state.isEmpty) {
      return const EmptySearchGuide(hasSearched: true);
    }

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

  /// 하단 네비게이션 3버튼
  Widget _buildBottomNav(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.white,
        border: Border(top: BorderSide(color: AppColors.divider, width: 1)),
      ),
      child: SafeArea(
        child: Row(
          children: [
            // 뒤로 버튼
            Expanded(
              child: InkWell(
                onTap: () => AppRouter.goBack(context),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.arrow_back,
                        size: 22,
                        color: AppColors.textSecondary,
                      ),
                      const SizedBox(height: AppSpacing.xxs),
                      Text(
                        '뒤로',
                        style: AppTypography.labelSmall.copyWith(
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            // 홈으로 버튼
            Expanded(
              child: InkWell(
                onTap: () =>
                    AppRouter.navigateToAndRemoveAll(context, AppRouter.main),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.home_outlined,
                        size: 22,
                        color: AppColors.textSecondary,
                      ),
                      const SizedBox(height: AppSpacing.xxs),
                      Text(
                        '홈으로',
                        style: AppTypography.labelSmall.copyWith(
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            // 전체메뉴 버튼
            Expanded(
              child: InkWell(
                onTap: () => _scaffoldKey.currentState?.openEndDrawer(),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.menu,
                        size: 22,
                        color: AppColors.textSecondary,
                      ),
                      const SizedBox(height: AppSpacing.xxs),
                      Text(
                        '전체메뉴',
                        style: AppTypography.labelSmall.copyWith(
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
