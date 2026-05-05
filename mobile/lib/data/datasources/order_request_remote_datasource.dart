import '../models/client_order_model.dart';
import '../models/order_cancel_model.dart';
import '../models/order_request_detail_model.dart';
import '../models/order_request_model.dart';
import '../models/order_draft_model.dart';
import '../models/product_for_order_model.dart';
import '../models/validation_result_model.dart';

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

  /// GET /api/v1/mobile/accounts/{accountId}/credit
  ///
  /// 거래처 여신 잔액을 조회합니다.
  Future<int> getCreditBalance({required int clientId});

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

  /// GET /api/v1/mobile/products/barcode/{barcode}
  ///
  /// 바코드로 제품을 조회합니다.
  Future<ProductForOrderModel> getProductByBarcode({required String barcode});

  /// POST /api/v1/mobile/me/orders/draft
  ///
  /// 주문서를 임시저장합니다 (서버 동기화).
  Future<void> saveDraftOrder({required OrderDraftModel draft});

  /// GET /api/v1/mobile/me/orders/draft
  ///
  /// 서버에 임시저장된 주문서를 조회합니다.
  Future<OrderDraftModel?> loadDraftOrder();

  /// POST /api/v1/mobile/me/orders/validate
  ///
  /// 주문서 유효성을 검증합니다.
  Future<ValidationResultModel> validateOrder({
    required OrderDraftModel draft,
  });

  /// POST /api/v1/mobile/me/orders
  ///
  /// 주문서를 전송합니다 (승인요청).
  Future<OrderSubmitResultModel> submitOrder({
    required OrderDraftModel draft,
  });

  /// PUT /api/v1/mobile/me/orders/{orderId}
  ///
  /// 기존 주문서를 수정합니다.
  Future<OrderSubmitResultModel> updateOrder({
    required int orderId,
    required OrderDraftModel draft,
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
