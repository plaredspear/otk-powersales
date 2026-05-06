import '../../../data/models/order_form/order_request_payload_model.dart';
import '../../../data/models/order_form/order_request_response_model.dart';
import '../../repositories/order_form_repository.dart';

/// 주문 등록 UseCase (Spec #598 P1-M, #592 POST).
///
/// `clientRequestId` 가 누락되면 멱등 보장이 깨지므로 명시 권고 (서버는 누락 시에도 동작).
class SubmitOrderRequest {
  final OrderFormRepository _repository;

  SubmitOrderRequest(this._repository);

  Future<OrderRequestResponseModel> call({
    required OrderRequestPayloadModel payload,
  }) {
    return _repository.submitOrderRequest(payload);
  }
}
