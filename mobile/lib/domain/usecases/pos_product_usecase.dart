import '../entities/pos_product.dart';
import '../repositories/pos_product_repository.dart';

/// POS 매출 조회용 제품 검색 UseCase.
///
/// 레거시 `posmain.jsp` 의 제품명 팝업 / 바코드 스캔 동등 — 매출 조회 제품 목록에 추가할 제품을 찾는다.
class PosProductUseCase {
  final PosProductRepository _repository;

  PosProductUseCase(this._repository);

  /// 제품명/제품코드 텍스트 검색. 공백 검색어는 빈 결과.
  Future<List<PosProduct>> searchByText(String query) {
    final trimmed = query.trim();
    if (trimmed.isEmpty) return Future.value(const []);
    return _repository.searchByText(trimmed);
  }

  /// 바코드로 제품 1건 조회. 없으면 null.
  Future<PosProduct?> findByBarcode(String barcode) {
    final trimmed = barcode.trim();
    if (trimmed.isEmpty) return Future.value(null);
    return _repository.findByBarcode(trimmed);
  }
}
