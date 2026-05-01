import 'package:dio/dio.dart';

import '../models/leader_account_model.dart';
import '../models/leader_schedule_create_request_model.dart';
import '../models/leader_schedule_create_response_model.dart';
import '../models/leader_team_member_model.dart';

/// 조장 대리 일정 등록 / 팀원 / 거래처 조회 API datasource (Spec #554 P1-B).
class LeaderScheduleApiDataSource {
  final Dio _dio;

  LeaderScheduleApiDataSource(this._dio);

  /// 본인 팀원 목록 조회.
  Future<List<LeaderTeamMemberModel>> getTeamMembers() async {
    final response = await _dio.get('/api/v1/mobile/leader/team-members');
    final data = response.data['data'] as List<dynamic>;
    return data
        .map((json) =>
            LeaderTeamMemberModel.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  /// 본인 거래처 목록 조회.
  Future<List<LeaderAccountModel>> getAccounts({String? keyword}) async {
    final queryParams = <String, dynamic>{};
    if (keyword != null && keyword.isNotEmpty) {
      queryParams['keyword'] = keyword;
    }
    final response = await _dio.get(
      '/api/v1/mobile/leader/accounts',
      queryParameters: queryParams.isEmpty ? null : queryParams,
    );
    final data = response.data['data'] as List<dynamic>;
    return data
        .map((json) =>
            LeaderAccountModel.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  /// 팀원 일정 대리 등록.
  Future<LeaderScheduleCreateResponseModel> createTeamMemberSchedule(
    LeaderScheduleCreateRequestModel request,
  ) async {
    final response = await _dio.post(
      '/api/v1/mobile/leader/team-member-schedule',
      data: request.toJson(),
    );
    return LeaderScheduleCreateResponseModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
