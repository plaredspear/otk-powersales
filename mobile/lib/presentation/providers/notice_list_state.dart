import '../../domain/entities/notice_category.dart';
import '../../domain/entities/notice_post.dart';

/// 공지사항 목록 화면 상태
///
/// 필터 조건(분류), 검색, 페이지네이션, 로딩/에러 상태를 포함합니다.
class NoticeListState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 공지사항 목록
  final List<NoticePost> posts;

  /// 전체 건수
  final int totalCount;

  /// 전체 페이지 수
  final int totalPages;

  /// 현재 페이지 번호 (1부터 시작)
  final int currentPage;

  /// 페이지 크기
  final int pageSize;

  /// 검색 실행 여부 (빈 결과 vs 초기 상태 구분)
  final bool hasSearched;

  // --- 필터 조건 ---

  /// 선택된 분류 (null이면 전체)
  final NoticeCategory? selectedCategory;

  /// 검색 키워드
  final String? searchKeyword;

  const NoticeListState({
    this.isLoading = false,
    this.errorMessage,
    this.posts = const [],
    this.totalCount = 0,
    this.totalPages = 0,
    this.currentPage = 1,
    this.pageSize = 10,
    this.hasSearched = false,
    this.selectedCategory,
    this.searchKeyword,
  });

  /// 초기 상태
  factory NoticeListState.initial() {
    return const NoticeListState();
  }

  /// 로딩 상태로 전환
  NoticeListState toLoading() {
    return copyWith(
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  NoticeListState toError(String message) {
    return copyWith(
      isLoading: false,
      errorMessage: message,
    );
  }

  /// 검색 결과가 있는지 여부
  bool get hasResults => posts.isNotEmpty;

  /// 검색 결과가 없는지 (검색 후)
  bool get isEmpty => hasSearched && posts.isEmpty;

  /// 다음 페이지가 있는지 여부
  bool get hasNextPage => currentPage < totalPages;

  /// 이전 페이지가 있는지 여부
  bool get hasPreviousPage => currentPage > 1;

  /// 필터가 적용되어 있는지 여부
  bool get hasActiveFilter =>
      selectedCategory != null || (searchKeyword != null && searchKeyword!.isNotEmpty);

  NoticeListState copyWith({
    bool? isLoading,
    String? errorMessage,
    List<NoticePost>? posts,
    int? totalCount,
    int? totalPages,
    int? currentPage,
    int? pageSize,
    bool? hasSearched,
    NoticeCategory? selectedCategory,
    String? searchKeyword,
    bool clearCategoryFilter = false,
    bool clearSearchKeyword = false,
  }) {
    return NoticeListState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      posts: posts ?? this.posts,
      totalCount: totalCount ?? this.totalCount,
      totalPages: totalPages ?? this.totalPages,
      currentPage: currentPage ?? this.currentPage,
      pageSize: pageSize ?? this.pageSize,
      hasSearched: hasSearched ?? this.hasSearched,
      selectedCategory: clearCategoryFilter
          ? null
          : (selectedCategory ?? this.selectedCategory),
      searchKeyword:
          clearSearchKeyword ? null : (searchKeyword ?? this.searchKeyword),
    );
  }
}
