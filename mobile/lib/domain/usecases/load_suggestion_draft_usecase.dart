import '../entities/suggestion_draft.dart';
import '../repositories/suggestion_repository.dart';

/// 제안하기 임시저장 조회 UseCase
///
/// 이어쓰기용 임시저장을 조회한다. 없으면 null.
class LoadSuggestionDraftUseCase {
  final SuggestionRepository _repository;

  LoadSuggestionDraftUseCase(this._repository);

  Future<SuggestionDraft?> call() {
    return _repository.loadDraft();
  }
}
