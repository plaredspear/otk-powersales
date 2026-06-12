import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/throttled_tap_mixin.dart';
import '../../domain/entities/suggestion_list_item.dart';
import '../providers/suggestion_list_provider.dart';
import '../widgets/common/error_view.dart';
import '../widgets/common/loading_indicator.dart';

/// 내 제안/물류클레임 목록 화면
///
/// 레거시 `fieldTalk/suggest/logisticsclaimlist.jsp` 대응(통합 목록 + 카테고리 배지).
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
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(suggestionListProvider.notifier).load();
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

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(suggestionListProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('내 제안 / 물류클레임'),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: _buildBody(state),
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
                  '등록된 제안/물류클레임이 없습니다',
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
