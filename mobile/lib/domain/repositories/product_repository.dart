import '../entities/product.dart';

/// 제품검색 결과 값 객체
///
/// 페이지네이션 정보를 포함한 검색 결과를 담는 도메인 레벨 값 객체입니다.
class ProductSearchResult {
  /// 검색된 제품 목록
  final List<Product> products;

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

  const ProductSearchResult({
    required this.products,
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
    if (other is! ProductSearchResult) return false;
    if (other.totalElements != totalElements) return false;
    if (other.totalPages != totalPages) return false;
    if (other.currentPage != currentPage) return false;
    if (other.pageSize != pageSize) return false;
    if (other.isFirst != isFirst) return false;
    if (other.isLast != isLast) return false;
    if (other.products.length != products.length) return false;
    for (var i = 0; i < products.length; i++) {
      if (other.products[i] != products[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      Object.hashAll(products),
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
    return 'ProductSearchResult(products: ${products.length}, '
        'totalElements: $totalElements, totalPages: $totalPages, '
        'currentPage: $currentPage, pageSize: $pageSize, '
        'isFirst: $isFirst, isLast: $isLast)';
  }
}

/// 제품검색 Repository 인터페이스
///
/// 제품 검색 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class ProductRepository {
  /// 제품 검색
  ///
  /// [query]: 검색어 (제품명, 제품코드, 바코드)
  /// [type]: 검색 유형 ('text' 또는 'barcode')
  /// [page]: 페이지 번호 (0부터 시작)
  /// [size]: 페이지 크기 (기본 20)
  ///
  /// Returns: 페이지네이션 정보를 포함한 검색 결과
  Future<ProductSearchResult> searchProducts({
    required String query,
    String type = 'text',
    int page = 0,
    int size = 20,
  });
}
