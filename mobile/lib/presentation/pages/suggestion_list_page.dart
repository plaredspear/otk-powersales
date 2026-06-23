import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/throttled_tap_mixin.dart';
import '../../domain/entities/suggestion_form.dart';
import '../../domain/entities/suggestion_list_item.dart';
import '../providers/suggestion_list_provider.dart';
import '../widgets/account/account_selector_sheet.dart';
import '../widgets/common/date_range_filter_field.dart';
import '../widgets/common/error_view.dart';
import '../widgets/common/loading_indicator.dart';

/// 내 물류클레임 조회 화면
///
/// 레거시 `fieldTalk/suggest/logisticsclaimlist.jsp` 대응(물류클레임 전용 목록).
/// 레거시에는 제안/물류클레임 통합 목록이 없으므로 LOGISTICS_CLAIM 만 조회한다.
/// 백엔드 `GET /api/v1/mobile/suggestions` 를 소비한다.
class SuggestionListPage extends ConsumerStatefulWidget {
  const SuggestionListPage({super.key});

  @override
  ConsumerState<SuggestionListPage> createState() => _SuggestionListPageState();
}

class _SuggestionListPageState extends ConsumerState<SuggestionListPage>
    with ThrottledTapMixin {
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    // 검색조건을 첫 build 전에 동기 초기화한다(레거시 정합: 거래처 전체 + 등록일 범위 기본 최근 30일).
    // 전역 provider 공유로 인한 이전 진입 필터 누수를 막고, 필터 UI 가 항상 유효한 범위를 갖게 한다.
    ref.read(suggestionListProvider.notifier).initFilters();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(suggestionListProvider.notifier)
          .load(category: SuggestionCategory.logisticsClaim.code);
    });
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_scrollController.hasClients) return;
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      ref.read(suggestionListProvider.notifier).loadNextPage();
    }
  }

  void _onItemTap(int id) {
    throttledTap(() {
      AppRouter.navigateTo(context, AppRouter.suggestionDetail, arguments: id);
    });
  }

  String get _title => '내 물류클레임 조회';

  String get _emptyMessage => '등록된 물류클레임이 없습니다';

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(suggestionListProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text(_title),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: Column(
        children: [
          // 레거시 logisticsclaimlist 는 진입 시 검색조건(거래처 + 등록일 범위)을 항상 노출한다.
          // 통합("내 제안/물류클레임")·전용 진입 모두 동일하게 필터를 노출해 레거시와 정합한다.
          _buildAccountFilter(state),
          _buildDateFilter(state),
          Expanded(child: _buildBody(state)),
        ],
      ),
    );
  }

  /// 거래처 필터 — 레거시 "거래처 전체" 옵션(필터 해제) 포함. 현장클레임 화면과 동일 패턴.
  Widget _buildAccountFilter(SuggestionListState state) {
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
            color: AppColors.white,
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
                      ref.read(suggestionListProvider.notifier).clearAccount(),
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
    final account =
        await AccountSelectorSheet.show(context, includeAllOption: true);
    if (account == null) return;
    final notifier = ref.read(suggestionListProvider.notifier);
    if (AccountSelectorSheet.isAllOption(account)) {
      notifier.clearAccount();
      return;
    }
    notifier.selectAccount(account.accountId, account.accountName);
  }

  /// 등록일 범위 필터 + 검색 버튼 — 레거시 maxSpan 30일 / 기본 최근 30일 정합.
  Widget _buildDateFilter(SuggestionListState state) {
    return Padding(
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: Row(
        children: [
          Expanded(
            // 레거시 daterangepicker: minDate/maxDate 없음, maxSpan 30일.
            child: DateRangeFilterField(
              label: '기간',
              // _isClaimOnly 진입 시 initState 에서 기본 범위를 동기 세팅하므로 항상 non-null.
              startDate: state.startDate!,
              endDate: state.endDate!,
              maxRangeDays: 30,
              onChanged: (start, end) => ref
                  .read(suggestionListProvider.notifier)
                  .updateDateRange(start, end),
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          SizedBox(
            height: DateRangeFilterField.fieldHeight,
            child: ElevatedButton(
              onPressed: () => throttledTapAsync(
                () => ref.read(suggestionListProvider.notifier).load(),
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

  Widget _buildBody(SuggestionListState state) {
    if (state.isLoading && state.items.isEmpty) {
      return const LoadingIndicator(message: '목록을 불러오는 중...');
    }

    if (state.errorMessage != null && state.items.isEmpty) {
      return ErrorView(
        message: '목록을 불러올 수 없습니다',
        description: state.errorMessage,
        onRetry: () => ref.read(suggestionListProvider.notifier).load(),
      );
    }

    if (state.isEmpty) {
      return RefreshIndicator(
        onRefresh: () => ref.read(suggestionListProvider.notifier).load(),
        child: ListView(
          children: [
            SizedBox(
              height: MediaQuery.of(context).size.height * 0.6,
              child: Center(
                child: Text(
                  _emptyMessage,
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: () => ref.read(suggestionListProvider.notifier).load(),
      color: AppColors.primary,
      child: ListView.builder(
        controller: _scrollController,
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
        itemCount: state.items.length + (state.isLoadingMore ? 1 : 0),
        itemBuilder: (context, index) {
          if (index == state.items.length) {
            return const Padding(
              padding: EdgeInsets.all(AppSpacing.md),
              child: Center(child: CircularProgressIndicator()),
            );
          }
          final item = state.items[index];
          return _SuggestionCard(item: item, onTap: () => _onItemTap(item.id));
        },
      ),
    );
  }
}

class _SuggestionCard extends StatelessWidget {
  final SuggestionListItem item;
  final VoidCallback onTap;

  const _SuggestionCard({required this.item, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final dateLabel =
        DateFormat('yyyy.MM.dd(E)', 'ko_KR').format(item.createdAt);

    return Container(
      margin: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.xs,
      ),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(color: AppColors.border, width: 1),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _CategoryBadge(item: item),
                  const SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: Text(
                      item.proposalNumber,
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textTertiary,
                      ),
                      overflow: TextOverflow.ellipsis,
                      textAlign: TextAlign.end,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: AppSpacing.sm),
              Text(
                item.title,
                style: AppTypography.bodyLarge.copyWith(
                  color: AppColors.textPrimary,
                  fontWeight: FontWeight.w700,
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: AppSpacing.xs),
              Text(
                dateLabel,
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textTertiary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CategoryBadge extends StatelessWidget {
  final SuggestionListItem item;

  const _CategoryBadge({required this.item});

  @override
  Widget build(BuildContext context) {
    final isClaim = item.isLogisticsClaim;
    final bgColor = isClaim ? AppColors.secondaryLight : AppColors.surface;
    final fgColor = isClaim ? AppColors.white : AppColors.textSecondary;

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      ),
      child: Text(
        item.categoryName,
        style: AppTypography.labelSmall.copyWith(
          color: fgColor,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}
