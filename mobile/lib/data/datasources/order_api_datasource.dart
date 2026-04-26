import 'package:dio/dio.dart';

import '../models/client_order_model.dart';
import '../models/order_cancel_model.dart';
import '../models/order_detail_model.dart';
import '../models/order_draft_model.dart';
import '../models/product_for_order_model.dart';
import '../models/validation_result_model.dart';
import 'order_remote_datasource.dart';

/// 주문 API 데이터소스 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API와 통신합니다.
class OrderApiDataSource implements OrderRemoteDataSource {
  final Dio _dio;

  OrderApiDataSource(this._dio);

  @override
  Future<OrderListResponseModel> getMyOrders({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
    int page = 0,
    int size = 20,
  }) async {
    final queryParameters = <String, dynamic>{
      'sortBy': sortBy,
      'sortDir': sortDir,
      'page': page,
      'size': size,
    };

    if (clientId != null) queryParameters['clientId'] = clientId;
    if (status != null) queryParameters['status'] = status;
    if (deliveryDateFrom != null) {
      queryParameters['deliveryDateFrom'] = deliveryDateFrom;
    }
    if (deliveryDateTo != null) {
      queryParameters['deliveryDateTo'] = deliveryDateTo;
    }

    final response = await _dio.get(
      '/api/v1/me/orders',
      queryParameters: queryParameters,
    );

    return OrderListResponseModel.fromJson(
      response.data as Map<String, dynamic>,
    );
  }

  @override
  Future<ClientOrderListResponseModel> getClientOrders({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  }) async {
    final queryParameters = <String, dynamic>{
      'clientId': clientId,
      'page': page,
      'size': size,
    };

    if (deliveryDate != null) {
      queryParameters['deliveryDate'] = deliveryDate;
    }

    final response = await _dio.get(
      '/api/v1/client-orders',
      queryParameters: queryParameters,
    );

    return ClientOrderListResponseModel.fromJson(
      response.data as Map<String, dynamic>,
    );
  }

  @override
  Future<OrderDetailModel> getOrderDetail({required int orderId}) {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<void> resendOrder({required int orderId}) {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<OrderCancelResponseModel> cancelOrder({
    required int orderId,
    required OrderCancelRequestModel request,
  }) {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<int> getCreditBalance({required int clientId}) {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<List<ProductForOrderModel>> getFavoriteProducts() {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<List<ProductForOrderModel>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<ProductForOrderModel> getProductByBarcode({required String barcode}) {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<void> saveDraftOrder({required OrderDraftModel draft}) {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<OrderDraftModel?> loadDraftOrder() {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<ValidationResultModel> validateOrder({
    required OrderDraftModel draft,
  }) {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<OrderSubmitResultModel> submitOrder({
    required OrderDraftModel draft,
  }) {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<OrderSubmitResultModel> updateOrder({
    required int orderId,
    required OrderDraftModel draft,
  }) {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<void> addToFavorites({required String productCode}) {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<void> removeFromFavorites({required String productCode}) {
    throw UnimplementedError('별도 스펙에서 구현');
  }

  @override
  Future<ClientOrderDetailModel> getClientOrderDetail({
    required String sapOrderNumber,
  }) {
    throw UnimplementedError('별도 스펙에서 구현');
  }
}
