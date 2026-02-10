import '../../domain/entities/order.dart';
import '../../domain/entities/order_detail.dart';
import '../../domain/repositories/order_repository.dart';
import '../datasources/order_remote_datasource.dart';

/// 주문 Repository 구현체
///
/// OrderRemoteDataSource를 사용하여 API를 호출하고,
/// 응답 데이터를 도메인 엔티티로 변환합니다.
class OrderRepositoryImpl implements OrderRepository {
  final OrderRemoteDataSource _remoteDataSource;

  OrderRepositoryImpl({
    required OrderRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<OrderListResult> getMyOrders({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
    int page = 0,
    int size = 20,
  }) async {
    final response = await _remoteDataSource.getMyOrders(
      clientId: clientId,
      status: status,
      deliveryDateFrom: deliveryDateFrom,
      deliveryDateTo: deliveryDateTo,
      sortBy: sortBy,
      sortDir: sortDir,
      page: page,
      size: size,
    );

    final orders = response.content
        .map((model) => model.toEntity())
        .toList();

    return OrderListResult(
      orders: orders,
      totalElements: response.totalElements,
      totalPages: response.totalPages,
      currentPage: response.number,
      pageSize: response.size,
      isFirst: response.first,
      isLast: response.last,
    );
  }

  @override
  Future<OrderDetail> getOrderDetail({required int orderId}) async {
    final response = await _remoteDataSource.getOrderDetail(orderId: orderId);
    return response.toEntity();
  }

  @override
  Future<void> resendOrder({required int orderId}) async {
    await _remoteDataSource.resendOrder(orderId: orderId);
  }
}
