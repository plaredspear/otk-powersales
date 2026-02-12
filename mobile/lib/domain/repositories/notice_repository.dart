import '../entities/notice_category.dart';
import '../entities/notice_post.dart';
import '../entities/notice_post_detail.dart';

/// 공지사항 Repository 인터페이스
///
/// 공지사항 목록 조회 및 상세 조회 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class NoticeRepository {
  /// 공지사항 목록 조회 (페이지네이션)
  ///
  /// [category]: 분류 필터 (null이면 전체)
  /// [search]: 검색 키워드 (타이틀 + 내용, null이면 검색 안함)
  /// [page]: 페이지 번호 (1부터 시작, 기본값 1)
  /// [size]: 페이지 크기 (기본값 10)
  ///
  /// Returns: 페이지네이션된 공지사항 목록
  Future<NoticePostPage> getPosts({
    NoticeCategory? category,
    String? search,
    int page = 1,
    int size = 10,
  });

  /// 공지사항 상세 조회
  ///
  /// [noticeId]: 공지사항 ID
  ///
  /// Returns: 공지사항 상세 정보 (본문, 이미지 포함)
  Future<NoticePostDetail> getPostDetail(int noticeId);
}
