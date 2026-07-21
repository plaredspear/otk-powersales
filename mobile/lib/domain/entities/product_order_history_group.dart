import 'product_for_order.dart';

/// 거래처 주문이력 그룹 (주문일 기준).
///
/// 제품추가 모달 "주문 이력" 탭에서 거래처+기간으로 조회한 본인 주문이력을
/// 주문일별로 묶은 도메인 엔티티. 각 제품은 제품검색/즐겨찾기와 동일한
/// [ProductForOrder] 형상이라 이력에서 바로 주문 라인으로 담을 수 있다.
class ProductOrderHistoryGroup {
  /// 주문일 (YYYY-MM-DD).
  final String orderDate;
  final List<ProductForOrder> products;

  const ProductOrderHistoryGroup({
    required this.orderDate,
    required this.products,
  });
}
