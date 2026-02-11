import '../models/shelf_life_item_model.dart';
import '../models/shelf_life_register_request.dart';
import '../models/shelf_life_update_request.dart';

/// 유통기한 일괄 삭제 응답 모델
class ShelfLifeBatchDeleteResponse {
  final int deletedCount;

  const ShelfLifeBatchDeleteResponse({required this.deletedCount});

  factory ShelfLifeBatchDeleteResponse.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;
    return ShelfLifeBatchDeleteResponse(
      deletedCount: data['deleted_count'] as int,
    );
  }
}

/// 유통기한 API DataSource 인터페이스
///
/// 유통기한 관련 API 호출을 추상화합니다.
abstract class ShelfLifeRemoteDataSource {
  /// GET /api/v1/shelf-life?store_id=&from_date=&to_date=
  ///
  /// 유통기한 목록을 조회합니다.
  Future<List<ShelfLifeItemModel>> getShelfLifeList({
    int? storeId,
    required String fromDate,
    required String toDate,
  });

  /// POST /api/v1/shelf-life
  ///
  /// 유통기한을 등록합니다.
  Future<ShelfLifeItemModel> registerShelfLife(
    ShelfLifeRegisterRequest request,
  );

  /// PUT /api/v1/shelf-life/{shelfLifeId}
  ///
  /// 유통기한을 수정합니다.
  Future<ShelfLifeItemModel> updateShelfLife(
    int shelfLifeId,
    ShelfLifeUpdateRequest request,
  );

  /// DELETE /api/v1/shelf-life/{shelfLifeId}
  ///
  /// 유통기한 단건을 삭제합니다.
  Future<void> deleteShelfLife(int shelfLifeId);

  /// DELETE /api/v1/shelf-life
  ///
  /// 유통기한을 일괄 삭제합니다.
  Future<ShelfLifeBatchDeleteResponse> deleteShelfLifeBatch(List<int> ids);
}
