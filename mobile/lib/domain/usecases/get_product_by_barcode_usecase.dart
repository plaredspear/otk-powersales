import '../entities/product_for_order.dart';
import '../repositories/order_repository.dart';

/// 바코드로 제품 조회 UseCase
///
/// 바코드 문자열로 제품을 조회합니다.
class GetProductByBarcode {
  final OrderRepository _repository;

  GetProductByBarcode(this._repository);

  /// 바코드로 제품 조회 실행
  ///
  /// [barcode]: 바코드 문자열
  /// Returns: 해당 바코드의 제품 정보
  /// Throws: Exception - 제품을 찾을 수 없는 경우
  Future<ProductForOrder> call({required String barcode}) async {
    if (barcode.trim().isEmpty) {
      throw ArgumentError('바코드를 입력해주세요');
    }
    return await _repository.getProductByBarcode(barcode: barcode.trim());
  }
}
