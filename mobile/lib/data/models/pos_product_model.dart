import '../../domain/entities/pos_product.dart';

/// POS 제품 검색 API 응답 파서.
///
/// 백엔드 `GET /api/v1/mobile/products/search` 의 `data`(Spring `Page<ProductDto>`) 를
/// [PosProduct] 목록으로 변환한다.
class PosProductModel {
  /// `ProductDto` JSON → [PosProduct]. POS `BARCODE` 매칭 키는 `barcode`(발주단위 대표바코드)
  /// 우선, 없으면 `logisticsBarcode` 로 대체한다.
  static PosProduct fromDto(Map<String, dynamic> json) {
    final barcode = (json['barcode'] as String?)?.trim();
    final logisticsBarcode = (json['logisticsBarcode'] as String?)?.trim();
    return PosProduct(
      productCode: json['productCode'] as String? ?? '',
      productName: json['productName'] as String? ?? '',
      barcode: (barcode != null && barcode.isNotEmpty)
          ? barcode
          : (logisticsBarcode ?? ''),
    );
  }

  /// `Page<ProductDto>` 의 `content` 배열 → [PosProduct] 목록.
  static List<PosProduct> listFromPage(Map<String, dynamic> page) {
    final content = (page['content'] as List<dynamic>?) ?? const [];
    return content
        .map((raw) => fromDto(raw as Map<String, dynamic>))
        .toList();
  }
}
