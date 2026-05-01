import '../models/product_expiration_item_model.dart';
import '../models/product_expiration_register_request.dart';
import '../models/product_expiration_update_request.dart';

/// 유통기한 일괄 삭제 응답 모델
class ProductExpirationBatchDeleteResponse {
  final int deletedCount;

  const ProductExpirationBatchDeleteResponse({required this.deletedCount});

  factory ProductExpirationBatchDeleteResponse.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;
    return ProductExpirationBatchDeleteResponse(
      deletedCount: data['deleted_count'] as int,
    );
  }
}

/// 유통기한 API DataSource 인터페이스
///
/// 유통기한 관련 API 호출을 추상화합니다.
abstract class ProductExpirationRemoteDataSource {
  /// GET /api/v1/mobile/product-expiration?accountCode=&fromDate=&toDate=
  ///
  /// 유통기한 목록을 조회합니다.
  Future<List<ProductExpirationItemModel>> getProductExpirationList({
    String? accountCode,
    required String fromDate,
    required String toDate,
  });

  /// POST /api/v1/mobile/product-expiration
  ///
  /// 유통기한을 등록합니다.
  Future<ProductExpirationItemModel> registerProductExpiration(
    ProductExpirationRegisterRequest request,
  );

  /// PUT /api/v1/mobile/product-expiration/{seq}
  ///
  /// 유통기한을 수정합니다.
  Future<ProductExpirationItemModel> updateProductExpiration(
    int seq,
    ProductExpirationUpdateRequest request,
  );

  /// DELETE /api/v1/mobile/product-expiration/{seq}
  ///
  /// 유통기한 단건을 삭제합니다.
  Future<void> deleteProductExpiration(int seq);

  /// POST /api/v1/mobile/product-expiration/batch-delete
  ///
  /// 유통기한을 일괄 삭제합니다.
  Future<ProductExpirationBatchDeleteResponse> deleteProductExpirationBatch(List<int> seqs);
}
