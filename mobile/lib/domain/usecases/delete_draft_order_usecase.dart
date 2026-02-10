import '../repositories/order_repository.dart';

/// 임시저장 주문서 삭제 UseCase
///
/// 로컬에 임시저장된 주문서를 삭제합니다.
class DeleteDraftOrder {
  final OrderRepository _repository;

  DeleteDraftOrder(this._repository);

  /// 임시저장 주문서 삭제 실행
  Future<void> call() async {
    await _repository.deleteDraftOrder();
  }
}
