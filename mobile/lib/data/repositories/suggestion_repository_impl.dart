import '../../domain/entities/suggestion_form.dart';
import '../../domain/entities/suggestion_result.dart';
import '../../domain/repositories/suggestion_repository.dart';
import '../datasources/suggestion_remote_datasource.dart';
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
}
