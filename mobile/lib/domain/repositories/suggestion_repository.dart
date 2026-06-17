import '../entities/suggestion_detail.dart';
import '../entities/suggestion_draft.dart';
import '../entities/suggestion_form.dart';
import '../entities/suggestion_list_item.dart';
import '../entities/suggestion_result.dart';

/// 제안/물류클레임 목록 페이지 결과
class SuggestionListPage {
  final List<SuggestionListItem> items;
  final int totalElements;
  final int currentPage;
  final bool isLast;

  const SuggestionListPage({
    required this.items,
    required this.totalElements,
    required this.currentPage,
    required this.isLast,
  });
}

/// 제안하기 Repository 인터페이스
///
/// 제안하기 등록/목록/상세 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class SuggestionRepository {
  /// 제안하기 등록
  ///
  /// [form]: 등록 폼 데이터
  /// Returns: 등록된 제안 결과 정보
  Future<SuggestionRegisterResult> registerSuggestion(
      SuggestionRegisterForm form);

  /// 내 제안/물류클레임 목록 조회 (페이징)
  Future<SuggestionListPage> getSuggestions({int page = 0, int size = 20});

  /// 제안/물류클레임 상세 조회
  Future<SuggestionDetail> getSuggestionDetail(int suggestionId);

  /// 제안하기 임시저장 (upsert)
  Future<void> saveDraft(SuggestionRegisterForm? form);

  /// 제안하기 임시저장 조회. 없으면 null.
  Future<SuggestionDraft?> loadDraft();

  /// 제안하기 임시저장 폐기
  Future<void> deleteDraft();
}
