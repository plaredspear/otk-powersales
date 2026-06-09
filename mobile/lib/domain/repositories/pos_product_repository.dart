import '../entities/pos_product.dart';

/// POS 매출 조회용 제품 검색 Repository 인터페이스.
///
/// 레거시 `posmain.jsp` 의 제품명 팝업(`productSelectList`) / 바코드 스캔(`selectBarcodeList`) 동등.
/// 백엔드 `GET /api/v1/mobile/products/search` 를 사용한다.
abstract class PosProductRepository {
  /// 제품명/제품코드 텍스트 검색 (레거시 제품명 팝업).
  Future<List<PosProduct>> searchByText(String query);

  /// 바코드로 제품 1건 조회 (레거시 바코드 스캔). 없으면 null.
  Future<PosProduct?> findByBarcode(String barcode);
}
