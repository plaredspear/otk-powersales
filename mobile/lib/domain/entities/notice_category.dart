/// 공지사항 분류 enum
///
/// 공지사항의 분류를 나타내는 열거형입니다.
/// - company: 회사공지
/// - branch: 지점공지
enum NoticeCategory {
  /// 회사공지
  company,

  /// 지점공지
  branch;

  /// 분류 표시명 반환
  String get displayName {
    switch (this) {
      case NoticeCategory.company:
        return '회사공지';
      case NoticeCategory.branch:
        return '지점공지';
    }
  }

  /// API 요청용 분류 코드 반환
  String get code {
    switch (this) {
      case NoticeCategory.company:
        return 'COMPANY';
      case NoticeCategory.branch:
        return 'BRANCH';
    }
  }

  /// API 응답 코드로부터 NoticeCategory 생성
  static NoticeCategory fromCode(String code) {
    switch (code.toUpperCase()) {
      case 'COMPANY':
        return NoticeCategory.company;
      case 'BRANCH':
        return NoticeCategory.branch;
      default:
        throw ArgumentError('Unknown notice category code: $code');
    }
  }
}
