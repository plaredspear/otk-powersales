import '../repositories/order_request_repository.dart';

/// 본인 주문요청 목록 조회 UseCase (페이징 없음 — 클라이언트 슬라이스).
class GetMyOrderRequests {
  final OrderRequestRepository _repository;

  GetMyOrderRequests(this._repository);

  /// [clientId]: 거래처 ID (미입력 시 전체)
  /// [status]: 주문요청 상태 코드 (미입력 시 전체)
  /// [deliveryDateFrom]: 납기일 시작 (YYYY-MM-DD)
  /// [deliveryDateTo]: 납기일 종료 (YYYY-MM-DD)
  /// [sortBy]: 정렬 기준 (기본: orderDate)
  /// [sortDir]: 정렬 방향 (기본: DESC)
  Future<OrderRequestListResult> call({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
  }) async {
    return await _repository.getMyOrderRequests(
      clientId: clientId,
      status: status,
      deliveryDateFrom: deliveryDateFrom,
      deliveryDateTo: deliveryDateTo,
      sortBy: sortBy,
      sortDir: sortDir,
    );
  }
}
