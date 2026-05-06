import '../../../data/models/order_form/order_draft_request_model.dart';
import '../../../data/models/order_form/order_draft_response_model.dart';
import '../../repositories/order_form_repository.dart';

/// 임시저장 등록 / 갱신 UseCase (Spec #598 P1-M, #596 POST).
class SaveOrderDraft {
  final OrderFormRepository _repository;

  SaveOrderDraft(this._repository);

  Future<OrderDraftSavedModel> call({required OrderDraftRequestModel request}) {
    return _repository.saveOrderDraft(request);
  }
}
