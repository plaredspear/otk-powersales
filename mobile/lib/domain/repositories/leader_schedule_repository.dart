import '../entities/leader_account.dart';
import '../entities/leader_daily_status.dart';
import '../entities/leader_schedule_created.dart';
import '../entities/leader_team_member.dart';

/// 조장 대리 일정 등록 Repository (Spec #554).
abstract class LeaderScheduleRepository {
  /// 조장 본인 팀원 목록 조회.
  Future<List<LeaderTeamMember>> getTeamMembers();

  /// 여사원 일별 현황 조회 (조회 전용). [date] 기준 진열/행사/연차 + 출근 현황.
  Future<LeaderDailyStatus> getDailyStatus(DateTime date);

  /// 조장 본인 거래처 목록 조회. [keyword] 부분 일치 검색 (선택).
  Future<List<LeaderAccount>> getAccounts({String? keyword});

  /// 팀원 일정 대리 등록.
  Future<LeaderScheduleCreated> createTeamMemberSchedule({
    required int targetEmployeeId,
    required DateTime workingDate,
    required int accountId,
    required String workingCategory3,
    String? workingCategory1,
  });
}
