import '../../../domain/entities/education_category.dart';
import '../../../domain/entities/education_post.dart';
import '../../../domain/entities/education_post_detail.dart';
import '../../../domain/repositories/education_repository.dart';
import '../../mock/education_mock_data.dart';

/// Education Mock Repository
///
/// Backend API가 준비되기 전까지 Mock 데이터로 동작하는 Repository.
class EducationMockRepository implements EducationRepository {
  /// Mock 데이터 커스텀 (테스트용)
  Map<EducationCategory, List<EducationPost>>? customPosts;
  Map<int, EducationPostDetail>? customPostDetails;
  Exception? exceptionToThrow;

  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 500));
  }

  @override
  Future<EducationPostPage> getPosts({
    required EducationCategory category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    // 카테고리별 게시물 조회
    final allPosts = customPosts?[category] ?? EducationMockData.getPostsByCategory(category);

    // 검색 필터링
    List<EducationPost> filteredPosts = allPosts;
    if (search != null && search.isNotEmpty) {
      filteredPosts = allPosts
          .where((post) => post.title.toLowerCase().contains(search.toLowerCase()))
          .toList();
    }

    // 페이지네이션 계산
    final totalCount = filteredPosts.length;
    final totalPages = (totalCount / size).ceil();
    final startIndex = (page - 1) * size;
    final endIndex = (startIndex + size).clamp(0, totalCount);

    // 페이지 범위 유효성 검사
    if (startIndex >= totalCount && totalCount > 0) {
      return EducationPostPage(
        content: [],
        totalCount: totalCount,
        totalPages: totalPages,
        currentPage: page,
        size: size,
      );
    }

    final content = filteredPosts.sublist(
      startIndex,
      endIndex,
    );

    return EducationPostPage(
      content: content,
      totalCount: totalCount,
      totalPages: totalPages,
      currentPage: page,
      size: size,
    );
  }

  @override
  Future<EducationPostDetail> getPostDetail(int postId) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    // 커스텀 데이터 또는 Mock 데이터에서 조회
    final postDetail = customPostDetails?[postId] ?? EducationMockData.getPostDetailById(postId);

    if (postDetail == null) {
      throw Exception('POST_NOT_FOUND: Post with ID $postId not found');
    }

    return postDetail;
  }
}
