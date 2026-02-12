import '../entities/education_post_detail.dart';
import '../repositories/education_repository.dart';

/// 교육 게시물 상세 조회 UseCase
///
/// 교육 게시물의 상세 정보를 조회한다. 본문, 이미지, 첨부파일을 포함한다.
class GetEducationPostDetailUseCase {
  final EducationRepository _repository;

  GetEducationPostDetailUseCase(this._repository);

  /// 교육 게시물 상세를 조회한다.
  ///
  /// [postId]: 게시물 ID
  ///
  /// Returns: 게시물 상세 정보 (본문, 이미지, 첨부파일 포함)
  Future<EducationPostDetail> call(int postId) async {
    // 게시물 ID 유효성 검증
    if (postId <= 0) {
      throw ArgumentError('Post ID must be greater than 0');
    }

    return await _repository.getPostDetail(postId);
  }
}
