import 'package:dio/dio.dart';

import '../models/product_expiration_item_model.dart';
import '../models/product_expiration_register_request.dart';
import '../models/product_expiration_update_request.dart';
import 'product_expiration_remote_datasource.dart';

/// 유통기한 API DataSource 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API와 통신합니다.
class ProductExpirationApiDataSource implements ProductExpirationRemoteDataSource {
  final Dio _dio;

  ProductExpirationApiDataSource(this._dio);

  @override
  Future<List<ProductExpirationItemModel>> getProductExpirationList({
    String? accountCode,
    required String fromDate,
    required String toDate,
  }) async {
    final queryParameters = <String, dynamic>{
      'fromDate': fromDate,
      'toDate': toDate,
    };
    if (accountCode != null && accountCode.isNotEmpty) {
      queryParameters['accountCode'] = accountCode;
    }

    final response = await _dio.get(
      '/api/v1/product-expiration',
      queryParameters: queryParameters,
    );

    final data = response.data['data'] as List<dynamic>;
    return data
        .map((e) => ProductExpirationItemModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<ProductExpirationItemModel> registerProductExpiration(
    ProductExpirationRegisterRequest request,
  ) async {
    final response = await _dio.post(
      '/api/v1/product-expiration',
      data: request.toJson(),
    );

    return ProductExpirationItemModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<ProductExpirationItemModel> updateProductExpiration(
    int seq,
    ProductExpirationUpdateRequest request,
  ) async {
    final response = await _dio.put(
      '/api/v1/product-expiration/$seq',
      data: request.toJson(),
    );

    return ProductExpirationItemModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<void> deleteProductExpiration(int seq) async {
    await _dio.delete('/api/v1/product-expiration/$seq');
  }

  @override
  Future<ProductExpirationBatchDeleteResponse> deleteProductExpirationBatch(
    List<int> seqs,
  ) async {
    final response = await _dio.post(
      '/api/v1/product-expiration/batch-delete',
      data: {'ids': seqs},
    );

    return ProductExpirationBatchDeleteResponse.fromJson(
      response.data as Map<String, dynamic>,
    );
  }
}
