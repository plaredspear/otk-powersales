import '../../domain/entities/client_order.dart';
import '../../domain/entities/order.dart';
import '../../domain/entities/order_cancel.dart';
import '../../domain/entities/order_detail.dart';
import '../../domain/entities/order_draft.dart';
import '../../domain/entities/product_for_order.dart';
import '../../domain/entities/validation_error.dart';
import '../../domain/repositories/order_repository.dart';
import '../datasources/order_local_datasource.dart';
import '../datasources/order_remote_datasource.dart';
import '../models/order_cancel_model.dart';
import '../models/order_draft_model.dart';

/// 주문 Repository 구현체
///
/// OrderRemoteDataSource를 사용하여 API를 호출하고,
/// OrderLocalDataSource를 사용하여 로컬 임시저장을 관리합니다.
/// 응답 데이터를 도메인 엔티티로 변환합니다.
class OrderRepositoryImpl implements OrderRepository {
  final OrderRemoteDataSource _remoteDataSource;
  final OrderLocalDataSource _localDataSource;

  OrderRepositoryImpl({
    required OrderRemoteDataSource remoteDataSource,
    required OrderLocalDataSource localDataSource,
  })  : _remoteDataSource = remoteDataSource,
        _localDataSource = localDataSource;

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

  @override
  Future<int> getCreditBalance({required int clientId}) async {
    return await _remoteDataSource.getCreditBalance(clientId: clientId);
  }

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
  Future<ProductForOrder> getProductByBarcode({
    required String barcode,
  }) async {
    final model =
        await _remoteDataSource.getProductByBarcode(barcode: barcode);
    return model.toEntity();
  }

  @override
  Future<void> saveDraftOrder({required OrderDraft orderDraft}) async {
    final model = OrderDraftModel.fromEntity(orderDraft);
    // 로컬에 임시저장 (Hive)
    await _localDataSource.saveDraft(model.toJson());
  }

  @override
  Future<OrderDraft?> loadDraftOrder() async {
    // 로컬에서 임시저장 불러오기
    final json = await _localDataSource.loadDraft();
    if (json == null) return null;

    final model = OrderDraftModel.fromJson(json);
    return model.toEntity();
  }

  @override
  Future<void> deleteDraftOrder() async {
    await _localDataSource.deleteDraft();
  }

  @override
  Future<ValidationResult> validateOrder({
    required OrderDraft orderDraft,
  }) async {
    final model = OrderDraftModel.fromEntity(orderDraft);
    final response = await _remoteDataSource.validateOrder(draft: model);
    return response.toEntity();
  }

  @override
  Future<OrderSubmitResult> submitOrder({
    required OrderDraft orderDraft,
  }) async {
    final model = OrderDraftModel.fromEntity(orderDraft);
    final response = await _remoteDataSource.submitOrder(draft: model);
    return response.toEntity();
  }

  @override
  Future<OrderSubmitResult> updateOrder({
    required int orderId,
    required OrderDraft orderDraft,
  }) async {
    final model = OrderDraftModel.fromEntity(orderDraft);
    final response = await _remoteDataSource.updateOrder(
      orderId: orderId,
      draft: model,
    );
    return response.toEntity();
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
