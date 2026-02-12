import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/domain/entities/education_post.dart';

/// 교육 자료 목록 화면 상태
class EducationPostsState {
  final EducationCategory category;
  final String? searchKeyword;
  final int currentPage;
  final EducationPostPage? postPage;
  final bool isLoading;
  final String? errorMessage;

  const EducationPostsState({
    required this.category,
    this.searchKeyword,
    required this.currentPage,
    this.postPage,
    required this.isLoading,
    this.errorMessage,
  });

  /// 초기 상태
  factory EducationPostsState.initial() {
    return const EducationPostsState(
      category: EducationCategory.tastingManual,
      searchKeyword: null,
      currentPage: 1,
      postPage: null,
      isLoading: false,
      errorMessage: null,
    );
  }

  /// 로딩 상태로 전환
  EducationPostsState toLoading() {
    return EducationPostsState(
      category: category,
      searchKeyword: searchKeyword,
      currentPage: currentPage,
      postPage: postPage,
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 데이터 로드 완료 상태로 전환
  EducationPostsState toData(EducationPostPage postPage) {
    return EducationPostsState(
      category: category,
      searchKeyword: searchKeyword,
      currentPage: currentPage,
      postPage: postPage,
      isLoading: false,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  EducationPostsState toError(String message) {
    return EducationPostsState(
      category: category,
      searchKeyword: searchKeyword,
      currentPage: currentPage,
      postPage: postPage,
      isLoading: false,
      errorMessage: message,
    );
  }

  /// 카테고리 변경
  EducationPostsState withCategory(EducationCategory newCategory) {
    return EducationPostsState(
      category: newCategory,
      searchKeyword: null, // 카테고리 변경 시 검색어 초기화
      currentPage: 1, // 첫 페이지로 리셋
      postPage: null,
      isLoading: false,
      errorMessage: null,
    );
  }

  /// 검색어 변경
  EducationPostsState withSearchKeyword(String? keyword) {
    return EducationPostsState(
      category: category,
      searchKeyword: keyword,
      currentPage: 1, // 검색 시 첫 페이지로 리셋
      postPage: null,
      isLoading: false,
      errorMessage: null,
    );
  }

  /// 페이지 변경
  EducationPostsState withPage(int page) {
    return EducationPostsState(
      category: category,
      searchKeyword: searchKeyword,
      currentPage: page,
      postPage: postPage,
      isLoading: false,
      errorMessage: null,
    );
  }

  /// 데이터 로드 완료 여부
  bool get isLoaded => postPage != null && !isLoading && errorMessage == null;

  /// 에러 발생 여부
  bool get isError => errorMessage != null;

  /// 게시물 목록 (빈 목록 반환 보장)
  List<EducationPost> get posts => postPage?.content ?? [];

  /// 전체 게시물 수
  int get totalCount => postPage?.totalCount ?? 0;

  /// 전체 페이지 수
  int get totalPages => postPage?.totalPages ?? 0;

  /// 마지막 페이지 여부
  bool get isLastPage => postPage?.isLastPage ?? false;

  /// 첫 페이지 여부
  bool get isFirstPage => postPage?.isFirstPage ?? true;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is EducationPostsState &&
          runtimeType == other.runtimeType &&
          category == other.category &&
          searchKeyword == other.searchKeyword &&
          currentPage == other.currentPage &&
          postPage == other.postPage &&
          isLoading == other.isLoading &&
          errorMessage == other.errorMessage;

  @override
  int get hashCode =>
      category.hashCode ^
      searchKeyword.hashCode ^
      currentPage.hashCode ^
      postPage.hashCode ^
      isLoading.hashCode ^
      errorMessage.hashCode;
}
