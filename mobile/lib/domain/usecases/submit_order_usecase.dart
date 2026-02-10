import '../entities/order_draft.dart';
import '../entities/validation_error.dart';
import '../repositories/order_repository.dart';

/// 주문서 전송 UseCase
///
/// 주문서를 승인요청합니다.
/// 전송 성공 시 임시저장 데이터를 자동 삭제합니다.
class SubmitOrder {
  final OrderRepository _repository;

  SubmitOrder(this._repository);

  /// 주문서 전송 실행
  ///
  /// [orderDraft]: 전송할 주문서 초안
  /// Returns: 전송 결과 (주문 ID, 요청번호, 상태)
  Future<OrderSubmitResult> call({required OrderDraft orderDraft}) async {
    final result = await _repository.submitOrder(orderDraft: orderDraft);

    // 전송 성공 시 임시저장 데이터 자동 삭제
    await _repository.deleteDraftOrder();

    return result;
  }
}
