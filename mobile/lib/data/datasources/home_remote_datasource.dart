import '../models/expiry_alert_model.dart';
import '../models/notice_model.dart';
import '../models/schedule_model.dart';

/// 홈 API 응답 데이터를 담는 모델
class HomeResponseModel {
  final List<ScheduleModel> todaySchedules;
  final ExpiryAlertModel? expiryAlert;
  final List<NoticeModel> notices;
  final String currentDate;

  const HomeResponseModel({
    required this.todaySchedules,
    this.expiryAlert,
    required this.notices,
    required this.currentDate,
  });

  factory HomeResponseModel.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;

    final schedulesJson = data['today_schedules'] as List<dynamic>? ?? [];
    final todaySchedules = schedulesJson
        .map((e) => ScheduleModel.fromJson(e as Map<String, dynamic>))
        .toList();

    ExpiryAlertModel? expiryAlert;
    final expiryAlertJson = data['expiry_alert'] as Map<String, dynamic>?;
    if (expiryAlertJson != null) {
      expiryAlert = ExpiryAlertModel.fromJson(expiryAlertJson);
    }

    final noticesJson = data['notices'] as List<dynamic>? ?? [];
    final notices = noticesJson
        .map((e) => NoticeModel.fromJson(e as Map<String, dynamic>))
        .toList();

    final currentDate = data['current_date'] as String;

    return HomeResponseModel(
      todaySchedules: todaySchedules,
      expiryAlert: expiryAlert,
      notices: notices,
      currentDate: currentDate,
    );
  }
}

/// Home API DataSource 인터페이스
///
/// Backend API와 직접 통신하는 데이터 소스.
abstract class HomeRemoteDataSource {
  /// GET /api/v1/home
  Future<HomeResponseModel> getHomeData();
}
