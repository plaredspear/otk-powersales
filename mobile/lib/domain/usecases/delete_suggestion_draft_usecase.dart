import '../repositories/suggestion_repository.dart';

/// 제안하기 임시저장 폐기 UseCase
class DeleteSuggestionDraftUseCase {
  final SuggestionRepository _repository;

  DeleteSuggestionDraftUseCase(this._repository);

  Future<void> call() {
    return _repository.deleteDraft();
  }
}
