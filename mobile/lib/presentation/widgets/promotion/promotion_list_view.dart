import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/throttled_tap_mixin.dart';
import '../../../domain/repositories/my_account_repository.dart';
import '../../providers/auth_provider.dart';
import '../../providers/promotion_list_provider.dart';
import '../../providers/promotion_list_state.dart';
import '../account/account_selector_field.dart';
import '../common/date_range_filter_field.dart';
import '../common/loading_indicator.dart';
import 'promotion_card.dart';

/// 행사 목록 본문 위젯 (AppBar 없는 임베드 가능 형태).
///
/// `PromotionListPage`(독립 화면) 에서 사용한다.
///
/// 레거시(heroku `promotion/event/list.jsp`) 정합:
/// - 거래처 전체 드롭다운 + 기간 + 검색 버튼 (버튼 트리거 검색)
/// - 권한 분기: 여사원(member)은 단일 날짜, 조장/지점장(leader)은 기간 범위 + 행사명 검색어
/// - "내 행사 현황 (N)" 카운트 헤더
class PromotionListView extends ConsumerStatefulWidget {
  const PromotionListView({super.key});

  @override
  ConsumerState<PromotionListView> createState() => _PromotionListViewState();
}

class _PromotionListViewState extends ConsumerState<PromotionListView>
    with ThrottledTapMixin {
  final _searchController = TextEditingController();
  final _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(promotionListProvider.notifier).initialize();
    });
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _searchController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  /// 현재 사용자가 조장/지점장(leader) 권한인지 여부.
  ///
  /// 레거시는 `appauthority__c` 가 `조장`(group_leader) 이면 기간 범위 + 행사명 검색,
  /// `여사원`(group_member) 이면 단일 날짜 + 검색 버튼만 노출한다.
  /// 모바일 도메인 role 은 `여사원→USER`, `조장→LEADER`, `지점장→ADMIN` 로 번역된다.
  bool get _isLeader {
    final role = ref.read(authProvider).user?.role;
    return role == 'LEADER' || role == 'ADMIN';
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      ref.read(promotionListProvider.notifier).loadNextPage();
    }
  }

  /// 검색 실행 (레거시: 검색 버튼 클릭 시에만 조회).
  void _onSearch() {
    final notifier = ref.read(promotionListProvider.notifier);
    if (_isLeader) {
      notifier.updateKeyword(_searchController.text.trim());
    }
    notifier.searchPromotions();
  }

  Future<void> _onRefresh() async {
    await ref.read(promotionListProvider.notifier).searchPromotions();
  }

  Future<void> _pickSingleDate(BuildContext context) async {
    final state = ref.read(promotionListProvider);
    final picked = await _showPicker(context, state.startDate);
    if (picked != null) {
      ref.read(promotionListProvider.notifier).updateSingleDate(picked);
    }
  }

  Future<String?> _showPicker(BuildContext context, String current) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: DateTime.parse(current),
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );
    if (picked == null) return null;
    return _fmtDate(picked);
  }

  String _fmtDate(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(promotionListProvider);

    ref.listen<String?>(
      promotionListProvider.select((s) => s.errorMessage),
      (prev, next) {
        if (next != null) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text(next)));
          ref.read(promotionListProvider.notifier).clearError();
        }
      },
    );

    return Column(
      children: [
        _buildAccountDropdown(state),
        const Divider(height: 1, color: AppColors.border),
        if (_isLeader) _buildLeaderFilter(state) else _buildMemberFilter(state),
        const Divider(height: 1, thickness: 8, color: AppColors.surfaceVariant),
        _buildCountHeader(state),
        const Divider(height: 1, color: AppColors.border),
        Expanded(child: _buildBody(state)),
      ],
    );
  }

  /// 거래처 선택 (레거시 `#accountCode` select) — 공용 [AccountSelectorField] 바텀시트 재사용.
  /// 목록 필터형이므로 "거래처 전체"(필터 해제) 항목/버튼을 노출한다.
  Widget _buildAccountDropdown(PromotionListState state) {
    return AccountSelectorField(
      selectedName: state.accountName,
      placeholder: '거래처 전체',
      scope: MyAccountScope.field,
      padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg, vertical: AppSpacing.md),
      onSelected: (account) {
        ref
            .read(promotionListProvider.notifier)
            .updateAccount(account.accountId, account.accountName);
        ref.read(promotionListProvider.notifier).searchPromotions();
      },
      onCleared: () {
        ref.read(promotionListProvider.notifier).updateAccount(null, null);
        ref.read(promotionListProvider.notifier).searchPromotions();
      },
    );
  }

  /// 여사원: 기간(단일 날짜, 기본 오늘) + 검색 버튼.
  Widget _buildMemberFilter(PromotionListState state) {
    return Padding(
      padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
      child: Row(
        children: [
          Text('기간  ',
              style: AppTypography.bodyMedium
                  .copyWith(color: AppColors.textSecondary)),
          _buildDateButton(
              state.startDate, () => throttledTap(() => _pickSingleDate(context))),
          const Spacer(),
          _buildSearchButton(),
        ],
      ),
    );
  }

  /// 조장/지점장: 행사명 검색어 + 검색 버튼 + 기간 범위.
  Widget _buildLeaderFilter(PromotionListState state) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
          AppSpacing.lg, AppSpacing.sm, AppSpacing.lg, AppSpacing.sm),
      child: Column(
        children: [
          // 주문 현황 납기일과 동일한 인라인 기간 UI.
          // 레거시(promotion/event/list.jsp): minDate/maxDate 없음, maxSpan 30일.
          DateRangeFilterField(
            label: '기간',
            startDate: DateTime.parse(state.startDate),
            endDate: DateTime.parse(state.endDate),
            maxRangeDays: 30,
            onChanged: (start, end) => ref
                .read(promotionListProvider.notifier)
                .updateDateRange(_fmtDate(start), _fmtDate(end)),
          ),
          const SizedBox(height: AppSpacing.sm),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _searchController,
                  textInputAction: TextInputAction.search,
                  onSubmitted: (_) => _onSearch(),
                  decoration: InputDecoration(
                    hintText: '행사명 입력',
                    hintStyle: AppTypography.bodyMedium
                        .copyWith(color: AppColors.textTertiary),
                    isDense: true,
                    filled: true,
                    fillColor: AppColors.surfaceVariant,
                    border: OutlineInputBorder(
                      borderRadius: AppSpacing.inputBorderRadius,
                      borderSide: BorderSide.none,
                    ),
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: AppSpacing.lg,
                      vertical: AppSpacing.md,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              _buildSearchButton(),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildDateButton(String date, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.xs,
        ),
        decoration: BoxDecoration(
          border: Border.all(color: AppColors.border),
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
        ),
        child: Text(
          date,
          style:
              AppTypography.bodyMedium.copyWith(color: AppColors.textPrimary),
        ),
      ),
    );
  }

  /// 노란색 검색 버튼 (레거시 `#btn_search`).
  Widget _buildSearchButton() {
    return ElevatedButton(
      onPressed: () => throttledTap(_onSearch),
      style: ElevatedButton.styleFrom(
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.onPrimary,
        elevation: 0,
        // 전역 테마의 minimumSize 가 Size.fromHeight(=너비 무한) 이라
        // Row(Expanded 형제) 측정 시 무한 너비 제약을 받아 크래시가 난다.
        // 검색 버튼은 콘텐츠 폭으로 들어가도록 minimumSize 를 유한값으로 덮어쓴다.
        minimumSize: const Size(0, AppSpacing.buttonHeight),
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.sm,
        ),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusXl),
        ),
      ),
      child: Text('검색',
          style: AppTypography.labelLarge
              .copyWith(color: AppColors.onPrimary)),
    );
  }

  /// "내 행사 현황 (N)" 카운트 헤더 (레거시 `.sect_top`).
  Widget _buildCountHeader(PromotionListState state) {
    return Padding(
      padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg, vertical: AppSpacing.md),
      child: Row(
        children: [
          Text('내 행사 현황 ',
              style: AppTypography.bodyLarge
                  .copyWith(fontWeight: FontWeight.w600)),
          Text(
            '(${state.totalElements})',
            style: AppTypography.bodyLarge.copyWith(
              fontWeight: FontWeight.w600,
              color: AppColors.otokiRed,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBody(PromotionListState state) {
    if (state.isLoading && !state.hasSearched) {
      return const LoadingIndicator(message: '행사를 불러오는 중...');
    }

    if (state.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.event_busy,
                size: 64, color: AppColors.textTertiary),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '조회된 행사가 없습니다',
              style: AppTypography.bodyMedium
                  .copyWith(color: AppColors.textSecondary),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _onRefresh,
      child: ListView.builder(
        controller: _scrollController,
        itemCount: state.items.length + (state.isLoadingMore ? 1 : 0),
        itemBuilder: (context, index) {
          if (index >= state.items.length) {
            return const Padding(
              padding: EdgeInsets.all(AppSpacing.lg),
              child: Center(child: CircularProgressIndicator()),
            );
          }
          final item = state.items[index];
          return PromotionCard(
            item: item,
            onTap: () => throttledTap(
              () => AppRouter.navigateTo(
                context,
                AppRouter.promotionDetail,
                arguments: item.id,
              ),
            ),
          );
        },
      ),
    );
  }
}
