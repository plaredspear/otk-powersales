import '../models/notice_post_model.dart';
import '../models/notice_post_detail_model.dart';

/// 공지사항 원격 데이터소스 인터페이스
///
/// API 서버와의 공지사항 관련 통신을 추상화합니다.
abstract class NoticeRemoteDataSource {
  /// 공지사항 목록 조회 API 호출
  ///
  /// GET /api/v1/notices
  Future<NoticePostPageModel> getPosts({
    String? category,
    String? search,
    int page = 1,
    int size = 10,
  });

  /// 공지사항 상세 조회 API 호출
  ///
  /// GET /api/v1/notices/{noticeId}
  Future<NoticePostDetailModel> getPostDetail(int noticeId);
}
