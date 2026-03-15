import 'package:dio/dio.dart';

import '../../domain/entities/attendance_result.dart';
import '../../domain/entities/store_schedule_item.dart';
import '../../domain/entities/attendance_status.dart';
import '../../domain/repositories/attendance_repository.dart';
import '../models/attendance_result_model.dart';
import '../models/attendance_status_model.dart';
import '../models/store_schedule_item_model.dart';

/// 출근등록 API DataSource
///
/// Dio HTTP 클라이언트를 사용하여 Backend 출근등록 API와 통신합니다.
class AttendanceApiDataSource {
  final Dio _dio;

  AttendanceApiDataSource(this._dio);

  /// 거래처 목록 조회
  Future<StoreListResult> getStoreList({String? keyword}) async {
    final queryParameters = <String, dynamic>{};
    if (keyword != null && keyword.isNotEmpty) {
      queryParameters['keyword'] = keyword;
    }

    final response = await _dio.get(
      '/api/v1/attendance/stores',
      queryParameters: queryParameters.isNotEmpty ? queryParameters : null,
    );

    final data = response.data['data'] as Map<String, dynamic>;
    final storesJson = data['stores'] as List<dynamic>;
    final stores = storesJson
        .map((json) =>
            StoreScheduleItemModel.fromJson(json as Map<String, dynamic>)
                .toEntity())
        .toList();

    return StoreListResult(
      stores: stores,
      totalCount: data['totalCount'] as int,
      registeredCount: data['registeredCount'] as int,
      currentDate: data['currentDate'] as String,
    );
  }

  /// 출근 등록
  Future<AttendanceResult> registerAttendance({
    required int scheduleId,
    required double latitude,
    required double longitude,
    String? workType,
  }) async {
    final body = <String, dynamic>{
      'schedule_id': scheduleId,
      'latitude': latitude,
      'longitude': longitude,
    };
    if (workType != null) {
      body['workType'] = workType;
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
    final statusListJson = data['statusList'] as List<dynamic>;
    final statusList = statusListJson
        .map((json) =>
            AttendanceStatusModel.fromJson(json as Map<String, dynamic>)
                .toEntity())
        .toList();

    return AttendanceStatusResult(
      totalCount: data['totalCount'] as int,
      registeredCount: data['registeredCount'] as int,
      statusList: statusList,
      currentDate: data['currentDate'] as String,
    );
  }
}
