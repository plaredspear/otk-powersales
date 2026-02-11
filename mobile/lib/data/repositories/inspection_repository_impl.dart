import '../../domain/entities/inspection_detail.dart';
import '../../domain/entities/inspection_field_type.dart';
import '../../domain/entities/inspection_form.dart';
import '../../domain/entities/inspection_list_item.dart';
import '../../domain/entities/inspection_theme.dart';
import '../../domain/repositories/inspection_repository.dart';
import '../datasources/inspection_remote_datasource.dart';
import '../models/inspection_register_request.dart';

/// 현장 점검 Repository 구현체
///
/// InspectionRemoteDataSource를 사용하여 API와 통신하고,
/// Model과 Entity 간 변환을 처리합니다.
class InspectionRepositoryImpl implements InspectionRepository {
  final InspectionRemoteDataSource _remoteDataSource;

  InspectionRepositoryImpl(this._remoteDataSource);

  @override
  Future<List<InspectionListItem>> getInspectionList(
    InspectionFilter filter,
  ) async {
    final models = await _remoteDataSource.getInspectionList(
      storeId: filter.storeId,
      category: filter.category?.toJson(),
      fromDate: filter.fromDate.toIso8601String().substring(0, 10),
      toDate: filter.toDate.toIso8601String().substring(0, 10),
    );

    return models.map((model) => model.toEntity()).toList();
  }

  @override
  Future<InspectionDetail> getInspectionDetail(int id) async {
    final model = await _remoteDataSource.getInspectionDetail(id);
    return model.toEntity();
  }

  @override
  Future<InspectionListItem> registerInspection(
    InspectionRegisterForm form,
  ) async {
    final request = InspectionRegisterRequest.fromEntity(form);
    final model = await _remoteDataSource.registerInspection(request);
    return model.toEntity();
  }

  @override
  Future<List<InspectionTheme>> getThemes() async {
    final models = await _remoteDataSource.getThemes();
    return models.map((model) => model.toEntity()).toList();
  }

  @override
  Future<List<InspectionFieldType>> getFieldTypes() async {
    final models = await _remoteDataSource.getFieldTypes();
    return models.map((model) => model.toEntity()).toList();
  }
}
