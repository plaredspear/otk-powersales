import '../entities/leader_account.dart';
import '../entities/leader_daily_status.dart';
import '../entities/leader_monthly_schedule.dart';
import '../entities/leader_schedule_created.dart';
import '../entities/leader_team_member.dart';

/// 조장 대리 일정 등록 Repository (Spec #554).
abstract class LeaderScheduleRepository {
  /// 조장 본인 팀원 목록 조회.
  Future<List<LeaderTeamMember>> getTeamMembers();

  /// 여사원 월간 일정 캘린더 (레거시 mgnSchedule). [employeeId] null 이면 "여사원 전체".
  Future<LeaderMonthlyCalendar> getMonthlyCalendar({
    int? employeeId,
    required int year,
    required int month,
  });

  /// 여사원 일별 현황 조회. [date] 기준 진열/행사/연차 + 출근 현황.
  Future<LeaderDailyStatus> getDailyStatus(DateTime date);

  /// 조장 대리출근 등록 (레거시 mngDaily addScheduleProc).
  /// 진열=[displayWorkScheduleId], 행사·기배정=[scheduleId] 중 하나 전달.
  Future<void> registerProxyAttendance({
    required int targetEmployeeId,
    int? scheduleId,
    int? displayWorkScheduleId,
  });

  /// 조장 행사 일정 변경 — 담당 여사원/투입일 재배정 (레거시 scheduleChangePromo M).
  Future<void> changeEventAssignment({
    required int scheduleId,
    required int targetEmployeeId,
    required DateTime workingDate,
  });

  /// 조장 행사 일정 삭제 — 행사 배정 해제 (레거시 scheduleChangePromo D).
  Future<void> deleteEventAssignment(int scheduleId);

  /// 조장 본인 거래처 목록 조회. [keyword] 부분 일치 검색 (선택).
  Future<List<LeaderAccount>> getAccounts({String? keyword});

  /// 팀원(여사원) 단말 초기화 (레거시 SF UUIDReset).
  Future<void> resetTeamMemberDevice(int employeeId);

  /// 팀원(여사원) 비밀번호 초기화 (레거시 SF PasswordReset).
  Future<void> resetTeamMemberPassword(int employeeId);

  /// 팀원 일정 대리 등록.
  Future<LeaderScheduleCreated> createTeamMemberSchedule({
    required int targetEmployeeId,
    required DateTime workingDate,
    required int accountId,
    required String workingCategory3,
    String? workingCategory1,
  });
}
