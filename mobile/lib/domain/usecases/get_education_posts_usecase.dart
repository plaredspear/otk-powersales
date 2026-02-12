import '../entities/education_category.dart';
import '../entities/education_post.dart';
import '../repositories/education_repository.dart';

/// 교육 게시물 목록 조회 UseCase
///
/// 카테고리별 교육 게시물 목록을 조회한다. 검색과 페이지네이션을 지원한다.
class GetEducationPostsUseCase {
  final EducationRepository _repository;

  GetEducationPostsUseCase(this._repository);

  /// 교육 게시물 목록을 조회한다.
  ///
  /// [category]: 교육 카테고리
  /// [search]: 검색 키워드 (선택)
  /// [page]: 페이지 번호 (1부터, 기본값 1)
  /// [size]: 페이지 크기 (기본값 10)
  ///
  /// Returns: 페이지네이션 정보를 포함한 게시물 목록
  Future<EducationPostPage> call({
    required EducationCategory category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    // 페이지 번호 유효성 검증
    if (page < 1) {
      throw ArgumentError('Page number must be greater than 0');
    }

    // 페이지 크기 유효성 검증
    if (size < 1 || size > 100) {
      throw ArgumentError('Page size must be between 1 and 100');
    }

    return await _repository.getPosts(
      category: category,
      search: search,
      page: page,
      size: size,
    );
  }
}
