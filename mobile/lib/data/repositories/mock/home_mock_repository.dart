import '../../../domain/entities/expiry_alert.dart';
import '../../../domain/entities/notice.dart';
import '../../../domain/entities/schedule.dart';
import '../../../domain/repositories/home_repository.dart';
import '../../mock/home_mock_data.dart';

/// Home Mock Repository
///
/// Backend API가 준비되기 전까지 Mock 데이터로 동작하는 Repository.
class HomeMockRepository implements HomeRepository {
  /// Mock 데이터 커스텀 (테스트용)
  List<Schedule>? customSchedules;
  ExpiryAlert? customExpiryAlert;
  bool useNullExpiryAlert = false;
  List<Notice>? customNotices;
  String? customCurrentDate;
  Exception? exceptionToThrow;

  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 500));
  }

  @override
  Future<HomeData> getHomeData() async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    final schedules = customSchedules ?? HomeMockData.todaySchedules;

    ExpiryAlert? expiryAlert;
    if (!useNullExpiryAlert) {
      expiryAlert = customExpiryAlert ?? HomeMockData.expiryAlert;
    }

    final notices = customNotices ?? HomeMockData.notices;
    final currentDate = customCurrentDate ?? HomeMockData.currentDate;

    return HomeData(
      todaySchedules: schedules,
      expiryAlert: expiryAlert,
      notices: notices,
      currentDate: currentDate,
    );
  }
}
