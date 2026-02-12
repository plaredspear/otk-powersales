import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/notice_api_datasource.dart';
import '../../data/repositories/notice_repository_impl.dart';
import '../../domain/entities/notice_category.dart';
import '../../domain/repositories/notice_repository.dart';
import '../../domain/usecases/get_notice_posts_usecase.dart';
import 'notice_list_state.dart';

// --- Dependency Providers ---

/// Dio HTTP Client Provider
final dioProvider = Provider<Dio>((ref) {
  return Dio(BaseOptions(
    baseUrl: 'https://api.example.com', // TODO: 실제 API URL로 변경
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 10),
  ));
});

/// Notice Repository Provider
final noticeRepositoryProvider = Provider<NoticeRepository>((ref) {
  final dio = ref.watch(dioProvider);
  final dataSource = NoticeApiDataSource(dio);
  return NoticeRepositoryImpl(remoteDataSource: dataSource);
});

/// GetNoticePosts UseCase Provider
final getNoticePostsUseCaseProvider = Provider<GetNoticePostsUseCase>((ref) {
  final repository = ref.watch(noticeRepositoryProvider);
  return GetNoticePostsUseCase(repository);
});

// --- NoticeListNotifier ---

/// 공지사항 목록 상태 관리 Notifier
///
/// 분류 필터, 검색, 페이지네이션 기능을 관리합니다.
class NoticeListNotifier extends StateNotifier<NoticeListState> {
  final GetNoticePostsUseCase _getNoticePosts;

  NoticeListNotifier({
    required GetNoticePostsUseCase getNoticePosts,
  })  : _getNoticePosts = getNoticePosts,
        super(NoticeListState.initial());

  /// 초기 데이터 로딩
  Future<void> initialize() async {
    await loadPosts();
  }

  /// 공지사항 목록 조회
  Future<void> loadPosts({int? page}) async {
    try {
      state = state.toLoading();

      final targetPage = page ?? state.currentPage;

      final result = await _getNoticePosts.call(
        category: state.selectedCategory,
        search: state.searchKeyword,
        page: targetPage,
        size: state.pageSize,
      );

      state = state.copyWith(
        isLoading: false,
        posts: result.content,
        totalCount: result.totalCount,
        totalPages: result.totalPages,
        currentPage: result.currentPage,
        hasSearched: true,
      );
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// 분류 필터 변경
  Future<void> setCategory(NoticeCategory? category) async {
    state = state.copyWith(
      selectedCategory: category,
      currentPage: 1, // 페이지 리셋
    );
    await loadPosts(page: 1);
  }

  /// 분류 필터 초기화
  Future<void> clearCategoryFilter() async {
    state = state.copyWith(
      clearCategoryFilter: true,
      currentPage: 1,
    );
    await loadPosts(page: 1);
  }

  /// 검색 키워드 설정 및 검색
  Future<void> search(String keyword) async {
    state = state.copyWith(
      searchKeyword: keyword.trim().isEmpty ? null : keyword.trim(),
      currentPage: 1, // 페이지 리셋
    );
    await loadPosts(page: 1);
  }

  /// 검색 초기화
  Future<void> clearSearch() async {
    state = state.copyWith(
      clearSearchKeyword: true,
      currentPage: 1,
    );
    await loadPosts(page: 1);
  }

  /// 다음 페이지 로딩
  Future<void> loadNextPage() async {
    if (!state.hasNextPage || state.isLoading) return;
    await loadPosts(page: state.currentPage + 1);
  }

  /// 이전 페이지 로딩
  Future<void> loadPreviousPage() async {
    if (!state.hasPreviousPage || state.isLoading) return;
    await loadPosts(page: state.currentPage - 1);
  }

  /// 특정 페이지로 이동
  Future<void> goToPage(int page) async {
    if (page < 1 || page > state.totalPages || state.isLoading) return;
    await loadPosts(page: page);
  }

  /// 처음 페이지로 이동
  Future<void> goToFirstPage() async {
    if (state.currentPage == 1 || state.isLoading) return;
    await loadPosts(page: 1);
  }

  /// 마지막 페이지로 이동
  Future<void> goToLastPage() async {
    if (state.currentPage == state.totalPages || state.isLoading) return;
    await loadPosts(page: state.totalPages);
  }

  /// 새로고침
  Future<void> refresh() async {
    await loadPosts(page: state.currentPage);
  }
}

/// Notice List Provider
final noticeListProvider =
    StateNotifierProvider<NoticeListNotifier, NoticeListState>((ref) {
  final getNoticePosts = ref.watch(getNoticePostsUseCaseProvider);
  return NoticeListNotifier(getNoticePosts: getNoticePosts);
});
