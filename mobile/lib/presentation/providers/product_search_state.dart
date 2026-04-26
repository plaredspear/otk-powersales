import '../../domain/entities/product.dart';

/// 검색 유형
enum SearchType {
  /// 텍스트 검색 (제품명, 제품코드, 바코드)
  text,

  /// 바코드 스캔 검색
  barcode,
}

/// 제품검색 화면 상태
class ProductSearchState {
  /// 로딩 상태
  final bool isLoading;

  /// 추가 페이지 로딩 상태
  final bool isLoadingMore;

  /// 에러 메시지
  final String? errorMessage;

  /// 현재 검색어
  final String query;

  /// 검색 유형 (text / barcode)
  final SearchType searchType;

  /// 검색 결과 제품 목록
  final List<Product> products;

  /// 전체 결과 수
  final int totalElements;

  /// 현재 페이지 번호
  final int currentPage;

  /// 마지막 페이지 여부
  final bool isLastPage;

  /// 검색 실행 여부 (빈 결과 vs 초기 상태 구분)
  final bool hasSearched;

  const ProductSearchState({
    this.isLoading = false,
    this.isLoadingMore = false,
    this.errorMessage,
    this.query = '',
    this.searchType = SearchType.text,
    this.products = const [],
    this.totalElements = 0,
    this.currentPage = 0,
    this.isLastPage = false,
    this.hasSearched = false,
  });

  /// 초기 상태
  factory ProductSearchState.initial() {
    return const ProductSearchState();
  }

  /// 로딩 상태로 전환
  ProductSearchState toLoading() {
    return copyWith(
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 추가 로딩 상태로 전환
  ProductSearchState toLoadingMore() {
    return copyWith(
      isLoadingMore: true,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  ProductSearchState toError(String message) {
    return copyWith(
      isLoading: false,
      isLoadingMore: false,
      errorMessage: message,
    );
  }

  /// 검색 결과가 있는지 여부
  bool get hasResults => products.isNotEmpty;

  /// 검색 결과가 없는지 (검색 후)
  bool get isEmpty => hasSearched && products.isEmpty;

  /// 검색 버튼 활성화 가능 여부 (2자 이상)
  bool get canSearch =>
      searchType == SearchType.barcode || query.length >= 2;

  /// 다음 페이지가 있는지 여부
  bool get hasNextPage => !isLastPage;

  ProductSearchState copyWith({
    bool? isLoading,
    bool? isLoadingMore,
    String? errorMessage,
    String? query,
    SearchType? searchType,
    List<Product>? products,
    int? totalElements,
    int? currentPage,
    bool? isLastPage,
    bool? hasSearched,
  }) {
    return ProductSearchState(
      isLoading: isLoading ?? this.isLoading,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
      errorMessage: errorMessage,
      query: query ?? this.query,
      searchType: searchType ?? this.searchType,
      products: products ?? this.products,
      totalElements: totalElements ?? this.totalElements,
      currentPage: currentPage ?? this.currentPage,
      isLastPage: isLastPage ?? this.isLastPage,
      hasSearched: hasSearched ?? this.hasSearched,
    );
  }
}
