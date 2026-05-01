import '../../domain/entities/leader_account.dart';
import '../../domain/entities/leader_schedule_created.dart';
import '../../domain/entities/leader_team_member.dart';
import '../../domain/repositories/leader_schedule_repository.dart';
import '../datasources/leader_schedule_api_datasource.dart';
import '../models/leader_schedule_create_request_model.dart';

/// 백엔드 정책 보존: working_type='근무', working_category2='전담' 자동 채움 (스펙 §1.4).
const String _workingTypeWork = '근무';
const String _workingCategory2Dedicated = '전담';

class LeaderScheduleRepositoryImpl implements LeaderScheduleRepository {
  final LeaderScheduleApiDataSource _dataSource;

  const LeaderScheduleRepositoryImpl(this._dataSource);

  @override
  Future<List<LeaderTeamMember>> getTeamMembers() async {
    final models = await _dataSource.getTeamMembers();
    return models.map((m) => m.toEntity()).toList();
  }

  @override
  Future<List<LeaderAccount>> getAccounts({String? keyword}) async {
    final models = await _dataSource.getAccounts(keyword: keyword);
    return models.map((m) => m.toEntity()).toList();
  }

  @override
  Future<LeaderScheduleCreated> createTeamMemberSchedule({
    required int targetEmployeeId,
    required DateTime workingDate,
    required int accountId,
    required String workingCategory3,
    String? workingCategory1,
  }) async {
    final request = LeaderScheduleCreateRequestModel(
      targetEmployeeId: targetEmployeeId,
      workingDate: _formatDate(workingDate),
      workingType: _workingTypeWork,
      workingCategory2: _workingCategory2Dedicated,
      workingCategory3: workingCategory3,
      accountId: accountId,
      workingCategory1: workingCategory1,
    );
    final model = await _dataSource.createTeamMemberSchedule(request);
    return model.toEntity();
  }

  String _formatDate(DateTime date) =>
      '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
}
