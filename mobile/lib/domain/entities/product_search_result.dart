import 'product_for_order.dart';

/// 주문용 제품 검색 결과 한 페이지 — 조회된 목록 + 서버가 집계한 전체 건수.
///
/// 주문서 제품검색은 무한스크롤로 전건을 조회하므로, [hasMore] 로 다음 페이지
/// 존재 여부를 판별해 추가 로드를 이어간다.
class ProductSearchResult {
  /// 이번 페이지에서 조회된 제품 목록
  final List<ProductForOrder> products;

  /// 검색 조건에 매칭되는 전체 건수 (서버 집계값)
  final int totalCount;

  /// 이번 응답의 페이지 번호 (0-based)
  final int page;

  /// 지금까지 누적 로드된 건수 — [hasMore] 판정 기준.
  /// 첫 페이지면 [products] 길이와 같고, 이어붙일 때는 누적값을 넘겨준다.
  final int loadedCount;

  const ProductSearchResult({
    required this.products,
    required this.totalCount,
    required this.page,
    required this.loadedCount,
  });

  const ProductSearchResult.empty()
      : products = const [],
        totalCount = 0,
        page = 0,
        loadedCount = 0;

  /// 아직 서버에 남은 제품이 있는지 여부.
  ///
  /// 빈 페이지를 받으면(서버 집계와 실제 행 수가 어긋나는 경우) false 로 떨어져
  /// 무한 재요청을 막는다.
  bool get hasMore => products.isNotEmpty && loadedCount < totalCount;
}
