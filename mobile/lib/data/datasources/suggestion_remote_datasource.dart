import '../models/suggestion_register_request.dart';
import '../models/suggestion_register_result_model.dart';

/// 제안하기 Remote DataSource
abstract class SuggestionRemoteDataSource {
  /// 제안하기 등록
  Future<SuggestionRegisterResultModel> registerSuggestion(
      SuggestionRegisterRequest request);
}
