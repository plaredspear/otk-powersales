import '../entities/client_order.dart';
import '../entities/order_request.dart';
import '../entities/order_cancel.dart';
import '../entities/order_detail.dart';
import '../entities/product_for_order.dart';
import '../entities/product_order_history_group.dart';

/// 본인 주문요청 목록 조회 결과 값 객체 (클라이언트 슬라이스 패턴).
///
/// 백엔드는 페이징 없이 전체 배열 + 메타를 반환한다.
class OrderRequestListResult {
  /// 검색 조건에 매칭된 모든 주문요청 (페이징 없음)
  final List<OrderRequest> orders;

  /// orders.length — 클라이언트 페이지 계산용
  final int total;

  /// 결과가 응답 라인 수 상한(2000건)에 도달해 잘렸는지 여부
  final bool truncated;

  /// 서버 조회 시각
  final DateTime fetchedAt;

  const OrderRequestListResult({
    required this.orders,
    required this.total,
    required this.truncated,
    required this.fetchedAt,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! OrderRequestListResult) return false;
    if (other.total != total) return false;
    if (other.truncated != truncated) return false;
    if (other.fetchedAt != fetchedAt) return false;
    if (other.orders.length != orders.length) return false;
    for (var i = 0; i < orders.length; i++) {
      if (other.orders[i] != orders[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode => Object.hash(Object.hashAll(orders), total, truncated, fetchedAt);

  @override
  String toString() => 'OrderRequestListResult(orders: ${orders.length}, '
      'total: $total, truncated: $truncated, fetchedAt: $fetchedAt)';
}

/// 주문 Repository 인터페이스
///
/// 주문 관련 데이터 접근을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class OrderRequestRepository {
  /// 본인 주문요청 목록 조회 (페이징 없음 — 클라이언트 슬라이스).
  ///
  /// [clientId]: 거래처 ID (미입력 시 전체)
  /// [status]: 주문요청 상태 코드 (미입력 시 전체)
  /// [deliveryDateFrom]: 납기일 시작 (YYYY-MM-DD, 필수)
  /// [deliveryDateTo]: 납기일 종료 (YYYY-MM-DD, 필수, 7일 한도)
  /// [sortBy]: 정렬 기준 (기본: orderDate)
  /// [sortDir]: 정렬 방향 (기본: DESC)
  Future<OrderRequestListResult> getMyOrderRequests({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
  });

  /// 주문 상세 조회
  ///
  /// [orderId]: 주문 고유 ID
  ///
  /// Returns: 주문 상세 정보 (주문정보 + 제품목록 + 처리현황 + 반려제품)
  Future<OrderDetail> getOrderRequestDetail({required int orderId});

  /// 주문 재전송
  ///
  /// [orderId]: 재전송할 주문 ID
  /// 전송실패 상태의 주문을 재전송합니다.
  Future<void> resendOrderRequest({required int orderId});

  /// 주문 취소
  ///
  /// [orderId]: 취소할 주문 ID
  /// [orderProductIds]: 취소할 주문 라인 PK 목록 (빈 배열이면 전체 취소)
  ///
  /// Returns: 취소 결과 (취소 후 주문 상태, 취소된 라인 목록)
  Future<OrderCancelResult> cancelOrderRequest({
    required int orderId,
    required List<int> orderProductIds,
  });

  // ─── 주문서 작성 관련 메서드 (F22) ─────────────────────────────
  //
  // NOTE: 거래처 여신/임시저장/검증/제출/수정/바코드는 신규 OrderFormRepository
  // (#592/#594/#596) 경로로 대체되어 제거됨. 여기에는 add_product 화면이 사용하는
  // 즐겨찾기/검색만 남는다. 즐겨찾기는 /api/v1/mobile/me/favorite-products 로 풀스택 구현됨.

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

  /// 즐겨찾기 추가
  ///
  /// [productCode]: 즐겨찾기에 추가할 제품코드
  Future<void> addToFavorites({required String productCode});

  /// 즐겨찾기 삭제
  ///
  /// [productCode]: 즐겨찾기에서 삭제할 제품코드
  Future<void> removeFromFavorites({required String productCode});

  /// 거래처 주문이력 조회 (제품 선택용)
  ///
  /// [accountId]: 거래처 내부 ID (Account.id)
  /// [startDate]/[endDate]: 주문일 기간
  /// Returns: 주문일별 그룹(최신순)
  Future<List<ProductOrderHistoryGroup>> getAccountOrderHistory({
    required int accountId,
    required DateTime startDate,
    required DateTime endDate,
  });

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
