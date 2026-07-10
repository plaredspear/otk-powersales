import '../entities/branch_option.dart';
import '../entities/leader_daily_status.dart';
import '../entities/leader_team_member.dart';

/// AccountViewAll 대리출근 Repository — 지점 선택형.
abstract class ProxyAttendanceRepository {
  /// 대리출근 지점 선택 옵션 조회.
  Future<List<BranchOption>> getBranches();

  /// 선택 지점 여사원 목록 조회.
  Future<List<LeaderTeamMember>> getTeamMembers(String branchCode);

  /// 선택 지점 여사원 일별 현황 조회.
  Future<LeaderDailyStatus> getDailyStatus(String branchCode, DateTime date);

  /// 대리출근 등록. 진열=[displayWorkScheduleId], 행사·기배정=[scheduleId] 중 하나 전달.
  Future<void> registerProxyAttendance({
    required String branchCode,
    required int targetEmployeeId,
    int? scheduleId,
    int? displayWorkScheduleId,
  });
}
