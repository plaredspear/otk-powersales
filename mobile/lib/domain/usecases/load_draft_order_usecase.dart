import '../entities/order_draft.dart';
import '../repositories/order_repository.dart';

/// 임시저장 주문서 불러오기 UseCase
///
/// 로컬에 임시저장된 주문서를 불러옵니다.
class LoadDraftOrder {
  final OrderRepository _repository;

  LoadDraftOrder(this._repository);

  /// 임시저장 주문서 불러오기 실행
  ///
  /// Returns: 임시저장된 주문서 초안 (없으면 null)
  Future<OrderDraft?> call() async {
    return await _repository.loadDraftOrder();
  }
}
