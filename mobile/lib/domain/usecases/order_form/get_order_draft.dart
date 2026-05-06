import '../../../data/models/order_form/order_draft_response_model.dart';
import '../../repositories/order_form_repository.dart';

/// 임시저장 조회 UseCase (Spec #598 P1-M, #596 GET).
///
/// P2-M 진입 시 자동 호출. 응답이 없으면 `null` 반환 (예외 아님).
class GetOrderDraft {
  final OrderFormRepository _repository;

  GetOrderDraft(this._repository);

  Future<OrderDraftResponseModel?> call() {
    return _repository.getOrderDraft();
  }
}
