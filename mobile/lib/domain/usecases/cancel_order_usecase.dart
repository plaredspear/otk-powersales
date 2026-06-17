import '../entities/order_cancel.dart';
import '../repositories/order_request_repository.dart';

/// 주문 취소 UseCase
///
/// 선택한 제품의 주문을 취소합니다.
class CancelOrderUseCase {
  final OrderRequestRepository _repository;

  CancelOrderUseCase(this._repository);

  /// 주문 취소를 실행합니다.
  ///
  /// [orderId]: 취소할 주문 ID
  /// [orderProductIds]: 취소할 주문 라인 PK 목록
  ///
  /// Returns: 취소 결과 (취소 후 주문 상태, 취소된 라인 목록)
  /// Throws: [ArgumentError] orderProductIds가 비어있을 때
  ///
  /// NOTE: 백엔드는 빈 배열을 "전체 취소"로 해석하지만, 본 화면은 사용자가
  /// 선택한 라인만 취소하는 UX이므로 빈 선택은 클라이언트에서 차단한다.
  Future<OrderCancelResult> call({
    required int orderId,
    required List<int> orderProductIds,
  }) {
    if (orderProductIds.isEmpty) {
      throw ArgumentError('취소할 제품을 선택해주세요');
    }
    return _repository.cancelOrderRequest(
      orderId: orderId,
      orderProductIds: orderProductIds,
    );
  }
}
