import 'package:dio/dio.dart';

import '../../domain/entities/leader_monthly_schedule.dart';
import '../models/leader_account_model.dart';
import '../models/leader_daily_status_model.dart';
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

  /// 여사원 일별 현황 조회 ([date]: YYYY-MM-DD).
  Future<LeaderDailyStatusModel> getDailyStatus(String date) async {
    final response = await _dio.get(
      '/api/v1/mobile/leader/daily-status',
      queryParameters: {'date': date},
    );
    return LeaderDailyStatusModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  /// 여사원 월간 일정 캘린더 (레거시 mgnSchedule). [employeeId] null 이면 "여사원 전체".
  Future<LeaderMonthlyCalendar> getMonthlyCalendar({
    int? employeeId,
    required int year,
    required int month,
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/leader/schedule/monthly',
      queryParameters: {
        'year': year,
        'month': month,
        'employeeId': ?employeeId,
      },
    );
    final data = response.data['data'] as Map<String, dynamic>;
    final days = (data['days'] as List<dynamic>? ?? [])
        .map((json) {
          final d = json as Map<String, dynamic>;
          return LeaderCalendarDay(
            date: d['date'] as String,
            total: d['total'] as int? ?? 0,
            attended: d['attended'] as int? ?? 0,
          );
        })
        .toList();
    return LeaderMonthlyCalendar(
      year: data['year'] as int,
      month: data['month'] as int,
      days: days,
    );
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
