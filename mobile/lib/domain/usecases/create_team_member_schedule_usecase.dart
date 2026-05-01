import '../entities/leader_schedule_created.dart';
import '../repositories/leader_schedule_repository.dart';

/// 팀원 일정 대리 등록 UseCase (Spec #554 P1-B).
///
/// 클라이언트 측 입력 검증(스펙 P2-M §5.1):
/// - 일자 / 거래처 / 근무 분류 3 누락 시 [ArgumentError].
class CreateTeamMemberScheduleUseCase {
  final LeaderScheduleRepository _repository;

  const CreateTeamMemberScheduleUseCase(this._repository);

  Future<LeaderScheduleCreated> call({
    required int targetEmployeeId,
    required DateTime? workingDate,
    required int? accountId,
    required String? workingCategory3,
    String? workingCategory1,
  }) {
    if (workingDate == null) {
      throw ArgumentError('일자를 선택해주세요');
    }
    if (accountId == null) {
      throw ArgumentError('거래처를 선택해주세요');
    }
    if (workingCategory3 == null || workingCategory3.isEmpty) {
      throw ArgumentError('근무 분류 3을 선택해주세요');
    }
    return _repository.createTeamMemberSchedule(
      targetEmployeeId: targetEmployeeId,
      workingDate: workingDate,
      accountId: accountId,
      workingCategory3: workingCategory3,
      workingCategory1: workingCategory1,
    );
  }
}
