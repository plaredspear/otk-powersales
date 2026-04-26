import '../entities/order_draft.dart';
import '../repositories/order_repository.dart';

/// 주문서 임시저장 UseCase
///
/// 작성 중인 주문서를 로컬에 임시 저장합니다.
/// 최근 1건만 유지됩니다.
class SaveDraftOrder {
  final OrderRepository _repository;

  SaveDraftOrder(this._repository);

  /// 주문서 임시저장 실행
  ///
  /// [orderDraft]: 저장할 주문서 초안
  Future<void> call({required OrderDraft orderDraft}) async {
    final draftToSave = orderDraft.copyWith(
      isDraft: true,
      lastModified: DateTime.now(),
    );
    await _repository.saveDraftOrder(orderDraft: draftToSave);
  }
}
