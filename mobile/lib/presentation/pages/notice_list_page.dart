import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';

import '../../app_router.dart';
import '../providers/notice_list_provider.dart';
import '../providers/notice_list_state.dart';
import '../widgets/notice/notice_category_filter.dart';
import '../widgets/notice/notice_pagination.dart';
import '../widgets/notice/notice_post_item.dart';
import '../widgets/notice/notice_search_bar.dart';

/// 공지사항 목록 화면
///
/// 분류 필터, 검색, 페이지네이션이 포함된 공지사항 목록을 표시합니다.
class NoticeListPage extends ConsumerStatefulWidget {
  const NoticeListPage({super.key});

  @override
  ConsumerState<NoticeListPage> createState() => _NoticeListPageState();
}

class _NoticeListPageState extends ConsumerState<NoticeListPage> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    // 초기 데이터 로딩
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(noticeListProvider.notifier).initialize();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(noticeListProvider);
    final notifier = ref.read(noticeListProvider.notifier);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text.rich(
          TextSpan(
            children: [
              const TextSpan(text: '공지사항'),
              if (state.hasSearched)
                TextSpan(
                  text: ' (${state.totalCount})',
                  style: const TextStyle(color: AppColors.otokiRed),
                ),
            ],
          ),
        ),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: Column(
        children: [
          // 분류 필터 (전체 너비)
          NoticeCategoryFilter(
            selectedCategory: state.selectedCategory,
            onCategoryChanged: (category) {
              notifier.setCategory(category);
            },
          ),

          // 검색 바 (전체 너비)
          NoticeSearchBar(
            controller: _searchController,
            onSearch: (keyword) {
              notifier.search(keyword);
            },
          ),

          const Divider(height: 1, color: AppColors.divider),

          // 목록 영역
          Expanded(
            child: _buildContent(state, notifier),
          ),

          // 페이지네이션
          if (state.hasResults)
            NoticePagination(
              currentPage: state.currentPage,
              totalPages: state.totalPages,
              totalCount: state.totalCount,
              onFirstPage: () => notifier.goToFirstPage(),
              onPreviousPage: () => notifier.loadPreviousPage(),
              onNextPage: () => notifier.loadNextPage(),
              onLastPage: () => notifier.goToLastPage(),
            ),
        ],
      ),
    );
  }

  Widget _buildContent(NoticeListState state, NoticeListNotifier notifier) {
    // 로딩 중
    if (state.isLoading) {
      return const Center(
        child: CircularProgressIndicator(),
      );
    }

    // 에러
    if (state.errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.error_outline,
              size: 48,
              color: AppColors.error,
            ),
            const SizedBox(height: AppSpacing.md),
            Text(
              state.errorMessage!,
              style: const TextStyle(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            ElevatedButton(
              onPressed: () => notifier.refresh(),
              child: const Text('다시 시도'),
            ),
          ],
        ),
      );
    }

    // 빈 목록 (당겨서 새로고침 가능하도록 스크롤 가능 영역으로 구성)
    if (state.isEmpty) {
      return RefreshIndicator(
        onRefresh: () => notifier.refresh(),
        child: LayoutBuilder(
          builder: (context, constraints) {
            return SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              child: ConstrainedBox(
                constraints: BoxConstraints(minHeight: constraints.maxHeight),
                child: const Center(
                  child: Text(
                    '공지사항이 없습니다',
                    style: TextStyle(
                      color: AppColors.textSecondary,
                    ),
                  ),
                ),
              ),
            );
          },
        ),
      );
    }

    // 목록 표시
    return RefreshIndicator(
      onRefresh: () => notifier.refresh(),
      child: ListView.builder(
        physics: const AlwaysScrollableScrollPhysics(),
        itemCount: state.posts.length,
        itemBuilder: (context, index) {
          final post = state.posts[index];
          return NoticePostItem(
            post: post,
            onTap: () {
              AppRouter.navigateTo(
                context,
                AppRouter.noticeDetail,
                arguments: post.id,
              );
            },
          );
        },
      ),
    );
  }
}
