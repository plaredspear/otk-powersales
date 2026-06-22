import '../../domain/entities/suggestion_detail.dart';
import '../../domain/entities/suggestion_draft.dart';
import '../../domain/entities/suggestion_form.dart';
import '../../domain/entities/suggestion_result.dart';
import '../../domain/repositories/suggestion_repository.dart';
import '../datasources/suggestion_remote_datasource.dart';
import '../models/suggestion_draft_request.dart';
import '../models/suggestion_register_request.dart';

/// SuggestionRepository 구현체
class SuggestionRepositoryImpl implements SuggestionRepository {
  const SuggestionRepositoryImpl(this._dataSource);

  final SuggestionRemoteDataSource _dataSource;

  @override
  Future<SuggestionRegisterResult> registerSuggestion(
      SuggestionRegisterForm form) async {
    final request = SuggestionRegisterRequest.fromEntity(form);
    final model = await _dataSource.registerSuggestion(request);
    return model.toEntity();
  }

  @override
  Future<SuggestionListPage> getSuggestions({
    int page = 0,
    int size = 20,
    String? category,
    int? accountId,
    String? startDate,
    String? endDate,
  }) async {
    final pageModel = await _dataSource.getSuggestions(
      page: page,
      size: size,
      category: category,
      accountId: accountId,
      startDate: startDate,
      endDate: endDate,
    );
    return SuggestionListPage(
      items: pageModel.content.map((m) => m.toEntity()).toList(),
      totalElements: pageModel.totalElements,
      currentPage: pageModel.number,
      isLast: pageModel.last,
    );
  }

  @override
  Future<SuggestionDetail> getSuggestionDetail(int suggestionId) async {
    final model = await _dataSource.getSuggestionDetail(suggestionId);
    return model.toEntity();
  }

  @override
  Future<void> saveDraft(SuggestionRegisterForm? form) async {
    await _dataSource.saveDraft(SuggestionDraftRequest.fromForm(form));
  }

  @override
  Future<SuggestionDraft?> loadDraft() async {
    return _dataSource.loadDraft();
  }

  @override
  Future<void> deleteDraft() async {
    await _dataSource.deleteDraft();
  }
}
