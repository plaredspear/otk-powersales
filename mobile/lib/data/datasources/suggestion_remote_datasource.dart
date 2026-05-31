import '../models/suggestion_detail_model.dart';
import '../models/suggestion_list_item_model.dart';
import '../models/suggestion_register_request.dart';
import '../models/suggestion_register_result_model.dart';

/// 제안하기 Remote DataSource
abstract class SuggestionRemoteDataSource {
  /// 제안하기 등록
  Future<SuggestionRegisterResultModel> registerSuggestion(
      SuggestionRegisterRequest request);

  /// 내 제안/물류클레임 목록 조회 (GET /api/v1/mobile/suggestions)
  Future<SuggestionListPageModel> getSuggestions({
    int page = 0,
    int size = 20,
  });

  /// 제안/물류클레임 상세 조회 (GET /api/v1/mobile/suggestions/{id})
  Future<SuggestionDetailModel> getSuggestionDetail(int suggestionId);
}
