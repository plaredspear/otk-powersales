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
      options: Options(
        // 주문 상세 조회는 백엔드가 SAP(SD03052)를 동기 1회 호출한다(백엔드는 SAP 실패 시
        // null fallback 으로 200 유지). 백엔드 read-timeout(30초) + DB/네트워크 여유를 더해
        // 45초로 잡는다. 전역 Dio receiveTimeout(35초)은 마진이 얇아 서버 처리 중 false
        // timeout 이 나면 상세를 못 받으므로, 이 요청에만 per-request 상향한다. (취소/여신
        // 동기 1회와 동일 근거 — 45초.)
        receiveTimeout: const Duration(seconds: 45),
      ),
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
        // payload 배열로 묶어 보내므로 라인 수와 무관하게 1회.
        //
        // 클라이언트 timeout 이 백엔드보다 먼저 끊기면, 백엔드/SAP 는 처리를 계속 진행해
        // 취소가 완료되는데 앱만 실패로 표시된다 → 사용자 재취소 → SAP 중복 전송. 이를 막기
        // 위해 클라이언트 receiveTimeout 은 백엔드 SAP read-timeout(300초) 이상이어야 하므로
        // 300초로 정합시킨다. 백엔드가 SAP 응답을 받아 commit 한 최종 성공/실패를 앱이 끝까지
        // 수신하도록 보장한다. (전역 Dio receiveTimeout(35초)은 SAP 지연 시 false timeout 을
        // 내므로 이 요청에만 per-request 상향.)
        receiveTimeout: const Duration(seconds: 300),
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
