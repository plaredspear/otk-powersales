import '../../domain/entities/client_order.dart';
import '../../domain/entities/order_cancel.dart';
import '../../domain/entities/order_detail.dart';
import '../../domain/entities/product_for_order.dart';
import '../../domain/repositories/order_request_repository.dart';
import '../datasources/order_request_remote_datasource.dart';
import '../models/order_cancel_model.dart';

/// 주문 Repository 구현체
///
/// OrderRequestRemoteDataSource를 사용하여 API를 호출하고
/// 응답 데이터를 도메인 엔티티로 변환합니다.
class OrderRequestRepositoryImpl implements OrderRequestRepository {
  final OrderRequestRemoteDataSource _remoteDataSource;

  OrderRequestRepositoryImpl({
    required OrderRequestRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<OrderRequestListResult> getMyOrderRequests({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
  }) async {
    final response = await _remoteDataSource.getMyOrderRequests(
      clientId: clientId,
      status: status,
      deliveryDateFrom: deliveryDateFrom,
      deliveryDateTo: deliveryDateTo,
      sortBy: sortBy,
      sortDir: sortDir,
    );

    final orders = response.items.map((model) => model.toEntity()).toList();

    return OrderRequestListResult(
      orders: orders,
      total: response.total,
      truncated: response.truncated,
      fetchedAt: response.fetchedAt,
    );
  }

  @override
  Future<OrderDetail> getOrderRequestDetail({required int orderId}) async {
    final response = await _remoteDataSource.getOrderRequestDetail(orderId: orderId);
    return response.toEntity();
  }

  @override
  Future<void> resendOrderRequest({required int orderId}) async {
    await _remoteDataSource.resendOrderRequest(orderId: orderId);
  }

  @override
  Future<OrderCancelResult> cancelOrderRequest({
    required int orderId,
    required List<int> orderProductIds,
  }) async {
    final requestModel = OrderCancelRequestModel(
      orderProductIds: orderProductIds,
    );
    final response = await _remoteDataSource.cancelOrderRequest(
      orderId: orderId,
      request: requestModel,
    );
    return response.toEntity();
  }

  // ─── 주문서 작성 관련 메서드 (F22) ─────────────────────────────
  // 여신/임시저장/검증/제출/수정/바코드는 신규 OrderFormRepository 경로로 대체되어 제거됨.

  @override
  Future<List<ProductForOrder>> getFavoriteProducts() async {
    final models = await _remoteDataSource.getFavoriteProducts();
    return models.map((model) => model.toEntity()).toList();
  }

  @override
  Future<List<ProductForOrder>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) async {
    final models = await _remoteDataSource.searchProductsForOrder(
      query: query,
      categoryMid: categoryMid,
      categorySub: categorySub,
    );
    return models.map((model) => model.toEntity()).toList();
  }

  @override
  Future<void> addToFavorites({required String productCode}) async {
    await _remoteDataSource.addToFavorites(productCode: productCode);
  }

  @override
  Future<void> removeFromFavorites({required String productCode}) async {
    await _remoteDataSource.removeFromFavorites(productCode: productCode);
  }

  // ─── 거래처별 주문 관련 메서드 (F28) ─────────────────────────────

  @override
  Future<ClientOrderListResult> getClientOrders({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  }) async {
    final response = await _remoteDataSource.getClientOrders(
      clientId: clientId,
      deliveryDate: deliveryDate,
      page: page,
      size: size,
    );

    final orders = response.content
        .map((model) => model.toEntity())
        .toList();

    return ClientOrderListResult(
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
  Future<ClientOrderDetail> getClientOrderDetail({
    required String sapOrderNumber,
  }) async {
    final response = await _remoteDataSource.getClientOrderDetail(
      sapOrderNumber: sapOrderNumber,
    );
    return response.toEntity();
  }
}
