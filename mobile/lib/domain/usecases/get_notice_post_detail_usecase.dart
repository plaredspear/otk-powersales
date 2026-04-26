import '../entities/notice_post_detail.dart';
import '../repositories/notice_repository.dart';

/// 공지사항 상세 조회 UseCase
///
/// 공지사항 ID로 상세 정보를 조회합니다.
class GetNoticePostDetailUseCase {
  final NoticeRepository _repository;

  GetNoticePostDetailUseCase(this._repository);

  /// 공지사항 상세 조회 실행
  ///
  /// [noticeId]: 공지사항 ID
  ///
  /// Returns: 공지사항 상세 정보 (본문, 이미지 포함)
  ///
  /// Throws:
  /// - [ArgumentError] 공지사항 ID가 유효하지 않은 경우
  Future<NoticePostDetail> call(int noticeId) async {
    // ID 검증
    if (noticeId <= 0) {
      throw ArgumentError('공지사항 ID는 양수여야 합니다');
    }

    // Repository에서 공지사항 상세 조회
    return await _repository.getPostDetail(noticeId);
  }
}
