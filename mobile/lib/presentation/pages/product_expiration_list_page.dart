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

/// 유통기한 관리 메인 페이지
///
/// 거래처/기간 필터로 유통기한 목록을 조회하고,
/// "유통기한 지남" / "유통기한 전" 그룹으로 표시합니다.
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
        title: const Text('유통기한 관리'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
      ),
      body: Column(
        children: [
          // 검색 필터
          ProductExpirationFilterBar(
            accounts: state.accounts,
            selectedAccountCode: state.selectedAccountCode,
            isAccountsLoading: state.isAccountsLoading,
            fromDate: state.fromDate,
            toDate: state.toDate,
            onAccountChanged: (code, name) {
              ref.read(productExpirationListProvider.notifier).selectAccount(code, name);
            },
            onFromDateChanged: (date) {
              ref.read(productExpirationListProvider.notifier).updateFromDate(date);
            },
            onToDateChanged: (date) {
              ref.read(productExpirationListProvider.notifier).updateToDate(date);
            },
            onSearch: () {
              ref.read(productExpirationListProvider.notifier).searchProductExpiration();
            },
          ),

          // 결과 헤더 (전체 개수 + 삭제 버튼)
          if (state.hasSearched) _buildResultHeader(state),

          // 목록
          Expanded(child: _buildBody(state)),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _onRegisterTap,
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.onPrimary,
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildResultHeader(ProductExpirationListState state) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        children: [
          Text(
            '전체 (${state.totalCount})',
            style: AppTypography.headlineSmall,
          ),
          const Spacer(),
          if (state.hasResults)
            TextButton.icon(
              onPressed: _onDeleteTap,
              icon: const Icon(Icons.close, size: 16),
              label: const Text('삭제'),
              style: TextButton.styleFrom(
                foregroundColor: AppColors.error,
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

    // 검색 결과 없음
    if (state.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.inbox_outlined,
                size: 64, color: AppColors.textTertiary),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '조회된 유통기한이 없습니다',
              style: AppTypography.bodyLarge
                  .copyWith(color: AppColors.textSecondary),
            ),
          ],
        ),
      );
    }

    // 검색 전 초기 상태
    if (!state.hasSearched) {
      return const SizedBox.shrink();
    }

    // 그룹별 목록
    return ListView(
      children: [
        // 유통기한 지남 그룹
        if (state.expiredItems.isNotEmpty) ...[
          ProductExpirationGroupHeader(
            isExpired: true,
            count: state.expiredItems.length,
          ),
          ...state.expiredItems.map((item) => ProductExpirationProductCard(
                item: item,
                onTap: () => _onItemTap(item),
              )),
          const SizedBox(height: AppSpacing.md),
        ],

        // 유통기한 전 그룹
        if (state.activeItems.isNotEmpty) ...[
          ProductExpirationGroupHeader(
            isExpired: false,
            count: state.activeItems.length,
          ),
          ...state.activeItems.map((item) => ProductExpirationProductCard(
                item: item,
                onTap: () => _onItemTap(item),
              )),
        ],

        // 하단 여백 (FAB과 겹치지 않도록)
        const SizedBox(height: 80),
      ],
    );
  }
}
