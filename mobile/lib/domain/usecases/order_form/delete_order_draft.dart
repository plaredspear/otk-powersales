import '../../repositories/order_form_repository.dart';

/// 임시저장 삭제 UseCase (Spec #598 P1-M, #596 DELETE).
///
/// 멱등 — 없는 경우에도 204 (성공).
class DeleteOrderDraft {
  final OrderFormRepository _repository;

  DeleteOrderDraft(this._repository);

  Future<void> call() {
    return _repository.deleteOrderDraft();
  }
}
