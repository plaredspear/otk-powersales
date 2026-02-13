import 'package:dio/dio.dart';
import '../models/daily_schedule_info_model.dart';
import '../models/monthly_schedule_day_model.dart';
import 'my_schedule_remote_datasource.dart';

/// 마이페이지 일정 API 데이터소스 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API와 통신합니다.
class MyScheduleApiDataSource implements MyScheduleRemoteDataSource {
  final Dio _dio;

  MyScheduleApiDataSource(this._dio);

  @override
  Future<List<MonthlyScheduleDayModel>> getMonthlySchedule(
    int year,
    int month,
  ) async {
    final response = await _dio.get(
      '/api/v1/mypage/schedule/monthly',
      queryParameters: {
        'year': year,
        'month': month,
      },
    );

    final data = response.data['data'] as List<dynamic>;
    return data
        .map((json) =>
            MonthlyScheduleDayModel.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<DailyScheduleInfoModel> getDailySchedule(String date) async {
    final response = await _dio.get(
      '/api/v1/mypage/schedule/daily',
      queryParameters: {
        'date': date,
      },
    );

    return DailyScheduleInfoModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
