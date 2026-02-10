import '../repositories/order_repository.dart';

/// 거래처 여신 잔액 조회 UseCase
///
/// 선택된 거래처의 여신 잔액을 조회합니다.
class GetCreditBalance {
  final OrderRepository _repository;

  GetCreditBalance(this._repository);

  /// 여신 잔액 조회 실행
  ///
  /// [clientId]: 거래처 ID
  /// Returns: 여신 잔액 (원)
  Future<int> call({required int clientId}) async {
    return await _repository.getCreditBalance(clientId: clientId);
  }
}
