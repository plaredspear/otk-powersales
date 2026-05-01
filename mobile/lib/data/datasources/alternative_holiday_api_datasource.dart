import 'package:dio/dio.dart';
import '../models/alternative_holiday_model.dart';

/// 대체휴무 API 데이터소스
class AlternativeHolidayApiDataSource {
  final Dio _dio;

  AlternativeHolidayApiDataSource(this._dio);

  /// 대체휴무 신청
  Future<AlternativeHolidayModel> createAlternativeHoliday({
    required String actualWorkDate,
    required String targetAltHolidayDate,
  }) async {
    final response = await _dio.post(
      '/api/v1/mobile/alternative-holidays',
      data: {
        'actual_work_date': actualWorkDate,
        'target_alt_holiday_date': targetAltHolidayDate,
      },
    );
    return AlternativeHolidayModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  /// 대체휴무 이력 조회
  Future<List<AlternativeHolidayModel>> getAlternativeHolidays({
    String? startDate,
    String? endDate,
  }) async {
    final queryParams = <String, dynamic>{};
    if (startDate != null) queryParams['startDate'] = startDate;
    if (endDate != null) queryParams['endDate'] = endDate;

    final response = await _dio.get(
      '/api/v1/mobile/alternative-holidays',
      queryParameters: queryParams,
    );

    final data = response.data['data'] as List<dynamic>;
    return data
        .map((json) =>
            AlternativeHolidayModel.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  /// 공휴일 목록 조회
  Future<List<String>> getHolidays(int year) async {
    final response = await _dio.get(
      '/api/v1/mobile/holidays',
      queryParameters: {'year': year},
    );

    final data = response.data['data'] as List<dynamic>;
    return data
        .map((json) => (json as Map<String, dynamic>)['date'] as String)
        .toList();
  }
}
