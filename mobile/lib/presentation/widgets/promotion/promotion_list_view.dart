import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/throttled_tap_mixin.dart';
import '../../../domain/entities/my_account.dart';
import '../../providers/auth_provider.dart';
import '../../providers/promotion_list_provider.dart';
import '../../providers/promotion_list_state.dart';
import '../common/loading_indicator.dart';
import 'promotion_card.dart';

/// 행사 목록 본문 위젯 (AppBar 없는 임베드 가능 형태).
///
/// `PromotionListPage`(독립 화면) 와 `SalesOverviewScreen` 의 행사 매출 탭에서
/// 공통으로 사용한다.
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

  Future<void> _pickRangeDate(BuildContext context, bool isStart) async {
    final state = ref.read(promotionListProvider);
    final picked =
        await _showPicker(context, isStart ? state.startDate : state.endDate);
    if (picked != null) {
      final notifier = ref.read(promotionListProvider.notifier);
      if (isStart) {
        notifier.updateDateRange(picked, state.endDate);
      } else {
        notifier.updateDateRange(state.startDate, picked);
      }
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
    return '${picked.year}-${picked.month.toString().padLeft(2, '0')}-${picked.day.toString().padLeft(2, '0')}';
  }

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

  /// 거래처 전체 드롭다운 (레거시 `#accountCode` select).
  Widget _buildAccountDropdown(PromotionListState state) {
    final accountsAsync = ref.watch(promotionAccountOptionsProvider);
    final accounts = accountsAsync.asData?.value ?? const <MyAccount>[];

    return Padding(
      padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg, vertical: AppSpacing.xs),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<int?>(
          value: state.accountId,
          isExpanded: true,
          icon: const Icon(Icons.keyboard_arrow_down,
              color: AppColors.textSecondary),
          style: AppTypography.bodyLarge.copyWith(color: AppColors.textPrimary),
          items: [
            const DropdownMenuItem<int?>(
              value: null,
              child: Text('거래처 전체'),
            ),
            ...accounts.map(
              (a) => DropdownMenuItem<int?>(
                value: a.accountId,
                child: Text(
                  '${a.accountName}(${a.accountCode})',
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ),
          ],
          onChanged: (value) {
            ref.read(promotionListProvider.notifier).updateAccount(value);
            ref.read(promotionListProvider.notifier).searchPromotions();
          },
        ),
      ),
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
          const SizedBox(height: AppSpacing.sm),
          Row(
            children: [
              Text('기간  ',
                  style: AppTypography.bodyMedium
                      .copyWith(color: AppColors.textSecondary)),
              _buildDateButton(state.startDate,
                  () => throttledTap(() => _pickRangeDate(context, true))),
              Text(' ~ ',
                  style: AppTypography.bodyMedium
                      .copyWith(color: AppColors.textSecondary)),
              _buildDateButton(state.endDate,
                  () => throttledTap(() => _pickRangeDate(context, false))),
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
