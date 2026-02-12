import '../entities/suggestion_form.dart';
import '../entities/suggestion_result.dart';
import '../repositories/suggestion_repository.dart';

/// 제안하기 등록 UseCase
///
/// 제안하기 등록 폼 데이터를 검증하고 등록합니다.
class RegisterSuggestionUseCase {
  final SuggestionRepository _repository;

  RegisterSuggestionUseCase(this._repository);

  /// 제안하기 등록 실행
  ///
  /// [form]: 등록 폼 데이터
  /// Returns: 등록된 제안 결과
  /// Throws: [Exception] 폼 유효성 검증 실패 시
  Future<SuggestionRegisterResult> call(SuggestionRegisterForm form) async {
    // 폼 유효성 검증
    final errors = form.validate();
    if (errors.isNotEmpty) {
      throw Exception(errors.first);
    }

    return await _repository.registerSuggestion(form);
  }
}
