import '../../domain/entities/shelf_life_form.dart';
import '../../domain/entities/shelf_life_item.dart';
import '../../domain/repositories/shelf_life_repository.dart';
import '../datasources/shelf_life_remote_datasource.dart';
import '../models/shelf_life_register_request.dart';
import '../models/shelf_life_update_request.dart';

/// 유통기한 Repository 구현체
///
/// ShelfLifeRemoteDataSource를 사용하여 API를 호출하고,
/// 응답 데이터를 도메인 엔티티로 변환합니다.
class ShelfLifeRepositoryImpl implements ShelfLifeRepository {
  final ShelfLifeRemoteDataSource _remoteDataSource;

  ShelfLifeRepositoryImpl({
    required ShelfLifeRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<List<ShelfLifeItem>> getShelfLifeList(ShelfLifeFilter filter) async {
    final response = await _remoteDataSource.getShelfLifeList(
      storeId: filter.storeId,
      fromDate: filter.fromDate.toIso8601String().substring(0, 10),
      toDate: filter.toDate.toIso8601String().substring(0, 10),
    );

    return response.map((model) => model.toEntity()).toList();
  }

  @override
  Future<ShelfLifeItem> registerShelfLife(ShelfLifeRegisterForm form) async {
    final request = ShelfLifeRegisterRequest.fromForm(form);
    final response = await _remoteDataSource.registerShelfLife(request);
    return response.toEntity();
  }

  @override
  Future<ShelfLifeItem> updateShelfLife(
    int id,
    ShelfLifeUpdateForm form,
  ) async {
    final request = ShelfLifeUpdateRequest.fromForm(form);
    final response = await _remoteDataSource.updateShelfLife(id, request);
    return response.toEntity();
  }

  @override
  Future<void> deleteShelfLife(int id) async {
    await _remoteDataSource.deleteShelfLife(id);
  }

  @override
  Future<int> deleteShelfLifeBatch(List<int> ids) async {
    final response = await _remoteDataSource.deleteShelfLifeBatch(ids);
    return response.deletedCount;
  }
}
