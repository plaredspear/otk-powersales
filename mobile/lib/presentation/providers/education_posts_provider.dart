import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/data/repositories/mock/education_mock_repository.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/domain/repositories/education_repository.dart';
import 'package:mobile/domain/usecases/get_education_posts_usecase.dart';
import 'package:mobile/presentation/providers/education_posts_state.dart';

/// 교육 Repository Provider
final educationRepositoryProvider = Provider<EducationRepository>((ref) {
  return EducationMockRepository();
});

/// 교육 게시물 목록 조회 UseCase Provider
final getEducationPostsUseCaseProvider = Provider<GetEducationPostsUseCase>((ref) {
  final repository = ref.watch(educationRepositoryProvider);
  return GetEducationPostsUseCase(repository);
});

/// 교육 게시물 목록 Notifier
class EducationPostsNotifier extends StateNotifier<EducationPostsState> {
  EducationPostsNotifier(this._getEducationPosts)
      : super(EducationPostsState.initial()) {
    // 초기 로드
    fetchPosts();
  }

  final GetEducationPostsUseCase _getEducationPosts;

  /// 게시물 목록 조회
  Future<void> fetchPosts() async {
    state = state.toLoading();
    try {
      final postPage = await _getEducationPosts(
        category: state.category,
        search: state.searchKeyword,
        page: state.currentPage,
        size: 10,
      );
      state = state.toData(postPage);
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// 카테고리 선택
  Future<void> selectCategory(EducationCategory category) async {
    if (state.category == category) return;
    state = state.withCategory(category);
    await fetchPosts();
  }

  /// 검색
  Future<void> search(String? keyword) async {
    state = state.withSearchKeyword(keyword);
    await fetchPosts();
  }

  /// 검색어 초기화
  Future<void> clearSearch() async {
    await search(null);
  }

  /// 페이지 변경
  Future<void> changePage(int page) async {
    if (state.currentPage == page) return;
    state = state.withPage(page);
    await fetchPosts();
  }

  /// 다음 페이지
  Future<void> nextPage() async {
    if (state.isLastPage) return;
    await changePage(state.currentPage + 1);
  }

  /// 이전 페이지
  Future<void> previousPage() async {
    if (state.isFirstPage) return;
    await changePage(state.currentPage - 1);
  }

  /// 새로고침
  Future<void> refresh() async {
    await fetchPosts();
  }
}

/// 교육 게시물 목록 Provider
final educationPostsProvider =
    StateNotifierProvider<EducationPostsNotifier, EducationPostsState>((ref) {
  final useCase = ref.watch(getEducationPostsUseCaseProvider);
  return EducationPostsNotifier(useCase);
});
