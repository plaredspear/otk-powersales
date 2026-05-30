import 'package:dio/dio.dart';

import '../models/product_model.dart';
import 'product_remote_datasource.dart';

/// 제품검색 API 데이터소스 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API(ProductController)와 통신합니다.
class ProductApiDataSource implements ProductRemoteDataSource {
  final Dio _dio;

  ProductApiDataSource(this._dio);

  @override
  Future<ProductPageModel> searchProducts({
    required String query,
    String type = 'text',
    int page = 0,
    int size = 20,
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/products/search',
      queryParameters: {
        'query': query,
        'type': type,
        'page': page,
        'size': size,
      },
    );

    return ProductPageModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
