import 'product_for_order.dart';

/// 주문용 제품 검색 결과 — 조회된 목록 + 서버가 집계한 전체 건수.
///
/// 주문서 제품검색은 페이징 UI 없이 1회 조회분(상한 [pageLimit])만 노출하므로,
/// 전체 건수가 상한을 넘었는지 판별해 "검색어를 좁혀 달라" 안내를 띄우는 데 사용한다.
class ProductSearchResult {
  /// 조회된 제품 목록 (최대 [pageLimit] 건)
  final List<ProductForOrder> products;

  /// 검색 조건에 매칭되는 전체 건수 (서버 집계값 — 목록 길이와 다를 수 있음)
  final int totalCount;

  /// 1회 조회 상한 — 이 값을 넘는 전체 건수는 목록에 담기지 않는다.
  final int pageLimit;

  const ProductSearchResult({
    required this.products,
    required this.totalCount,
    required this.pageLimit,
  });

  const ProductSearchResult.empty()
      : products = const [],
        totalCount = 0,
        pageLimit = 0;

  /// 전체 건수가 1회 조회 상한을 초과해 일부만 노출되고 있는지 여부.
  bool get isTruncated => totalCount > pageLimit;
}
