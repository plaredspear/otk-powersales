import '../entities/suggestion_form.dart';
import '../repositories/suggestion_repository.dart';

/// 제안하기 임시저장 UseCase
///
/// 검증 없이 현재 폼 상태를 서버에 upsert 한다.
class SaveSuggestionDraftUseCase {
  final SuggestionRepository _repository;

  SaveSuggestionDraftUseCase(this._repository);

  /// 임시저장 실행 ([form] 이 null 이어도 빈 draft 로 upsert 가능)
  Future<void> call(SuggestionRegisterForm? form) {
    return _repository.saveDraft(form);
  }
}
