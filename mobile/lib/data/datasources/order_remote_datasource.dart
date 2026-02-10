import '../models/order_cancel_model.dart';
import '../models/order_detail_model.dart';
import '../models/order_model.dart';
import '../models/order_draft_model.dart';
import '../models/product_for_order_model.dart';
import '../models/validation_result_model.dart';

/// 주문 API 응답 데이터를 담는 모델
///
/// 페이지네이션을 포함한 주문 목록 응답을 파싱합니다.
class OrderListResponseModel {
  final List<OrderModel> content;
  final int totalElements;
  final int totalPages;
  final int number;
  final int size;
  final bool first;
  final bool last;

  const OrderListResponseModel({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.number,
    required this.size,
    required this.first,
    required this.last,
  });

  /// API 응답 JSON에서 파싱
  factory OrderListResponseModel.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;
    final contentJson = data['content'] as List<dynamic>? ?? [];
    final content = contentJson
        .map((e) => OrderModel.fromJson(e as Map<String, dynamic>))
        .toList();

    return OrderListResponseModel(
      content: content,
      totalElements: data['total_elements'] as int,
      totalPages: data['total_pages'] as int,
      number: data['number'] as int,
      size: data['size'] as int,
      first: data['first'] as bool,
      last: data['last'] as bool,
    );
  }
}

/// 주문 API DataSource 인터페이스
///
/// 주문 관련 API 호출을 추상화합니다.
abstract class OrderRemoteDataSource {
  /// GET /api/v1/me/orders
  ///
  /// 내 주문 목록을 조회합니다.
  Future<OrderListResponseModel> getMyOrders({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
    int page = 0,
    int size = 20,
  });

  /// GET /api/v1/me/orders/{orderId}
  ///
  /// 주문 상세 정보를 조회합니다.
  Future<OrderDetailModel> getOrderDetail({required int orderId});

  /// POST /api/v1/me/orders/{orderId}/resend
  ///
  /// 전송실패 주문을 재전송합니다.
  Future<void> resendOrder({required int orderId});

  /// POST /api/v1/me/orders/{orderId}/cancel
  ///
  /// 선택한 제품의 주문을 취소합니다.
  Future<OrderCancelResponseModel> cancelOrder({
    required int orderId,
    required OrderCancelRequestModel request,
  });

  // ─── 주문서 작성 관련 API (F22) ─────────────────────────────

  /// GET /api/v1/stores/{storeId}/credit
  ///
  /// 거래처 여신 잔액을 조회합니다.
  Future<int> getCreditBalance({required int clientId});

  /// GET /api/v1/products/favorites
  ///
  /// 즐겨찾기 제품 목록을 조회합니다.
  Future<List<ProductForOrderModel>> getFavoriteProducts();

  /// GET /api/v1/products/search
  ///
  /// 제품을 검색합니다 (중분류/소분류/제품명/제품코드).
  Future<List<ProductForOrderModel>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  });

  /// GET /api/v1/products/barcode/{barcode}
  ///
  /// 바코드로 제품을 조회합니다.
  Future<ProductForOrderModel> getProductByBarcode({required String barcode});

  /// POST /api/v1/me/orders/draft
  ///
  /// 주문서를 임시저장합니다 (서버 동기화).
  Future<void> saveDraftOrder({required OrderDraftModel draft});

  /// GET /api/v1/me/orders/draft
  ///
  /// 서버에 임시저장된 주문서를 조회합니다.
  Future<OrderDraftModel?> loadDraftOrder();

  /// POST /api/v1/me/orders/validate
  ///
  /// 주문서 유효성을 검증합니다.
  Future<ValidationResultModel> validateOrder({
    required OrderDraftModel draft,
  });

  /// POST /api/v1/me/orders
  ///
  /// 주문서를 전송합니다 (승인요청).
  Future<OrderSubmitResultModel> submitOrder({
    required OrderDraftModel draft,
  });

  /// PUT /api/v1/me/orders/{orderId}
  ///
  /// 기존 주문서를 수정합니다.
  Future<OrderSubmitResultModel> updateOrder({
    required int orderId,
    required OrderDraftModel draft,
  });

  /// POST /api/v1/products/{productId}/favorite
  ///
  /// 제품을 즐겨찾기에 추가합니다.
  Future<void> addToFavorites({required String productCode});

  /// DELETE /api/v1/products/{productId}/favorite
  ///
  /// 제품을 즐겨찾기에서 삭제합니다.
  Future<void> removeFromFavorites({required String productCode});
}
