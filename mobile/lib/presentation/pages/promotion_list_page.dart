import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../providers/promotion_list_provider.dart';
import '../providers/promotion_list_state.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/promotion/promotion_card.dart';

/// 행사 목록 페이지
class PromotionListPage extends ConsumerStatefulWidget {
  const PromotionListPage({super.key});

  @override
  ConsumerState<PromotionListPage> createState() => _PromotionListPageState();
}

class _PromotionListPageState extends ConsumerState<PromotionListPage> {
  final _searchController = TextEditingController();
  final _scrollController = ScrollController();
  Timer? _debounceTimer;

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
    _debounceTimer?.cancel();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      ref.read(promotionListProvider.notifier).loadNextPage();
    }
  }

  void _onSearchChanged(String value) {
    _debounceTimer?.cancel();
    _debounceTimer = Timer(const Duration(milliseconds: 500), () {
      ref.read(promotionListProvider.notifier).updateKeyword(value.trim());
      ref.read(promotionListProvider.notifier).searchPromotions();
    });
  }

  Future<void> _onRefresh() async {
    await ref.read(promotionListProvider.notifier).searchPromotions();
  }

  Future<void> _pickDate(BuildContext context, bool isStart) async {
    final state = ref.read(promotionListProvider);
    final initial = DateTime.parse(isStart ? state.startDate : state.endDate);
    final picked = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );
    if (picked != null) {
      final formatted =
          '${picked.year}-${picked.month.toString().padLeft(2, '0')}-${picked.day.toString().padLeft(2, '0')}';
      final notifier = ref.read(promotionListProvider.notifier);
      if (isStart) {
        notifier.updateDateRange(formatted, state.endDate);
      } else {
        notifier.updateDateRange(state.startDate, formatted);
      }
      notifier.searchPromotions();
    }
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

    return Scaffold(
      appBar: AppBar(
        title: const Text('행사 현황'),
      ),
      body: Column(
        children: [
          _buildSearchBar(),
          _buildDateFilter(state),
          Expanded(child: _buildBody(state)),
        ],
      ),
    );
  }

  Widget _buildSearchBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
          AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.sm),
      child: TextField(
        controller: _searchController,
        onChanged: _onSearchChanged,
        decoration: InputDecoration(
          hintText: '행사명/거래처 검색',
          hintStyle:
              AppTypography.bodyMedium.copyWith(color: AppColors.textTertiary),
          prefixIcon:
              const Icon(Icons.search, color: AppColors.textTertiary),
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
    );
  }

  Widget _buildDateFilter(PromotionListState state) {
    return Padding(
      padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg, vertical: AppSpacing.xs),
      child: Row(
        children: [
          Text('기간: ',
              style: AppTypography.bodySmall
                  .copyWith(color: AppColors.textSecondary)),
          _buildDateButton(state.startDate, () => _pickDate(context, true)),
          Text(' ~ ',
              style: AppTypography.bodySmall
                  .copyWith(color: AppColors.textSecondary)),
          _buildDateButton(state.endDate, () => _pickDate(context, false)),
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
          horizontal: AppSpacing.sm,
          vertical: AppSpacing.xs,
        ),
        decoration: BoxDecoration(
          border: Border.all(color: AppColors.border),
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
        ),
        child: Text(
          date,
          style:
              AppTypography.bodySmall.copyWith(color: AppColors.textPrimary),
        ),
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
            Icon(Icons.event_busy, size: 64, color: Colors.grey[300]),
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
            onTap: () => AppRouter.navigateTo(
              context,
              AppRouter.promotionDetail,
              arguments: item.id,
            ),
          );
        },
      ),
    );
  }
}
