import '../entities/client_order.dart';
import '../entities/order.dart';
import '../entities/order_cancel.dart';
import '../entities/order_detail.dart';
import '../entities/order_draft.dart';
import '../entities/product_for_order.dart';
import '../entities/validation_error.dart';

/// 주문 목록 조회 결과 값 객체
///
/// 페이지네이션 정보를 포함한 주문 목록 결과를 담는 도메인 레벨 값 객체입니다.
class OrderListResult {
  /// 주문 목록
  final List<Order> orders;

  /// 전체 결과 수
  final int totalElements;

  /// 전체 페이지 수
  final int totalPages;

  /// 현재 페이지 번호 (0부터 시작)
  final int currentPage;

  /// 페이지 크기
  final int pageSize;

  /// 첫 번째 페이지 여부
  final bool isFirst;

  /// 마지막 페이지 여부
  final bool isLast;

  const OrderListResult({
    required this.orders,
    required this.totalElements,
    required this.totalPages,
    required this.currentPage,
    required this.pageSize,
    required this.isFirst,
    required this.isLast,
  });

  /// 다음 페이지가 있는지 여부
  bool get hasNextPage => !isLast;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! OrderListResult) return false;
    if (other.totalElements != totalElements) return false;
    if (other.totalPages != totalPages) return false;
    if (other.currentPage != currentPage) return false;
    if (other.pageSize != pageSize) return false;
    if (other.isFirst != isFirst) return false;
    if (other.isLast != isLast) return false;
    if (other.orders.length != orders.length) return false;
    for (var i = 0; i < orders.length; i++) {
      if (other.orders[i] != orders[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      Object.hashAll(orders),
      totalElements,
      totalPages,
      currentPage,
      pageSize,
      isFirst,
      isLast,
    );
  }

  @override
  String toString() {
    return 'OrderListResult(orders: ${orders.length}, '
        'totalElements: $totalElements, totalPages: $totalPages, '
        'currentPage: $currentPage, pageSize: $pageSize, '
        'isFirst: $isFirst, isLast: $isLast)';
  }
}

/// 주문 Repository 인터페이스
///
/// 주문 관련 데이터 접근을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class OrderRepository {
  /// 내 주문 목록 조회
  ///
  /// [clientId]: 거래처 ID (미입력 시 전체)
  /// [status]: 승인상태 코드 (미입력 시 전체)
  /// [deliveryDateFrom]: 납기일 시작 (YYYY-MM-DD)
  /// [deliveryDateTo]: 납기일 종료 (YYYY-MM-DD)
  /// [sortBy]: 정렬 기준 (기본: orderDate)
  /// [sortDir]: 정렬 방향 (기본: DESC)
  /// [page]: 페이지 번호 (0부터 시작)
  /// [size]: 페이지 크기 (기본: 20)
  ///
  /// Returns: 페이지네이션 정보를 포함한 주문 목록
  Future<OrderListResult> getMyOrders({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
    int page = 0,
    int size = 20,
  });

  /// 주문 상세 조회
  ///
  /// [orderId]: 주문 고유 ID
  ///
  /// Returns: 주문 상세 정보 (주문정보 + 제품목록 + 처리현황 + 반려제품)
  Future<OrderDetail> getOrderDetail({required int orderId});

  /// 주문 재전송
  ///
  /// [orderId]: 재전송할 주문 ID
  /// 전송실패 상태의 주문을 재전송합니다.
  Future<void> resendOrder({required int orderId});

  /// 주문 취소
  ///
  /// [orderId]: 취소할 주문 ID
  /// [productCodes]: 취소할 제품코드 목록
  ///
  /// Returns: 취소 결과 (취소된 제품 수, 취소된 제품코드 목록)
  Future<OrderCancelResult> cancelOrder({
    required int orderId,
    required List<String> productCodes,
  });

  // ─── 주문서 작성 관련 메서드 (F22) ─────────────────────────────

  /// 거래처 여신 잔액 조회
  ///
  /// [clientId]: 거래처 ID
  /// Returns: 여신 잔액 (원)
  Future<int> getCreditBalance({required int clientId});

  /// 즐겨찾기 제품 목록 조회
  ///
  /// Returns: 즐겨찾기 등록된 제품 목록
  Future<List<ProductForOrder>> getFavoriteProducts();

  /// 제품 검색
  ///
  /// [query]: 검색어 (제품명 또는 제품코드)
  /// [categoryMid]: 중분류 카테고리 (선택)
  /// [categorySub]: 소분류 카테고리 (선택)
  /// Returns: 검색 결과 제품 목록
  Future<List<ProductForOrder>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  });

  /// 바코드로 제품 조회
  ///
  /// [barcode]: 바코드 문자열
  /// Returns: 해당 바코드의 제품 정보
  Future<ProductForOrder> getProductByBarcode({required String barcode});

  /// 주문서 임시저장 (로컬)
  ///
  /// [orderDraft]: 저장할 주문서 초안
  Future<void> saveDraftOrder({required OrderDraft orderDraft});

  /// 임시저장 주문서 불러오기
  ///
  /// Returns: 임시저장된 주문서 초안 (없으면 null)
  Future<OrderDraft?> loadDraftOrder();

  /// 임시저장 주문서 삭제
  Future<void> deleteDraftOrder();

  /// 주문서 유효성 검증
  ///
  /// [orderDraft]: 검증할 주문서 초안
  /// Returns: 유효성 검증 결과
  Future<ValidationResult> validateOrder({required OrderDraft orderDraft});

  /// 주문서 전송 (승인요청)
  ///
  /// [orderDraft]: 전송할 주문서 초안
  /// Returns: 전송 결과 (주문 ID, 요청번호, 상태)
  Future<OrderSubmitResult> submitOrder({required OrderDraft orderDraft});

  /// 주문서 수정
  ///
  /// [orderId]: 수정할 주문 ID
  /// [orderDraft]: 수정된 주문서 초안
  /// Returns: 수정 결과 (주문 ID, 요청번호, 상태)
  Future<OrderSubmitResult> updateOrder({
    required int orderId,
    required OrderDraft orderDraft,
  });

  /// 즐겨찾기 추가
  ///
  /// [productCode]: 즐겨찾기에 추가할 제품코드
  Future<void> addToFavorites({required String productCode});

  /// 즐겨찾기 삭제
  ///
  /// [productCode]: 즐겨찾기에서 삭제할 제품코드
  Future<void> removeFromFavorites({required String productCode});

  // ─── 거래처별 주문 관련 메서드 (F28) ─────────────────────────────

  /// 거래처별 주문 목록 조회
  ///
  /// [clientId]: 거래처 ID (필수)
  /// [deliveryDate]: 납기일 (YYYY-MM-DD, 기본: 오늘)
  /// [page]: 페이지 번호 (0부터 시작)
  /// [size]: 페이지 크기 (기본: 20)
  ///
  /// Returns: 페이지네이션 정보를 포함한 거래처별 주문 목록
  Future<ClientOrderListResult> getClientOrders({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  });

  /// 거래처별 주문 상세 조회
  ///
  /// [sapOrderNumber]: SAP 주문번호
  ///
  /// Returns: 거래처별 주문 상세 정보
  Future<ClientOrderDetail> getClientOrderDetail({
    required String sapOrderNumber,
  });
}
