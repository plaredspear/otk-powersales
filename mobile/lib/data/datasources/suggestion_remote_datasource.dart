import '../../domain/entities/suggestion_draft.dart';
import '../models/suggestion_detail_model.dart';
import '../models/suggestion_draft_request.dart';
import '../models/suggestion_list_item_model.dart';
import '../models/suggestion_register_request.dart';
import '../models/suggestion_register_result_model.dart';

/// 제안하기 Remote DataSource
abstract class SuggestionRemoteDataSource {
  /// 제안하기 등록
  Future<SuggestionRegisterResultModel> registerSuggestion(
      SuggestionRegisterRequest request);

  /// 내 제안/물류클레임 목록 조회 (GET /api/v1/mobile/suggestions)
  ///
  /// [category] 지정 시 해당 분류만 조회(예: LOGISTICS_CLAIM = 물류클레임 전용).
  Future<SuggestionListPageModel> getSuggestions({
    int page = 0,
    int size = 20,
    String? category,
  });

  /// 제안/물류클레임 상세 조회 (GET /api/v1/mobile/suggestions/{id})
  Future<SuggestionDetailModel> getSuggestionDetail(int suggestionId);

  /// 제안하기 임시저장 (upsert, POST /api/v1/mobile/suggestions/draft)
  Future<void> saveDraft(SuggestionDraftRequest request);

  /// 제안하기 임시저장 조회 (GET /api/v1/mobile/suggestions/draft).
  /// 없으면 null. photoUrls 는 임시 파일로 내려받아 채운다.
  Future<SuggestionDraft?> loadDraft();

  /// 제안하기 임시저장 폐기 (DELETE /api/v1/mobile/suggestions/draft)
  Future<void> deleteDraft();
}
