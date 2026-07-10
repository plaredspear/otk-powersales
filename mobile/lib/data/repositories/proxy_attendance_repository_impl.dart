import '../../domain/entities/branch_option.dart';
import '../../domain/entities/leader_daily_status.dart';
import '../../domain/entities/leader_team_member.dart';
import '../../domain/repositories/proxy_attendance_repository.dart';
import '../datasources/proxy_attendance_api_datasource.dart';

/// [ProxyAttendanceRepository] 구현 — datasource 응답 모델을 도메인 엔티티로 변환.
class ProxyAttendanceRepositoryImpl implements ProxyAttendanceRepository {
  final ProxyAttendanceApiDataSource _dataSource;

  ProxyAttendanceRepositoryImpl(this._dataSource);

  @override
  Future<List<BranchOption>> getBranches() async {
    final models = await _dataSource.getBranches();
    return models.map((m) => m.toEntity()).toList();
  }

  @override
  Future<List<LeaderTeamMember>> getTeamMembers(String branchCode) async {
    final models = await _dataSource.getTeamMembers(branchCode);
    return models.map((m) => m.toEntity()).toList();
  }

  @override
  Future<LeaderDailyStatus> getDailyStatus(
    String branchCode,
    DateTime date,
  ) async {
    final model = await _dataSource.getDailyStatus(branchCode, _formatDate(date));
    return model.toEntity();
  }

  @override
  Future<void> registerProxyAttendance({
    required String branchCode,
    required int targetEmployeeId,
    int? scheduleId,
    int? displayWorkScheduleId,
  }) {
    return _dataSource.registerProxyAttendance(
      branchCode: branchCode,
      targetEmployeeId: targetEmployeeId,
      scheduleId: scheduleId,
      displayWorkScheduleId: displayWorkScheduleId,
    );
  }

  String _formatDate(DateTime date) =>
      '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
}
