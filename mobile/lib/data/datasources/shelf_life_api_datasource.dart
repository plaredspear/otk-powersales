import 'package:dio/dio.dart';

import '../models/shelf_life_item_model.dart';
import '../models/shelf_life_register_request.dart';
import '../models/shelf_life_update_request.dart';
import 'shelf_life_remote_datasource.dart';

/// 유통기한 API DataSource 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API와 통신합니다.
class ShelfLifeApiDataSource implements ShelfLifeRemoteDataSource {
  final Dio _dio;

  ShelfLifeApiDataSource(this._dio);

  @override
  Future<List<ShelfLifeItemModel>> getShelfLifeList({
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
      '/api/v1/shelf-life',
      queryParameters: queryParameters,
    );

    final data = response.data['data'] as List<dynamic>;
    return data
        .map((e) => ShelfLifeItemModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<ShelfLifeItemModel> registerShelfLife(
    ShelfLifeRegisterRequest request,
  ) async {
    final response = await _dio.post(
      '/api/v1/shelf-life',
      data: request.toJson(),
    );

    return ShelfLifeItemModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<ShelfLifeItemModel> updateShelfLife(
    int seq,
    ShelfLifeUpdateRequest request,
  ) async {
    final response = await _dio.put(
      '/api/v1/shelf-life/$seq',
      data: request.toJson(),
    );

    return ShelfLifeItemModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<void> deleteShelfLife(int seq) async {
    await _dio.delete('/api/v1/shelf-life/$seq');
  }

  @override
  Future<ShelfLifeBatchDeleteResponse> deleteShelfLifeBatch(
    List<int> seqs,
  ) async {
    final response = await _dio.post(
      '/api/v1/shelf-life/batch-delete',
      data: {'ids': seqs},
    );

    return ShelfLifeBatchDeleteResponse.fromJson(
      response.data as Map<String, dynamic>,
    );
  }
}
