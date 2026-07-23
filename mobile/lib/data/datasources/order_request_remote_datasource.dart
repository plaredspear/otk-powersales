import '../models/client_order_model.dart';
import '../models/order_cancel_model.dart';
import '../models/order_history_group_model.dart';
import '../models/order_request_detail_model.dart';
import '../models/order_request_model.dart';
import '../models/product_for_order_model.dart';

/// 주문요청 목록 응답 모델 (클라이언트 슬라이스 패턴).
///
/// 백엔드는 페이징 없이 전체 배열 + 메타를 반환하고, 모바일이 받아서 슬라이스한다.
class OrderRequestListResponseModel {
  final List<OrderRequestModel> items;
  final int total;
  final bool truncated;
  final DateTime fetchedAt;

  const OrderRequestListResponseModel({
    required this.items,
    required this.total,
    required this.truncated,
    required this.fetchedAt,
  });

  /// API 응답 JSON에서 파싱
  factory OrderRequestListResponseModel.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;
    final itemsJson = data['items'] as List<dynamic>? ?? [];
    final items = itemsJson
        .map((e) => OrderRequestModel.fromJson(e as Map<String, dynamic>))
        .toList();

    return OrderRequestListResponseModel(
      items: items,
      total: data['total'] as int,
      truncated: data['truncated'] as bool,
      fetchedAt: DateTime.parse(data['fetchedAt'] as String),
    );
  }
}

/// 주문 API DataSource 인터페이스
///
/// 주문 관련 API 호출을 추상화합니다.
abstract class OrderRequestRemoteDataSource {
  /// GET /api/v1/mobile/me/order-requests
  ///
  /// 내 주문요청 목록을 조회합니다 (페이징 없음 — 클라이언트 슬라이스).
  Future<OrderRequestListResponseModel> getMyOrderRequests({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
  });

  /// GET /api/v1/mobile/me/order-requests/{orderId}
  ///
  /// 주문요청 상세 정보를 조회합니다.
  Future<OrderRequestDetailModel> getOrderRequestDetail({required int orderId});

  /// POST /api/v1/mobile/me/order-requests/{orderId}/resend
  ///
  /// 전송실패 주문요청을 재전송합니다.
  Future<void> resendOrderRequest({required int orderId});

  /// POST /api/v1/mobile/me/order-requests/{orderId}/cancel
  ///
  /// 선택한 제품의 주문을 취소합니다.
  Future<OrderCancelResponseModel> cancelOrderRequest({
    required int orderId,
    required OrderCancelRequestModel request,
  });

  // ─── 주문서 작성 관련 API (F22) ─────────────────────────────
  //
  // NOTE: 여신/임시저장/검증/제출/수정/바코드는 신규 OrderFormRemoteDataSource
  // (#592/#594/#596) 로 대체되어 제거됨. 즐겨찾기/검색만 유지.

  /// GET /api/v1/mobile/products/favorites
  ///
  /// 즐겨찾기 제품 목록을 조회합니다.
  Future<List<ProductForOrderModel>> getFavoriteProducts();

  /// GET /api/v1/mobile/products/search
  ///
  /// 제품을 검색합니다 (중분류/소분류/제품명/제품코드).
  Future<List<ProductForOrderModel>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  });

  /// GET /api/v1/mobile/me/order-requests/product-history
  ///
  /// 거래처(accountId) + 주문일 기간으로 본인 주문이력을 주문일별 그룹으로 조회합니다.
  Future<List<OrderHistoryGroupModel>> getAccountOrderHistory({
    required int accountId,
    required String startDate,
    required String endDate,
  });

  /// POST /api/v1/mobile/products/{productId}/favorite
  ///
  /// 제품을 즐겨찾기에 추가합니다.
  Future<void> addToFavorites({required String productCode});

  /// DELETE /api/v1/mobile/products/{productId}/favorite
  ///
  /// 제품을 즐겨찾기에서 삭제합니다.
  Future<void> removeFromFavorites({required String productCode});

  // ─── 거래처별 주문 관련 API (F28) ─────────────────────────────

  /// GET /api/v1/mobile/client-orders
  ///
  /// 거래처별 주문 목록을 조회합니다.
  Future<ClientOrderListResponseModel> getClientOrders({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  });

  /// GET /api/v1/mobile/client-orders/{sapOrderNumber}
  ///
  /// 거래처별 주문 상세를 조회합니다.
  Future<ClientOrderDetailModel> getClientOrderDetail({
    required String sapOrderNumber,
  });
}
