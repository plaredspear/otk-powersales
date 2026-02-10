import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/order_draft.dart';
import '../../domain/entities/product_for_order.dart';
import '../../domain/usecases/add_to_favorites_usecase.dart';
import '../../domain/usecases/get_favorite_products_usecase.dart';
import '../../domain/usecases/remove_from_favorites_usecase.dart';
import '../../domain/usecases/search_products_for_order_usecase.dart';
import 'add_product_state.dart';
import 'order_list_provider.dart';

// --- Dependency Providers ---

/// GetFavoriteProducts UseCase Provider
final getFavoriteProductsUseCaseProvider =
    Provider<GetFavoriteProducts>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return GetFavoriteProducts(repository);
});

/// SearchProductsForOrder UseCase Provider
final searchProductsForOrderUseCaseProvider =
    Provider<SearchProductsForOrder>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return SearchProductsForOrder(repository);
});

/// AddToFavorites UseCase Provider
final addToFavoritesUseCaseProvider = Provider<AddToFavorites>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return AddToFavorites(repository);
});

/// RemoveFromFavorites UseCase Provider
final removeFromFavoritesUseCaseProvider =
    Provider<RemoveFromFavorites>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return RemoveFromFavorites(repository);
});

// --- AddProductNotifier ---

/// 제품 추가 화면 상태 관리 Notifier
///
/// 3개 탭(즐겨찾기/제품검색/주문이력)의 데이터 조회와
/// 제품 선택/즐겨찾기 관리를 처리합니다.
class AddProductNotifier extends StateNotifier<AddProductState> {
  final GetFavoriteProducts _getFavoriteProducts;
  final SearchProductsForOrder _searchProductsForOrder;
  final AddToFavorites _addToFavorites;
  final RemoveFromFavorites _removeFromFavorites;

  AddProductNotifier({
    required GetFavoriteProducts getFavoriteProducts,
    required SearchProductsForOrder searchProductsForOrder,
    required AddToFavorites addToFavorites,
    required RemoveFromFavorites removeFromFavorites,
  })  : _getFavoriteProducts = getFavoriteProducts,
        _searchProductsForOrder = searchProductsForOrder,
        _addToFavorites = addToFavorites,
        _removeFromFavorites = removeFromFavorites,
        super(AddProductState.initial());

  /// 초기화 — 즐겨찾기 탭 데이터 로드
  Future<void> initialize() async {
    await loadFavoriteProducts();
  }

  /// 탭 변경
  void changeTab(AddProductTab tab) {
    state = state.copyWith(currentTab: tab);
  }

