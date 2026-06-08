import 'package:dio/dio.dart';

/// 제품추가(유통기한) 팝업 전용 원격 데이터소스.
///
/// 레거시 제품추가 팝업(productPop.jsp)의 3개 호출을 묶는다.
///  - 중분류/소분류 카테고리 목록 (`/products/categories`)
///  - 제품 필터 검색 (`/products/search/filter`)
///  - 거래처 주문이력 (`/me/order-requests/product-history`)
class ProductAddRemoteDataSource {
  final Dio _dio;

  ProductAddRemoteDataSource(this._dio);

  /// 중분류→소분류 카테고리 목록 조회.
  ///
  /// 반환: `[{ middle, subs: [...] }, ...]`
  Future<List<Map<String, dynamic>>> fetchCategories() async {
    final response = await _dio.get('/api/v1/mobile/products/categories');
    final data = response.data['data'] as List<dynamic>;
    return data.cast<Map<String, dynamic>>();
  }

  /// 제품 필터 검색 — 제품명/바코드/중분류/소분류 조합(모두 선택적).
  ///
  /// 반환: Page 형태 `{ content: [...], totalElements, last, ... }`
  Future<Map<String, dynamic>> searchByFilter({
    String? productName,
    String? barcode,
    String? category2,
    String? category3,
    int page = 0,
    int size = 50,
  }) async {
    final query = <String, dynamic>{
      'page': page,
      'size': size,
    };
    if (productName != null && productName.isNotEmpty) {
      query['productName'] = productName;
    }
    if (barcode != null && barcode.isNotEmpty) query['barcode'] = barcode;
    if (category2 != null && category2.isNotEmpty) query['category2'] = category2;
    if (category3 != null && category3.isNotEmpty) query['category3'] = category3;

    final response = await _dio.get(
      '/api/v1/mobile/products/search/filter',
      queryParameters: query,
    );
    return response.data['data'] as Map<String, dynamic>;
  }

  /// 거래처 주문이력 조회 (주문일별 그룹).
  ///
  /// 반환: `[{ orderDate, products: [{ productCode, productName }] }, ...]`
  Future<List<Map<String, dynamic>>> fetchOrderHistory({
    required String accountCode,
    required String startDate,
    required String endDate,
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/me/order-requests/product-history',
      queryParameters: {
        'accountCode': accountCode,
        'startDate': startDate,
        'endDate': endDate,
      },
    );
    final data = response.data['data'] as List<dynamic>;
    return data.cast<Map<String, dynamic>>();
  }
}
