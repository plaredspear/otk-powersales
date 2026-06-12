import '../entities/inspection_form.dart';
import '../repositories/inspection_repository.dart';

/// 현장 점검 임시저장 UseCase
///
/// 검증 없이 현재 폼 상태를 서버에 임시저장(upsert)합니다.
class SaveInspectionDraftUseCase {
  final InspectionRepository _repository;

  SaveInspectionDraftUseCase(this._repository);

  /// [form]: 등록 폼 데이터
  /// [accountName]/[productName]: prefill 표시용 이름
  Future<void> call(
    InspectionRegisterForm form, {
    String? accountName,
    String? productName,
  }) {
    return _repository.saveDraft(
      form,
      accountName: accountName,
      productName: productName,
    );
  }
}
