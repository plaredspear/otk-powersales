import '../repositories/order_repository.dart';

/// 주문 재전송 UseCase
///
/// 전송실패 상태의 주문을 재전송합니다.
class ResendOrder {
  final OrderRepository _repository;

  ResendOrder(this._repository);

  /// 주문을 재전송합니다.
  ///
  /// [orderId]: 재전송할 주문 ID
  Future<void> call({required int orderId}) {
    return _repository.resendOrder(orderId: orderId);
  }
}
