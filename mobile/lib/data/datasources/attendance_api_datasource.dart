import 'package:dio/dio.dart';

import '../../domain/entities/attendance_result.dart';
import '../../domain/repositories/attendance_repository.dart';
import '../models/attendance_result_model.dart';
import '../models/attendance_status_model.dart';
import '../models/account_schedule_item_model.dart';

/// 출근등록 API DataSource
///
/// Dio HTTP 클라이언트를 사용하여 Backend 출근등록 API와 통신합니다.
class AttendanceApiDataSource {
  final Dio _dio;

  AttendanceApiDataSource(this._dio);

  /// 거래처 목록 조회
  Future<AccountListResult> getAccountList({String? keyword}) async {
    final queryParameters = <String, dynamic>{};
    if (keyword != null && keyword.isNotEmpty) {
      queryParameters['keyword'] = keyword;
    }

    final response = await _dio.get(
      '/api/v1/attendance/accounts',
      queryParameters: queryParameters.isNotEmpty ? queryParameters : null,
    );

    final data = response.data['data'] as Map<String, dynamic>;
    final accountsJson = data['accounts'] as List<dynamic>;
    final accounts = accountsJson
        .map((json) =>
            AccountScheduleItemModel.fromJson(json as Map<String, dynamic>)
                .toEntity())
        .toList();

    return AccountListResult(
      accounts: accounts,
      totalCount: data['total_count'] as int,
      registeredCount: data['registered_count'] as int,
      currentDate: data['current_date'] as String,
      safetyCheckCompleted: data['safety_check_completed'] as bool? ?? true,
    );
  }

  /// 출근 등록
  ///
  /// [displayWorkScheduleId]가 있으면 진열마스터 기반 등록,
  /// 없으면 기존 schedule_id 기반 등록
  Future<AttendanceResult> registerAttendance({
    required int scheduleId,
    int? displayWorkScheduleId,
    required double latitude,
    required double longitude,
  }) async {
    final body = <String, dynamic>{
      'latitude': latitude,
      'longitude': longitude,
    };

    if (displayWorkScheduleId != null) {
      body['display_work_schedule_id'] = displayWorkScheduleId;
    } else {
      body['schedule_id'] = scheduleId;
    }

    final response = await _dio.post(
      '/api/v1/attendance',
      data: body,
    );

    final data = response.data['data'] as Map<String, dynamic>;
    return AttendanceResultModel.fromJson(data).toEntity();
  }

  /// 출근 현황 조회
  Future<AttendanceStatusResult> getAttendanceStatus() async {
    final response = await _dio.get('/api/v1/attendance/status');

    final data = response.data['data'] as Map<String, dynamic>;
    final statusListJson = data['status_list'] as List<dynamic>;
    final statusList = statusListJson
        .map((json) =>
            AttendanceStatusModel.fromJson(json as Map<String, dynamic>)
                .toEntity())
        .toList();

    return AttendanceStatusResult(
      totalCount: data['total_count'] as int,
      registeredCount: data['registered_count'] as int,
      statusList: statusList,
      currentDate: data['current_date'] as String,
    );
  }
}
