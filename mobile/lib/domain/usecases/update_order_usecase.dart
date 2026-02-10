import '../entities/order_draft.dart';
import '../entities/validation_error.dart';
import '../repositories/order_repository.dart';

/// 주문서 수정 UseCase
///
/// 기존 주문서를 수정합니다.
class UpdateOrder {
  final OrderRepository _repository;

  UpdateOrder(this._repository);

  /// 주문서 수정 실행
  ///
  /// [orderId]: 수정할 주문 ID
  /// [orderDraft]: 수정된 주문서 초안
  /// Returns: 수정 결과 (주문 ID, 요청번호, 상태)
  Future<OrderSubmitResult> call({
    required int orderId,
    required OrderDraft orderDraft,
  }) async {
    return await _repository.updateOrder(
      orderId: orderId,
      orderDraft: orderDraft,
    );
  }
}
