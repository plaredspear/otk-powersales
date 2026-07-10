import 'package:dio/dio.dart';

import '../models/branch_option_model.dart';
import '../models/leader_daily_status_model.dart';
import '../models/leader_team_member_model.dart';

/// AccountViewAll 대리출근 API datasource.
///
/// 조장 대리출근(`/api/v1/mobile/leader/*`)과 달리, AccountViewAll 은 지점을 직접 선택하므로
/// 지점 목록/지점별 여사원/지점별 일별현황 조회에 branchCode 를 전달한다.
class ProxyAttendanceApiDataSource {
  final Dio _dio;

  ProxyAttendanceApiDataSource(this._dio);

  /// 대리출근 지점 선택 옵션 조회.
  Future<List<BranchOptionModel>> getBranches() async {
    final response = await _dio.get('/api/v1/mobile/proxy-attendance/branches');
    final data = response.data['data'] as List<dynamic>;
    return data
        .map((json) => BranchOptionModel.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  /// 선택 지점 여사원 목록 조회.
  Future<List<LeaderTeamMemberModel>> getTeamMembers(String branchCode) async {
    final response = await _dio.get(
      '/api/v1/mobile/proxy-attendance/team-members',
      queryParameters: {'branchCode': branchCode},
    );
    final data = response.data['data'] as List<dynamic>;
    return data
        .map((json) =>
            LeaderTeamMemberModel.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  /// 선택 지점 여사원 일별 현황 조회 ([date]: YYYY-MM-DD).
  Future<LeaderDailyStatusModel> getDailyStatus(
    String branchCode,
    String date,
  ) async {
    final response = await _dio.get(
      '/api/v1/mobile/proxy-attendance/daily-status',
      queryParameters: {'branchCode': branchCode, 'date': date},
    );
    return LeaderDailyStatusModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  /// 대리출근 등록. 진열=displayWorkScheduleId, 행사·기배정=scheduleId 중 하나 전달.
  Future<void> registerProxyAttendance({
    required String branchCode,
    required int targetEmployeeId,
    int? scheduleId,
    int? displayWorkScheduleId,
  }) async {
    final body = <String, dynamic>{
      'branchCode': branchCode,
      'targetEmployeeId': targetEmployeeId,
    };
    if (displayWorkScheduleId != null) {
      body['displayWorkScheduleId'] = displayWorkScheduleId;
    } else if (scheduleId != null) {
      body['scheduleId'] = scheduleId;
    }
    await _dio.post('/api/v1/mobile/proxy-attendance', data: body);
  }
}
