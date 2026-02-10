import '../repositories/order_repository.dart';

/// 내 주문 목록 조회 UseCase
///
/// 필터 조건과 정렬, 페이지네이션을 적용하여 내 주문 목록을 조회합니다.
class GetMyOrders {
  final OrderRepository _repository;

  GetMyOrders(this._repository);

  /// 내 주문 목록 조회 실행
  ///
  /// [clientId]: 거래처 ID (미입력 시 전체)
  /// [status]: 승인상태 코드 (미입력 시 전체)
  /// [deliveryDateFrom]: 납기일 시작 (YYYY-MM-DD)
  /// [deliveryDateTo]: 납기일 종료 (YYYY-MM-DD)
  /// [sortBy]: 정렬 기준 (기본: orderDate)
  /// [sortDir]: 정렬 방향 (기본: DESC)
  /// [page]: 페이지 번호 (0부터 시작)
  /// [size]: 페이지 크기 (기본: 20)
  Future<OrderListResult> call({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
    int page = 0,
    int size = 20,
  }) async {
    return await _repository.getMyOrders(
      clientId: clientId,
      status: status,
      deliveryDateFrom: deliveryDateFrom,
      deliveryDateTo: deliveryDateTo,
      sortBy: sortBy,
      sortDir: sortDir,
      page: page,
      size: size,
    );
  }
}
