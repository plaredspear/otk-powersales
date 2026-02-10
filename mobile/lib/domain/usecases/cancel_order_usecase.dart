import '../entities/order_cancel.dart';
import '../repositories/order_repository.dart';

/// 주문 취소 UseCase
///
/// 선택한 제품의 주문을 취소합니다.
class CancelOrderUseCase {
  final OrderRepository _repository;

  CancelOrderUseCase(this._repository);

  /// 주문 취소를 실행합니다.
  ///
  /// [orderId]: 취소할 주문 ID
  /// [productCodes]: 취소할 제품코드 목록
  ///
  /// Returns: 취소 결과 (취소된 제품 수, 취소된 제품코드 목록)
  /// Throws: [ArgumentError] productCodes가 비어있을 때
  Future<OrderCancelResult> call({
    required int orderId,
    required List<String> productCodes,
  }) {
    if (productCodes.isEmpty) {
      throw ArgumentError('취소할 제품을 선택해주세요');
    }
    return _repository.cancelOrder(
      orderId: orderId,
      productCodes: productCodes,
    );
  }
}
