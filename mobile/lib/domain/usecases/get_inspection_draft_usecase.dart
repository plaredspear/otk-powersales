import '../entities/inspection_draft.dart';
import '../repositories/inspection_repository.dart';

/// 현장 점검 임시저장 조회 UseCase
class GetInspectionDraftUseCase {
  final InspectionRepository _repository;

  GetInspectionDraftUseCase(this._repository);

  /// 사원 본인의 임시저장 조회 (없으면 null)
  Future<InspectionDraft?> call() {
    return _repository.getDraft();
  }
}
