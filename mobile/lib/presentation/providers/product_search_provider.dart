import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/mock/mock_product_repository.dart';
import '../../domain/repositories/product_repository.dart';
import 'product_search_state.dart';

// --- Dependency Providers ---

/// Product Repository Provider
final productRepositoryProvider = Provider<ProductRepository>((ref) {
  return MockProductRepository();
});

// --- ProductSearchNotifier ---

/// 제품검색 상태 관리 Notifier
///
/// 텍스트 검색, 바코드 검색, 페이지네이션(무한 스크롤)을 관리합니다.
class ProductSearchNotifier extends StateNotifier<ProductSearchState> {
  final ProductRepository _repository;

  ProductSearchNotifier({
    required ProductRepository repository,
  })  : _repository = repository,
        super(ProductSearchState.initial());

  /// 검색어 변경
  void updateQuery(String query) {
    state = state.copyWith(query: query);
  }

  /// 검색 유형 변경
  void updateSearchType(SearchType type) {
    state = state.copyWith(searchType: type);
  }

  /// 제품 검색 실행
  ///
  /// 새 검색어로 첫 페이지부터 검색합니다.
  Future<void> search() async {
    if (!state.canSearch) return;

    state = state.toLoading();

    try {
      final result = await _repository.searchProducts(
        query: state.query,
        type: state.searchType == SearchType.barcode ? 'barcode' : 'text',
        page: 0,
      );

      state = state.copyWith(
        isLoading: false,
        products: result.products,
        totalElements: result.totalElements,
        currentPage: 0,
        isLastPage: result.isLast,
        hasSearched: true,
        errorMessage: null,
      );
    } on ArgumentError catch (e) {
      state = state.toError(e.message as String);
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 바코드로 검색
  ///
  /// 바코드 스캔 결과를 사용하여 즉시 검색합니다.
  Future<void> searchByBarcode(String barcode) async {
    state = state.copyWith(
      query: barcode,
      searchType: SearchType.barcode,
    );
    await search();
  }

  /// 다음 페이지 로드 (무한 스크롤)
  Future<void> loadNextPage() async {
    if (state.isLoading || state.isLoadingMore || state.isLastPage) return;

    state = state.toLoadingMore();

    try {
      final nextPage = state.currentPage + 1;
      final result = await _repository.searchProducts(
        query: state.query,
        type: state.searchType == SearchType.barcode ? 'barcode' : 'text',
        page: nextPage,
      );

      state = state.copyWith(
        isLoadingMore: false,
        products: [...state.products, ...result.products],
        totalElements: result.totalElements,
        currentPage: nextPage,
        isLastPage: result.isLast,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 검색 초기화
  void clearSearch() {
    state = ProductSearchState.initial();
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

/// ProductSearch StateNotifier Provider
final productSearchProvider =
    StateNotifierProvider<ProductSearchNotifier, ProductSearchState>((ref) {
  return ProductSearchNotifier(
    repository: ref.watch(productRepositoryProvider),
  );
});
