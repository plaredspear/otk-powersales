import '../../domain/entities/order.dart';
import '../../domain/entities/order_cancel.dart';
import '../../domain/entities/order_detail.dart';
import '../../domain/entities/order_draft.dart';
import '../../domain/entities/product_for_order.dart';
import '../../domain/entities/validation_error.dart';
import '../../domain/repositories/order_repository.dart';
import '../datasources/order_remote_datasource.dart';
import '../models/order_cancel_model.dart';

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

  @override
  Future<OrderCancelResult> cancelOrder({
    required int orderId,
    required List<String> productCodes,
  }) async {
    final requestModel = OrderCancelRequestModel(
      productCodes: productCodes,
    );
    final response = await _remoteDataSource.cancelOrder(
      orderId: orderId,
      request: requestModel,
    );
    return response.toEntity();
  }

  // ─── 주문서 작성 관련 메서드 (F22) ─────────────────────────────
  // TODO: Backend API 구현 후 실제 API 호출로 대체

  @override
  Future<int> getCreditBalance({required int clientId}) async {
    throw UnimplementedError('Backend API 구현 후 연동 예정');
  }

  @override
  Future<List<ProductForOrder>> getFavoriteProducts() async {
    throw UnimplementedError('Backend API 구현 후 연동 예정');
  }

  @override
  Future<List<ProductForOrder>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) async {
    throw UnimplementedError('Backend API 구현 후 연동 예정');
  }

  @override
  Future<ProductForOrder> getProductByBarcode({required String barcode}) async {
    throw UnimplementedError('Backend API 구현 후 연동 예정');
  }

  @override
  Future<void> saveDraftOrder({required OrderDraft orderDraft}) async {
    throw UnimplementedError('Backend API 구현 후 연동 예정');
  }

  @override
  Future<OrderDraft?> loadDraftOrder() async {
    throw UnimplementedError('Backend API 구현 후 연동 예정');
  }

  @override
  Future<void> deleteDraftOrder() async {
    throw UnimplementedError('Backend API 구현 후 연동 예정');
  }

  @override
  Future<ValidationResult> validateOrder({
    required OrderDraft orderDraft,
  }) async {
    throw UnimplementedError('Backend API 구현 후 연동 예정');
  }

  @override
  Future<OrderSubmitResult> submitOrder({
    required OrderDraft orderDraft,
  }) async {
    throw UnimplementedError('Backend API 구현 후 연동 예정');
  }

  @override
  Future<OrderSubmitResult> updateOrder({
    required int orderId,
    required OrderDraft orderDraft,
  }) async {
    throw UnimplementedError('Backend API 구현 후 연동 예정');
  }

  @override
  Future<void> addToFavorites({required String productCode}) async {
    throw UnimplementedError('Backend API 구현 후 연동 예정');
  }

  @override
  Future<void> removeFromFavorites({required String productCode}) async {
    throw UnimplementedError('Backend API 구현 후 연동 예정');
  }
}
