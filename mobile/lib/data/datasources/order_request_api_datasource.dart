import 'package:dio/dio.dart';

import '../models/client_order_model.dart';
import '../models/order_cancel_model.dart';
import '../models/order_request_detail_model.dart';
import '../models/product_for_order_model.dart';
import 'order_request_remote_datasource.dart';

/// 주문 API 데이터소스 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API와 통신합니다.
class OrderRequestApiDataSource implements OrderRequestRemoteDataSource {
  final Dio _dio;

  OrderRequestApiDataSource(this._dio);

  @override
  Future<OrderRequestListResponseModel> getMyOrderRequests({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
  }) async {
    final queryParameters = <String, dynamic>{
      'sortBy': sortBy,
      'sortDir': sortDir,
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
      '/api/v1/mobile/me/order-requests',
      queryParameters: queryParameters,
    );

    return OrderRequestListResponseModel.fromJson(
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
      '/api/v1/mobile/client-orders',
      queryParameters: queryParameters,
    );

    return ClientOrderListResponseModel.fromJson(
      response.data as Map<String, dynamic>,
    );
  }

  @override
  Future<OrderRequestDetailModel> getOrderRequestDetail({
    required int orderId,
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/me/order-requests/$orderId',
    );

    return OrderRequestDetailModel.fromJson(
      response.data as Map<String, dynamic>,
    );
  }

  @override
  Future<void> resendOrderRequest({required int orderId}) async {
    await _dio.post(
      '/api/v1/mobile/me/order-requests/$orderId/resend',
    );
  }

  @override
  Future<OrderCancelResponseModel> cancelOrderRequest({
    required int orderId,
    required OrderCancelRequestModel request,
  }) async {
    final response = await _dio.post(
      '/api/v1/mobile/me/order-requests/$orderId/cancel',
      data: request.toJson(),
      options: Options(
        // 주문 취소는 백엔드가 SAP(SD03051)를 동기 1회 호출한다 — 취소 라인 전체를 단일
        // payload 배열로 묶어 보내므로 라인 수와 무관하게 1회. 백엔드 read-timeout(30초) +
        // DB 커밋/검증 여유를 더해 45초로 잡는다. 전역 Dio receiveTimeout(35초)은 SAP
        // 정상 처리(관측 8~10초)에도 마진이 얇아, 이 요청에만 per-request 상향한다.
        // (등록은 SAP 동기 다회라 100초 — order_form_api_datasource 참고. 취소는 1회라 45초.)
        receiveTimeout: const Duration(seconds: 45),
      ),
    );

    return OrderCancelResponseModel.fromJson(
      response.data as Map<String, dynamic>,
    );
  }

  @override
  Future<List<ProductForOrderModel>> getFavoriteProducts() async {
    final response = await _dio.get(
      '/api/v1/mobile/me/favorite-products',
    );

    final data = (response.data['data'] as List<dynamic>?) ?? const [];
    return data
        .map((raw) =>
            ProductForOrderModel.fromJson(raw as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<List<ProductForOrderModel>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) async {
    final queryParameters = <String, dynamic>{
      'query': query,
      'page': 0,
      'size': 30,
    };
    if (categoryMid != null && categoryMid.isNotEmpty) {
      queryParameters['categoryMid'] = categoryMid;
    }
    if (categorySub != null && categorySub.isNotEmpty) {
      queryParameters['categorySub'] = categorySub;
    }

    final response = await _dio.get(
      '/api/v1/mobile/products/search/order',
      queryParameters: queryParameters,
    );

    final page = response.data['data'] as Map<String, dynamic>;
    final content = (page['content'] as List<dynamic>?) ?? const [];
    return content
        .map((raw) =>
            ProductForOrderModel.fromJson(raw as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<void> addToFavorites({required String productCode}) async {
    await _dio.post(
      '/api/v1/mobile/me/favorite-products/$productCode',
    );
  }

  @override
  Future<void> removeFromFavorites({required String productCode}) async {
    await _dio.delete(
      '/api/v1/mobile/me/favorite-products/$productCode',
    );
  }

  @override
  Future<ClientOrderDetailModel> getClientOrderDetail({
    required String sapOrderNumber,
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/client-orders/$sapOrderNumber',
    );
    return ClientOrderDetailModel.fromJson(
      response.data as Map<String, dynamic>,
    );
  }
}
