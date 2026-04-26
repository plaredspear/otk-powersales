import '../entities/education_category.dart';
import '../entities/education_post.dart';
import '../entities/education_post_detail.dart';

/// 교육 Repository 인터페이스
///
/// 교육 게시물 조회 기능을 제공한다.
abstract class EducationRepository {
  /// 카테고리별 교육 게시물 목록을 조회한다.
  ///
  /// [category]: 교육 카테고리
  /// [search]: 검색 키워드 (타이틀+내용, 선택)
  /// [page]: 페이지 번호 (1부터 시작, 기본값 1)
  /// [size]: 페이지 크기 (기본값 10)
  ///
  /// Returns: 페이지네이션 정보를 포함한 게시물 목록
  Future<EducationPostPage> getPosts({
    required EducationCategory category,
    String? search,
    int page = 1,
    int size = 10,
  });

  /// 교육 게시물 상세를 조회한다.
  ///
  /// [postId]: 게시물 ID
  ///
  /// Returns: 게시물 상세 정보 (본문, 이미지, 첨부파일 포함)
  Future<EducationPostDetail> getPostDetail(int postId);
}
