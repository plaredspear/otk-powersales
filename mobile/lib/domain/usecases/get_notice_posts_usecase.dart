import '../entities/notice_category.dart';
import '../entities/notice_post.dart';
import '../repositories/notice_repository.dart';

/// 공지사항 목록 조회 UseCase
///
/// 분류별, 검색 키워드별로 공지사항 목록을 페이지네이션하여 조회합니다.
class GetNoticePostsUseCase {
  final NoticeRepository _repository;

  GetNoticePostsUseCase(this._repository);

  /// 공지사항 목록 조회 실행
  ///
  /// [category]: 분류 필터 (null이면 전체)
  /// [search]: 검색 키워드 (타이틀 + 내용, null이면 검색 안함)
  /// [page]: 페이지 번호 (1부터 시작, 기본값 1)
  /// [size]: 페이지 크기 (기본값 10)
  ///
  /// Returns: 페이지네이션된 공지사항 목록
  ///
  /// Throws:
  /// - [ArgumentError] 페이지 번호나 크기가 유효하지 않은 경우
  Future<NoticePostPage> call({
    NoticeCategory? category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    // 페이지 번호 검증
    if (page < 1) {
      throw ArgumentError('페이지 번호는 1 이상이어야 합니다');
    }

    // 페이지 크기 검증
    if (size < 1 || size > 100) {
      throw ArgumentError('페이지 크기는 1~100 사이여야 합니다');
    }

    // 검색 키워드 trim (빈 문자열은 null로 변환)
    final trimmedSearch = search?.trim();
    final effectiveSearch = (trimmedSearch == null || trimmedSearch.isEmpty)
        ? null
        : trimmedSearch;

    // Repository에서 공지사항 목록 조회
    return await _repository.getPosts(
      category: category,
      search: effectiveSearch,
      page: page,
      size: size,
    );
  }
}
