import '../entities/product_order_history_group.dart';
import '../repositories/order_request_repository.dart';

/// 거래처 주문이력 조회 UseCase (제품추가 모달 "주문 이력" 탭).
///
/// 본인이 해당 거래처([accountCode] = Account.externalKey)에 등록한 주문요청을
/// 주문일 기간([startDate]~[endDate])으로 조회해 주문일별 그룹으로 반환한다.
class GetAccountOrderHistory {
  final OrderRequestRepository _repository;

  GetAccountOrderHistory(this._repository);

  Future<List<ProductOrderHistoryGroup>> call({
    required String accountCode,
    required DateTime startDate,
    required DateTime endDate,
  }) {
    return _repository.getAccountOrderHistory(
      accountCode: accountCode,
      startDate: startDate,
      endDate: endDate,
    );
  }
}
