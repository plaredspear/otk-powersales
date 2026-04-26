import '../entities/client_order.dart';
import '../repositories/order_repository.dart';

/// 거래처별 주문 목록 조회 UseCase
///
/// 선택한 거래처 + 납기일을 기반으로 거래처별 주문 목록을 조회합니다.
/// 페이지네이션을 지원하며, 페이지 번호 버튼 방식으로 페이지를 이동합니다.
class GetClientOrdersUseCase {
  final OrderRepository _repository;

  GetClientOrdersUseCase(this._repository);

  /// 거래처별 주문 목록을 조회합니다.
  ///
  /// [clientId]: 거래처 ID (필수)
  /// [deliveryDate]: 납기일 (YYYY-MM-DD, 기본: 오늘)
  /// [page]: 페이지 번호 (0부터 시작, 기본: 0)
  /// [size]: 페이지 크기 (기본: 20)
  Future<ClientOrderListResult> call({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  }) async {
    return await _repository.getClientOrders(
      clientId: clientId,
      deliveryDate: deliveryDate,
      page: page,
      size: size,
    );
  }
}
