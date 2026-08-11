import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/product_for_order.dart';
import '../../domain/usecases/add_to_favorites_usecase.dart';
import '../../domain/usecases/get_account_order_history_usecase.dart';
import '../../domain/usecases/get_favorite_products_usecase.dart';
import '../../domain/usecases/remove_from_favorites_usecase.dart';
import '../../domain/usecases/search_products_for_order_usecase.dart';
import 'add_product_state.dart';
import 'order_request_list_provider.dart';

// --- Dependency Providers ---

/// GetFavoriteProducts UseCase Provider
final getFavoriteProductsUseCaseProvider =
    Provider<GetFavoriteProducts>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
  return GetFavoriteProducts(repository);
});

/// SearchProductsForOrder UseCase Provider
final searchProductsForOrderUseCaseProvider =
    Provider<SearchProductsForOrder>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
  return SearchProductsForOrder(repository);
});

/// GetAccountOrderHistory UseCase Provider
final getAccountOrderHistoryUseCaseProvider =
    Provider<GetAccountOrderHistory>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
  return GetAccountOrderHistory(repository);
});

/// AddToFavorites UseCase Provider
final addToFavoritesUseCaseProvider = Provider<AddToFavorites>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
  return AddToFavorites(repository);
});

/// RemoveFromFavorites UseCase Provider
final removeFromFavoritesUseCaseProvider =
    Provider<RemoveFromFavorites>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
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
  final GetAccountOrderHistory _getAccountOrderHistory;
  final AddToFavorites _addToFavorites;
  final RemoveFromFavorites _removeFromFavorites;

  /// 주문이력 조회용 거래처 내부 ID(Account.id). 주문서 작성처럼
  /// 거래처가 선택된 화면에서만 주입되며, 없으면 주문이력 탭은 항상 비어 있다.
  int? _orderHistoryAccountId;

  /// 마지막 검색에 사용한 분류 조건 — 무한스크롤 추가 로드 시 동일 조건으로 재요청한다.
  /// (검색어는 state.searchQuery 에 남지만 분류는 화면 상태라 여기서 보관한다.)
  String? _searchCategoryMid;
  String? _searchCategorySub;

  AddProductNotifier({
    required GetFavoriteProducts getFavoriteProducts,
    required SearchProductsForOrder searchProductsForOrder,
    required GetAccountOrderHistory getAccountOrderHistory,
    required AddToFavorites addToFavorites,
    required RemoveFromFavorites removeFromFavorites,
  })  : _getFavoriteProducts = getFavoriteProducts,
        _searchProductsForOrder = searchProductsForOrder,
        _getAccountOrderHistory = getAccountOrderHistory,
        _addToFavorites = addToFavorites,
        _removeFromFavorites = removeFromFavorites,
        super(AddProductState.initial());

  /// 초기화 — 선택 모드 설정 + 즐겨찾기 탭 데이터 로드.
  ///
  /// [orderHistoryAccountId] 를 주면 주문이력 탭에서 해당 거래처 주문이력을 조회한다.
  Future<void> initialize({
    bool multiSelect = true,
    int? orderHistoryAccountId,
  }) async {
    _orderHistoryAccountId = orderHistoryAccountId;
    state = state.copyWith(
      multiSelect: multiSelect,
      hasOrderHistoryAccount: orderHistoryAccountId != null,
      // 거래처가 바뀌어 다시 초기화될 수 있으므로 주문이력 캐시는 버린다.
      orderHistoryGroups: const [],
      clearOrderHistoryLoadedKey: true,
    );
    await loadFavoriteProducts();
  }

  /// 탭 변경 — 주문이력 탭 진입 시 거래처 주문이력을 조회한다.
  ///
  /// 이미 같은 조건(거래처 + 기간)으로 받아둔 이력이 있으면 재조회하지 않는다
  /// (탭 왕복마다 API 호출 + 펼침 상태 초기화가 발생하던 문제).
  void changeTab(AddProductTab tab) {
    state = state.copyWith(currentTab: tab);
    if (tab == AddProductTab.orderHistory) {
      loadOrderHistory();
    }
  }

  /// 거래처 주문이력 조회(현재 기간 기준). 거래처가 선택되지 않았으면 빈 목록을 유지한다.
  ///
  /// [force] 가 false 면 직전 조회와 조건(거래처 + 기간)이 같을 때 캐시를 재사용한다.
  Future<void> loadOrderHistory({bool force = false}) async {
    final accountId = _orderHistoryAccountId;
    if (accountId == null) {
      state = state.copyWith(
        orderHistoryGroups: const [],
        isLoading: false,
        clearOrderHistoryLoadedKey: true,
      );
      return;
    }

    final now = DateTime.now();
    final from = state.historyDateFrom ?? now.subtract(const Duration(days: 3));
    final to = state.historyDateTo ?? now;

    // 조회 조건 키 — 기간은 일 단위 비교(시:분 차이로 캐시가 깨지지 않게).
    final loadKey = '$accountId|${_dateKey(from)}|${_dateKey(to)}';
    if (!force && state.orderHistoryLoadedKey == loadKey) return;

    state = state.toLoading();
    try {
      final groups = await _getAccountOrderHistory.call(
        accountId: accountId,
        startDate: from,
        endDate: to,
      );
      // 재조회 시 사용자가 펼쳐둔 주문(주문일 기준)은 그대로 승계한다.
      final previousExpanded = {
        for (final group in state.orderHistoryGroups)
          group.orderDate: group.isExpanded,
      };
      // 도메인 그룹 → 표시용 그룹. 그룹 키(orderId)는 주문일 인덱스로 부여하고,
      // 단일 거래처 조회라 거래처명은 별도로 싣지 않는다(제목은 주문일만 표시).
      final mapped = <OrderHistoryGroup>[
        for (var i = 0; i < groups.length; i++)
          OrderHistoryGroup(
            orderId: i,
            orderDate: groups[i].orderDate,
            clientName: '',
            products: groups[i].products,
            isExpanded: previousExpanded[groups[i].orderDate] ?? (i == 0),
          ),
      ];
      state = state.copyWith(
        isLoading: false,
        orderHistoryGroups: mapped,
        orderHistoryLoadedKey: loadKey,
        clearError: true,
      );
    } catch (e) {
      // 실패는 캐시로 남기지 않는다(다음 진입 시 재시도).
      state = state.toError(extractErrorMessage(e));
    }
  }

  /// 주문이력 캐시 키용 일자 문자열(yyyy-MM-dd)
  String _dateKey(DateTime date) {
    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');
    return '${date.year}-$month-$day';
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
        extractErrorMessage(e),
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

    // 검색어가 비어도 분류(중/소)가 지정되면 분류 검색을 허용한다(전산매출 등).
    final hasCategory =
        (categoryMid != null && categoryMid.isNotEmpty) ||
            (categorySub != null && categorySub.isNotEmpty);
    if (query.trim().isEmpty && !hasCategory) {
      state = state.copyWith(
        searchResults: [],
        searchTotalCount: 0,
        searchPage: 0,
        hasMoreSearchResults: false,
        isLoadingMore: false,
      );
      return;
    }

    // 새 검색은 항상 첫 페이지부터 — 이전 검색의 누적 결과/페이지를 버린다.
    _searchCategoryMid = categoryMid;
    _searchCategorySub = categorySub;
    state = state.toLoading();

    try {
      final result = await _searchProductsForOrder.call(
        query: query,
        categoryMid: categoryMid,
        categorySub: categorySub,
      );
      state = state.copyWith(
        isLoading: false,
        searchResults: result.products,
        searchTotalCount: result.totalCount,
        searchPage: result.page,
        hasMoreSearchResults: result.hasMore,
        isLoadingMore: false,
        clearError: true,
      );
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 검색 결과 다음 페이지 로드 (무한스크롤).
  ///
  /// 이미 로딩 중이거나 더 불러올 결과가 없으면 아무것도 하지 않는다.
  /// 추가 로드 실패는 기존 목록을 유지한 채 에러 메시지만 노출한다(누적분 보존).
  Future<void> loadMoreSearchResults() async {
    if (state.isLoading || state.isLoadingMore || !state.hasMoreSearchResults) {
      return;
    }

    state = state.copyWith(isLoadingMore: true);

    try {
      final result = await _searchProductsForOrder.call(
        query: state.searchQuery,
        categoryMid: _searchCategoryMid,
        categorySub: _searchCategorySub,
        page: state.searchPage + 1,
        loadedCount: state.searchResults.length,
      );
      state = state.copyWith(
        isLoadingMore: false,
        searchResults: [...state.searchResults, ...result.products],
        searchTotalCount: result.totalCount,
        searchPage: result.page,
        hasMoreSearchResults: result.hasMore,
        clearError: true,
      );
    } catch (e) {
      state = state.copyWith(
        isLoadingMore: false,
        errorMessage: extractErrorMessage(e),
      );
    }
  }

  /// 주문 이력 그룹 설정 (외부에서 주입)
  void setOrderHistoryGroups(List<OrderHistoryGroup> groups) {
    state = state.copyWith(orderHistoryGroups: groups);
  }

  /// 주문 이력 날짜 범위 설정 — 변경 즉시 재조회한다.
  void setHistoryDateRange(DateTime from, DateTime to) {
    state = state.copyWith(
      historyDateFrom: from,
      historyDateTo: to,
    );
    // 기간 선택은 명시적 조회 액션이라 항상 재조회한다.
    loadOrderHistory(force: true);
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
  ///
  /// 단건 선택 모드(`multiSelect == false`)에서는 새 제품을 고르면
  /// 기존 선택을 대체하고, 같은 제품을 다시 누르면 선택 해제한다.
  void toggleProductSelection(String productCode) {
    final isSelected = state.selectedProductCodes.contains(productCode);

    if (!state.multiSelect) {
      state = state.copyWith(
        selectedProductCodes: isSelected ? const {} : {productCode},
      );
      return;
    }

    final updatedSelection = Set<String>.from(state.selectedProductCodes);
    if (isSelected) {
      updatedSelection.remove(productCode);
    } else {
      updatedSelection.add(productCode);
    }

    state = state.copyWith(selectedProductCodes: updatedSelection);
  }

  /// 전체 선택/해제 — 지정한 제품 코드들을 한 번에 선택하거나 해제한다.
  ///
  /// 다건 선택 모드에서만 동작한다(단건 모드에는 전체 선택이 없다).
  /// 차단 제품(전용상품 등) 제외는 호출 측에서 코드 목록을 걸러 전달한다.
  void setSelectionForCodes(Iterable<String> productCodes, bool selected) {
    if (!state.multiSelect) return;

    final updatedSelection = Set<String>.from(state.selectedProductCodes);
    if (selected) {
      updatedSelection.addAll(productCodes);
    } else {
      updatedSelection.removeAll(productCodes);
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
        extractErrorMessage(e),
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
        extractErrorMessage(e),
      );
    }
  }

  /// 선택된 제품들을 [ProductForOrder] 목록으로 반환
  ///
  /// 모든 탭의 제품을 productCode 기준으로 통합하여 선택된 것만 반환한다.
  /// 주문/클레임/매출조회 등 호출 화면이 이 결과를 각자 필요한 모델로
  /// 매핑해서 사용한다(모달은 주문 도메인에 결합되지 않는다).
  List<ProductForOrder> getSelectedProducts() {
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

    return state.selectedProductCodes
        .where((code) => allProducts.containsKey(code))
        .map((code) => allProducts[code]!)
        .toList();
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
    getAccountOrderHistory: ref.watch(getAccountOrderHistoryUseCaseProvider),
    addToFavorites: ref.watch(addToFavoritesUseCaseProvider),
    removeFromFavorites: ref.watch(removeFromFavoritesUseCaseProvider),
  );
});
