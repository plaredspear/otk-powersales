import '../entities/suggestion_form.dart';
import '../entities/suggestion_result.dart';

/// 제안하기 Repository 인터페이스
///
/// 제안하기 등록 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class SuggestionRepository {
  /// 제안하기 등록
  ///
  /// [form]: 등록 폼 데이터
  /// Returns: 등록된 제안 결과 정보
  Future<SuggestionRegisterResult> registerSuggestion(
      SuggestionRegisterForm form);
}
