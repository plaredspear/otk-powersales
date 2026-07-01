import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/throttled_tap_mixin.dart';
import '../providers/claim_list_provider.dart';
import '../widgets/account/account_selector_sheet.dart';
import '../widgets/claim/claim_list_item_card.dart';
import '../widgets/common/date_range_filter_field.dart';
import '../widgets/common/loading_indicator.dart';

/// 클레임 현황 목록 페이지
class ClaimListPage extends ConsumerStatefulWidget {
  const ClaimListPage({super.key});

  @override
  ConsumerState<ClaimListPage> createState() => _ClaimListPageState();
}

class _ClaimListPageState extends ConsumerState<ClaimListPage>
    with ThrottledTapMixin {
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(claimListProvider.notifier).loadClaims();
    });
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  /// 목록 하단 근처에 도달하면 다음 페이지(20건)를 추가로 노출한다(클라이언트 페이징).
  void _onScroll() {
    if (!_scrollController.hasClients) return;
    final position = _scrollController.position;
    if (position.pixels >= position.maxScrollExtent - 200) {
      ref.read(claimListProvider.notifier).loadMore();
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(claimListProvider);

    ref.listen<String?>(
      claimListProvider.select((s) => s.errorMessage),
      (prev, next) {
        if (next != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(next)),
          );
          ref.read(claimListProvider.notifier).clearError();
        }
      },
    );

    return Scaffold(
      appBar: AppBar(title: const Text('제품 클레임 조회')),
      body: Column(
        children: [
          _buildAccountFilter(state),
          _buildDateFilter(state),
          Expanded(child: _buildBody(state)),
        ],
      ),
    );
  }

  Widget _buildAccountFilter(dynamic state) {
    final hasAccount = state.selectedAccountId != null;
    return Padding(
      padding: const EdgeInsets.fromLTRB(
          AppSpacing.lg, AppSpacing.lg, AppSpacing.lg, 0),
      child: InkWell(
        onTap: () => throttledTap(_selectAccount),
        borderRadius: BorderRadius.circular(8),
        child: Container(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.sm,
            vertical: AppSpacing.sm,
          ),
          decoration: BoxDecoration(
            border: Border.all(color: AppColors.border),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Row(
            children: [
              const Icon(Icons.store_outlined,
                  size: 18, color: AppColors.textSecondary),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: Text(
                  hasAccount ? state.selectedAccountName! : '거래처 전체',
                  style: AppTypography.bodySmall.copyWith(
                    color: hasAccount
                        ? AppColors.textPrimary
                        : AppColors.textSecondary,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              if (hasAccount)
                GestureDetector(
                  onTap: () =>
                      ref.read(claimListProvider.notifier).clearAccount(),
                  child: const Icon(Icons.close,
                      size: 18, color: AppColors.textSecondary),
                )
              else
                const Icon(Icons.arrow_drop_down,
                    color: AppColors.textSecondary),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _selectAccount() async {
    // 클레임 목록은 레거시 `claim/list.jsp` 의 "거래처 전체" 옵션이 있는 필터형 화면.
    final account =
        await AccountSelectorSheet.show(context, includeAllOption: true);
    if (account == null) return;
    if (AccountSelectorSheet.isAllOption(account)) {
      ref.read(claimListProvider.notifier).clearAccount();
      return;
    }
    ref
        .read(claimListProvider.notifier)
        .selectAccount(account.accountId, account.accountName);
  }

  Widget _buildDateFilter(dynamic state) {
    return Padding(
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: Row(
        children: [
          Expanded(
            // 주문 현황 납기일과 동일한 인라인 기간 UI.
            // 레거시(claim/list.jsp): minDate/maxDate 없음, maxSpan 7일.
            child: DateRangeFilterField(
              label: '기간',
              startDate: state.startDate,
              endDate: state.endDate,
              maxRangeDays: 7,
              onChanged: (start, end) => ref
                  .read(claimListProvider.notifier)
                  .updateDateRange(start, end),
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          SizedBox(
            height: DateRangeFilterField.fieldHeight,
            child: ElevatedButton(
              onPressed: () => throttledTapAsync(
                () => ref.read(claimListProvider.notifier).loadClaims(),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
                minimumSize: const Size(48, DateRangeFilterField.fieldHeight),
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
              child: const Icon(Icons.search, size: 20),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBody(dynamic state) {
    if (state.isLoading) {
      return const LoadingIndicator(message: '클레임 목록을 불러오는 중...');
    }

    if (state.isEmpty) {
      return Center(
        child: Text(
          '결과가 없습니다',
          style: AppTypography.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
      );
    }

    if (!state.hasSearched) {
      return const SizedBox.shrink();
    }

    final visibleItems = state.visibleItems;
    return ListView.builder(
      controller: _scrollController,
      itemCount: visibleItems.length + (state.hasMore ? 1 : 0),
      itemBuilder: (context, index) {
        if (index >= visibleItems.length) {
          return _buildLoadMoreFooter(state);
        }
        final item = visibleItems[index];
        return ClaimListItemCard(
          item: item,
          onTap: () => throttledTap(
            () => AppRouter.navigateTo(
              context,
              AppRouter.claimDetail,
              arguments: item.claimId,
            ),
          ),
        );
      },
    );
  }

  /// 다음 페이지가 남아있을 때 하단에 노출하는 '더보기' 푸터.
  /// 스크롤로 자동 로드되지만, 탭으로도 다음 20건을 즉시 표시할 수 있다.
  Widget _buildLoadMoreFooter(dynamic state) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      child: Center(
        child: TextButton(
          onPressed: () => ref.read(claimListProvider.notifier).loadMore(),
          child: Text(
            '더보기 (${state.visibleItems.length}/${state.items.length})',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ),
      ),
    );
  }

}
