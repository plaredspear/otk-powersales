import 'package:dio/dio.dart';

import '../../domain/entities/pos_product.dart';
import '../models/pos_product_model.dart';

/// POS 매출 조회용 제품 검색 API 데이터소스.
///
/// Dio 로 백엔드 `ProductController` 의 제품 검색 엔드포인트와 통신한다.
class PosProductApiDataSource {
  final Dio _dio;

  PosProductApiDataSource(this._dio);

  /// 제품명/제품코드 텍스트 검색 (레거시 제품명 팝업).
  Future<List<PosProduct>> searchByText(String query, {int size = 30}) async {
    final response = await _dio.get(
      '/api/v1/mobile/products/search',
      queryParameters: <String, dynamic>{
        'query': query,
        'type': 'text',
        'page': 0,
        'size': size,
      },
    );

    return PosProductModel.listFromPage(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  /// 바코드로 제품 1건 조회 (레거시 바코드 스캔). 없으면 null.
  ///
  /// 물류바코드(`type=barcode`) 우선 조회 후 미발견 시 텍스트(`type=text`, 숫자는 제품코드+바코드
  /// 포함 검색)로 fallback 하여 소비자 바코드도 매칭한다.
  Future<PosProduct?> findByBarcode(String barcode) async {
    final byLogistics = await _searchFirst(barcode, 'barcode');
    if (byLogistics != null) return byLogistics;
    return _searchFirst(barcode, 'text');
  }

  Future<PosProduct?> _searchFirst(String query, String type) async {
    final response = await _dio.get(
      '/api/v1/mobile/products/search',
      queryParameters: <String, dynamic>{
        'query': query,
        'type': type,
        'page': 0,
        'size': 1,
      },
    );

    final products = PosProductModel.listFromPage(
      response.data['data'] as Map<String, dynamic>,
    );
    return products.isEmpty ? null : products.first;
  }
}
