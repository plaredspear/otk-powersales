import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/presentation/providers/education_posts_provider.dart';
import 'package:mobile/presentation/providers/education_posts_state.dart';
import 'package:mobile/presentation/widgets/common/error_view.dart';
import 'package:mobile/presentation/widgets/common/loading_indicator.dart';
import 'package:mobile/presentation/widgets/education/education_post_item.dart';
import 'package:mobile/presentation/widgets/education/education_search_bar.dart';
import 'package:mobile/presentation/widgets/education/education_pagination.dart';

/// 교육 자료 목록 화면
///
/// 선택한 카테고리의 교육 자료 목록을 표시합니다.
/// 검색, 페이지네이션 기능을 제공합니다.
class EducationListPage extends ConsumerStatefulWidget {
  /// 카테고리 (optional - 없으면 provider의 현재 카테고리 사용)
  final EducationCategory? category;

  const EducationListPage({
    super.key,
    this.category,
  });

  @override
  ConsumerState<EducationListPage> createState() => _EducationListPageState();
}

class _EducationListPageState extends ConsumerState<EducationListPage> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    // 카테고리가 전달된 경우 해당 카테고리로 변경
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (widget.category != null) {
        ref.read(educationPostsProvider.notifier).selectCategory(widget.category!);
      }
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  /// 검색 실행
  void _onSearch(String keyword) {
    ref.read(educationPostsProvider.notifier).search(
          keyword.trim().isEmpty ? null : keyword.trim(),
        );
  }

  /// 게시물 탭 핸들러
  void _onPostTap(int postId) {
    // TODO: Router 연동 시 게시물 상세 화면으로 이동
    // context.push('/education/detail/$postId');
    debugPrint('Post tapped: $postId');
  }

  /// Pull-to-refresh 핸들러
  Future<void> _onRefresh() async {
    await ref.read(educationPostsProvider.notifier).refresh();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(educationPostsProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.otokiYellow,
        elevation: 0,
        title: Text(
          state.category.displayName,
          style: AppTypography.headlineLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(
            Icons.arrow_back,
            color: AppColors.textPrimary,
          ),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: _buildBody(state),
    );
  }

  /// 본문 영역
  Widget _buildBody(EducationPostsState state) {
    // 초기 로딩 중 (데이터 없음)
    if (state.postPage == null && state.isLoading) {
      return const LoadingIndicator(
        message: '게시물을 불러오는 중...',
      );
    }

    // 에러 상태 (데이터 없음)
    if (state.isError && state.postPage == null) {
      return ErrorView(
        message: '게시물을 불러올 수 없습니다',
        description: state.errorMessage,
        onRetry: () {
          ref.read(educationPostsProvider.notifier).refresh();
        },
      );
    }

    // 데이터 있음 → 화면 렌더링
    return Column(
      children: [
        // 검색 바
        Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: EducationSearchBar(
            controller: _searchController,
            onSearch: _onSearch,
            onChanged: (value) {
              setState(() {}); // suffixIcon 업데이트를 위해
            },
          ),
        ),

        // 게시물 목록
        Expanded(
          child: RefreshIndicator(
            onRefresh: _onRefresh,
            color: AppColors.primary,
            child: _buildPostList(state),
          ),
        ),

        // 페이지네이션
        if (state.postPage != null && state.totalPages > 0)
          EducationPagination(
            currentPage: state.currentPage,
            totalPages: state.totalPages,
            totalCount: state.totalCount,
            onPreviousPage: () {
              ref.read(educationPostsProvider.notifier).previousPage();
            },
            onNextPage: () {
              ref.read(educationPostsProvider.notifier).nextPage();
            },
          ),
      ],
    );
  }

  /// 게시물 목록 위젯
  Widget _buildPostList(EducationPostsState state) {
    // 로딩 오버레이
    if (state.isLoading) {
      return const Center(
        child: CircularProgressIndicator(
          color: AppColors.primary,
        ),
      );
    }

    // 게시물 없음
    if (state.posts.isEmpty) {
      return SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        child: SizedBox(
          height: MediaQuery.of(context).size.height * 0.5,
          child: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  Icons.search_off,
                  size: 64,
                  color: AppColors.textTertiary.withOpacity(0.5),
                ),
                const SizedBox(height: AppSpacing.md),
                Text(
                  '검색 결과가 없습니다',
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    // 게시물 목록
    return ListView.builder(
      physics: const AlwaysScrollableScrollPhysics(),
      itemCount: state.posts.length,
      itemBuilder: (context, index) {
        final post = state.posts[index];
        return EducationPostItem(
          post: post,
          onTap: () => _onPostTap(post.id),
        );
      },
    );
  }
}