  /// 즐겨찾기 제품 목록 조회
  Future<void> loadFavoriteProducts() async {
    state = state.toLoading();

    try {
      final products = await _getFavoriteProducts.call();
      state = state.copyWith(
        isLoading: false,
        favoriteProducts: products,
        clearError: true,
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 제품 검색
  Future<void> searchProducts({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) async {
    state = state.copyWith(searchQuery: query);

    if (query.trim().isEmpty) {
      state = state.copyWith(searchResults: []);
      return;
    }

    state = state.toLoading();

    try {
      final results = await _searchProductsForOrder.call(
        query: query,
        categoryMid: categoryMid,
        categorySub: categorySub,
      );
      state = state.copyWith(
        isLoading: false,
        searchResults: results,
        clearError: true,
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 주문 이력 그룹 설정 (외부에서 주입)
  void setOrderHistoryGroups(List<OrderHistoryGroup> groups) {
    state = state.copyWith(orderHistoryGroups: groups);
  }

  /// 주문 이력 날짜 범위 설정
  void setHistoryDateRange(DateTime from, DateTime to) {
    state = state.copyWith(
      historyDateFrom: from,
      historyDateTo: to,
    );
  }

  /// 주문 이력 그룹 확장/축소 토글
  void toggleOrderHistoryExpansion(int orderId) {
    final updatedGroups = state.orderHistoryGroups.map((group) {
      if (group.orderId == orderId) {
        return group.copyWith(isExpanded: !group.isExpanded);
      }
      return group;
    }).toList();

    state = state.copyWith(orderHistoryGroups: updatedGroups);
  }

  /// 제품 선택 토글
  void toggleProductSelection(String productCode) {
    final updatedSelection = Set<String>.from(state.selectedProductCodes);
    if (updatedSelection.contains(productCode)) {
      updatedSelection.remove(productCode);
    } else {
      updatedSelection.add(productCode);
    }

    state = state.copyWith(selectedProductCodes: updatedSelection);
  }

  /// 선택 초기화
  void clearSelection() {
    state = state.copyWith(selectedProductCodes: const {});
  }

  /// 즐겨찾기 추가
  Future<void> addToFavorites(String productCode) async {
    try {
      await _addToFavorites.call(productCode: productCode);

      // 검색 결과에서 isFavorite 업데이트
      final updatedSearchResults = state.searchResults.map((product) {
        if (product.productCode == productCode) {
          return product.copyWith(isFavorite: true);
        }
        return product;
      }).toList();

      state = state.copyWith(
        searchResults: updatedSearchResults,
        successMessage: '즐겨찾기에 추가되었습니다.',
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 즐겨찾기 삭제
  Future<void> removeFromFavorites(String productCode) async {
    try {
      await _removeFromFavorites.call(productCode: productCode);

      // 즐겨찾기 목록에서 제거
      final updatedFavorites = state.favoriteProducts
          .where((p) => p.productCode != productCode)
          .toList();

      // 검색 결과에서 isFavorite 업데이트
      final updatedSearchResults = state.searchResults.map((product) {
        if (product.productCode == productCode) {
          return product.copyWith(isFavorite: false);
        }
        return product;
      }).toList();

      state = state.copyWith(
        favoriteProducts: updatedFavorites,
        searchResults: updatedSearchResults,
        successMessage: '즐겨찾기에서 삭제되었습니다.',
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 선택된 제품들을 OrderDraftItem 목록으로 변환
  ///
  /// 모든 탭의 제품을 통합하여 선택된 제품만 반환
  List<OrderDraftItem> getSelectedOrderDraftItems() {
    final allProducts = <String, ProductForOrder>{};

    // 모든 탭의 제품을 productCode 기준으로 수집
    for (final product in state.favoriteProducts) {
      allProducts[product.productCode] = product;
    }
    for (final product in state.searchResults) {
      allProducts[product.productCode] = product;
    }
    for (final group in state.orderHistoryGroups) {
      for (final product in group.products) {
        allProducts[product.productCode] = product;
      }
    }

    // 선택된 제품만 OrderDraftItem으로 변환
    return state.selectedProductCodes
        .where((code) => allProducts.containsKey(code))
        .map((code) {
      final product = allProducts[code]!;
      return OrderDraftItem(
        productCode: product.productCode,
        productName: product.productName,
        quantityBoxes: 0,
        quantityPieces: 0,
        unitPrice: product.unitPrice,
        boxSize: product.boxSize,
        totalPrice: 0,
      );
    }).toList();
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(clearError: true);
  }

  /// 성공 메시지 초기화
  void clearSuccess() {
    state = state.copyWith(clearSuccess: true);
  }
}

/// AddProduct StateNotifier Provider
final addProductProvider =
    StateNotifierProvider.autoDispose<AddProductNotifier, AddProductState>(
        (ref) {
  return AddProductNotifier(
    getFavoriteProducts: ref.watch(getFavoriteProductsUseCaseProvider),
    searchProductsForOrder: ref.watch(searchProductsForOrderUseCaseProvider),
    addToFavorites: ref.watch(addToFavoritesUseCaseProvider),
    removeFromFavorites: ref.watch(removeFromFavoritesUseCaseProvider),
  );
});
