import '../entities/expiry_alert.dart';
import '../entities/notice.dart';
import '../entities/schedule.dart';

/// 홈 데이터 조회 결과를 담는 값 객체
class HomeData {
  final List<Schedule> todaySchedules;
  final ExpiryAlert? expiryAlert;
  final List<Notice> notices;
  final String currentDate;

  const HomeData({
    required this.todaySchedules,
    this.expiryAlert,
    required this.notices,
    required this.currentDate,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! HomeData) return false;

    if (todaySchedules.length != other.todaySchedules.length) return false;
    for (var i = 0; i < todaySchedules.length; i++) {
      if (todaySchedules[i] != other.todaySchedules[i]) return false;
    }

    if (notices.length != other.notices.length) return false;
    for (var i = 0; i < notices.length; i++) {
      if (notices[i] != other.notices[i]) return false;
    }

    return expiryAlert == other.expiryAlert &&
        currentDate == other.currentDate;
  }

  @override
  int get hashCode {
    return Object.hash(
      Object.hashAll(todaySchedules),
      expiryAlert,
      Object.hashAll(notices),
      currentDate,
    );
  }

  @override
  String toString() {
    return 'HomeData(todaySchedules: $todaySchedules, expiryAlert: $expiryAlert, notices: $notices, currentDate: $currentDate)';
  }
}

/// Home Repository 인터페이스
abstract class HomeRepository {
  /// 홈 화면에 필요한 통합 데이터를 조회한다.
  ///
  /// 오늘 일정, 유통기한 알림, 공지사항을 한 번에 조회.
  Future<HomeData> getHomeData();
}
