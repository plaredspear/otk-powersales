import '../models/product_detail_model.dart';
import '../models/product_model.dart';

/// 제품검색 원격 데이터소스 인터페이스
///
/// API 서버와의 제품 검색 관련 통신을 추상화합니다.
abstract class ProductRemoteDataSource {
  /// 제품 검색 API 호출
  ///
  /// GET /api/v1/mobile/products/search
  ///
  /// [query]: 검색어 (제품명/제품코드/바코드)
  /// [type]: 검색 유형 ('text' 또는 'barcode')
  /// [page]: 페이지 번호 (0부터 시작)
  /// [size]: 페이지 크기
  Future<ProductPageModel> searchProducts({
    required String query,
    String type = 'text',
    int page = 0,
    int size = 20,
  });

  /// 제품 상세 조회 API 호출
  ///
  /// GET /api/v1/mobile/products/{productCode}
  ///
  /// [productCode]: 제품코드
  Future<ProductDetailModel> getProductDetail(String productCode);
}
