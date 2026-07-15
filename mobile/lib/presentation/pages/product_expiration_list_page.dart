import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/product_expiration_item.dart';
import '../providers/product_expiration_list_provider.dart';
import '../providers/product_expiration_list_state.dart';
import '../widgets/product_expiration/product_expiration_filter_bar.dart';
import '../widgets/product_expiration/product_expiration_group_header.dart';
import '../widgets/product_expiration/product_expiration_product_card.dart';

/// 소비기한 관리 메인 페이지
///
/// 거래처/기간 필터로 소비기한 목록을 조회하고,
/// "소비기한 지남" / "소비기한 전" 그룹으로 표시합니다.
class ProductExpirationListPage extends ConsumerStatefulWidget {
  const ProductExpirationListPage({super.key});

  @override
  ConsumerState<ProductExpirationListPage> createState() => _ProductExpirationListPageState();
}

class _ProductExpirationListPageState extends ConsumerState<ProductExpirationListPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(productExpirationListProvider.notifier).initialize();
    });
  }

  void _onItemTap(ProductExpirationItem item) {
    AppRouter.navigateTo(
      context,
      AppRouter.productExpirationEdit,
      arguments: item,
    ).then((_) {
      // 수정 후 복귀 시 목록 새로고침
      _refreshList();
    });
  }

  void _onDeleteTap() {
    final state = ref.read(productExpirationListProvider);
    AppRouter.navigateTo(
      context,
      AppRouter.productExpirationDelete,
      arguments: state.items,
    ).then((_) {
      // 삭제 후 복귀 시 목록 새로고침
      _refreshList();
    });
  }

  void _onRegisterTap() {
    AppRouter.navigateTo(
      context,
      AppRouter.productExpirationRegister,
    ).then((_) {
      // 등록 후 복귀 시 목록 새로고침
      _refreshList();
    });
  }

  void _refreshList() {
    ref.read(productExpirationListProvider.notifier).searchProductExpiration();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(productExpirationListProvider);

    // 에러 리스닝
    ref.listen(productExpirationListProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
        ref.read(productExpirationListProvider.notifier).clearError();
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('소비기한 관리'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
      ),
      body: Column(
        children: [
          // 검색 필터
          ProductExpirationFilterBar(
            selectedAccountName: state.selectedAccountName,
            fromDate: state.fromDate,
            toDate: state.toDate,
            // 필터 변경 시 별도 검색 버튼 없이 즉시 조회한다.
            // select/update 는 동기적으로 state 를 갱신하므로,
            // 이어지는 searchProductExpiration 이 방금 바뀐 조건으로 fetch 한다.
            onAccountChanged: (code, name) {
              final notifier =
                  ref.read(productExpirationListProvider.notifier);
              notifier.selectAccount(code, name);
              notifier.searchProductExpiration();
            },
            onDateRangeChanged: (from, to) {
              final notifier =
                  ref.read(productExpirationListProvider.notifier);
              notifier.updateFromDate(from);
              notifier.updateToDate(to);
              notifier.searchProductExpiration();
            },
          ),

          // 검색 영역 하단 구분 띠 (레거시 bline)
          _band(),

          // 목록
          Expanded(child: _buildBody(state)),
        ],
      ),
      // 제품 추가 (레거시 btn_add_product — 주황 원형 "+제품")
      floatingActionButton: FloatingActionButton(
        onPressed: _onRegisterTap,
        backgroundColor: AppColors.warning,
        foregroundColor: AppColors.white,
        shape: const CircleBorder(),
        child: const Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.add, size: 18),
            Text(
              '제품',
              style: TextStyle(
                  fontSize: 10, fontWeight: FontWeight.w700, height: 1.1),
            ),
          ],
        ),
      ),
    );
  }

  /// 레거시 bline (높이 10px, #F0F0F0 구분 띠)
  Widget _band({bool marginTop = false}) {
    return Container(
      margin: marginTop ? const EdgeInsets.only(top: AppSpacing.md) : EdgeInsets.zero,
      height: 10,
      color: AppColors.surfaceVariant,
    );
  }

  /// 레거시 sect_top — "전체 (N)" (N 빨강) + 삭제
  Widget _buildSectTop(ProductExpirationListState state) {
    return Container(
      height: 56,
      padding: const EdgeInsets.symmetric(horizontal: 20),
      alignment: Alignment.center,
      child: Row(
        children: [
          Text.rich(
            TextSpan(
              children: [
                const TextSpan(text: '전체 '),
                TextSpan(
                  text: '(${state.totalCount})',
                  style: const TextStyle(color: AppColors.legacyDanger),
                ),
              ],
              style: AppTypography.headlineSmall,
            ),
          ),
          const Spacer(),
          if (state.hasResults)
            TextButton.icon(
              onPressed: _onDeleteTap,
              icon: const Icon(Icons.delete_outline, size: 16),
              label: const Text('삭제'),
              style: TextButton.styleFrom(
                foregroundColor: AppColors.textSecondary,
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildBody(ProductExpirationListState state) {
    // 로딩 상태
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    // 에러 상태 (데이터 없음)
    if (state.errorMessage != null && !state.hasResults) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline,
                size: 64, color: AppColors.textTertiary),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '오류가 발생했습니다',
              style: AppTypography.bodyLarge
                  .copyWith(color: AppColors.textSecondary),
            ),
            const SizedBox(height: AppSpacing.lg),
            ElevatedButton(
              onPressed: () {
                ref.read(productExpirationListProvider.notifier).searchProductExpiration();
              },
              child: const Text('재시도'),
            ),
          ],
        ),
      );
    }

    // 검색 전 초기 상태
    if (!state.hasSearched) {
      return const SizedBox.shrink();
    }

    // 레거시 정합: 검색 후에는 전체 카운트와 두 그룹 헤더(지남/전)를 항상 표시.
    // 항목이 0개여도 헤더는 (0)으로 노출된다.
    return ListView(
      children: [
        // 전체 (N)
        _buildSectTop(state),
        _band(),

        // 소비기한 지남 그룹
        ProductExpirationGroupHeader(
          isExpired: true,
          count: state.expiredItems.length,
        ),
        ...state.expiredItems.map((item) => ProductExpirationProductCard(
              item: item,
              onTap: () => _onItemTap(item),
            )),

        // 그룹 구분 띠 (레거시 bline mt20)
        _band(marginTop: true),

        // 소비기한 전 그룹
        ProductExpirationGroupHeader(
          isExpired: false,
          count: state.activeItems.length,
        ),
        ...state.activeItems.map((item) => ProductExpirationProductCard(
              item: item,
              onTap: () => _onItemTap(item),
            )),

        // 하단 여백 (FAB과 겹치지 않도록)
        const SizedBox(height: 80),
      ],
    );
  }
}
