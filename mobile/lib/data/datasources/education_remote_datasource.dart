import '../models/education_post_detail_model.dart';
import '../models/education_post_model.dart';

/// 교육 원격 데이터소스 인터페이스
///
/// API 서버와의 교육 게시물 관련 통신을 추상화합니다.
abstract class EducationRemoteDataSource {
  /// 교육 게시물 목록 조회 API 호출
  ///
  /// GET /api/v1/mobile/education/posts
  ///
  /// [category]: 교육 카테고리 코드 (edu_code, 필수)
  /// [search]: 검색 키워드 (선택)
  /// [page]: 페이지 번호 (1부터 시작)
  /// [size]: 페이지 크기
  Future<EducationPostPageModel> getPosts({
    required String category,
    String? search,
    int page = 1,
    int size = 10,
  });

  /// 교육 게시물 상세 조회 API 호출
  ///
  /// GET /api/v1/mobile/education/posts/{postId}
  Future<EducationPostDetailModel> getPostDetail(String postId);
}
