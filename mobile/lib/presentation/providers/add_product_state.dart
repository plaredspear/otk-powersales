import '../../domain/entities/product_for_order.dart';

/// 제품 추가 탭
enum AddProductTab {
  favorites('즐겨찾기'),
  search('제품 검색'),
  orderHistory('주문 이력');

  final String label;
  const AddProductTab(this.label);
}

/// 주문 이력 항목 (주문건별 그룹)
class OrderHistoryGroup {
  /// 주문 ID
  final int orderId;

  /// 주문 일자
  final String orderDate;

  /// 거래처명
  final String clientName;

  /// 해당 주문의 제품 목록
  final List<ProductForOrder> products;

  /// 확장 여부 (ExpansionTile)
  final bool isExpanded;

  const OrderHistoryGroup({
    required this.orderId,
    required this.orderDate,
    required this.clientName,
    required this.products,
    this.isExpanded = false,
  });

  OrderHistoryGroup copyWith({
    int? orderId,
    String? orderDate,
    String? clientName,
    List<ProductForOrder>? products,
    bool? isExpanded,
  }) {
    return OrderHistoryGroup(
      orderId: orderId ?? this.orderId,
      orderDate: orderDate ?? this.orderDate,
      clientName: clientName ?? this.clientName,
      products: products ?? this.products,
      isExpanded: isExpanded ?? this.isExpanded,
    );
  }
}

/// 제품 추가 화면 상태
class AddProductState {
  /// 현재 선택된 탭
  final AddProductTab currentTab;

  /// 즐겨찾기 제품 목록
  final List<ProductForOrder> favoriteProducts;

  /// 검색 결과 제품 목록 (1회 조회 상한까지만 담긴다)
  final List<ProductForOrder> searchResults;

  /// 검색 조건에 매칭되는 전체 건수 (서버 집계값 — [searchResults] 길이와 다를 수 있음)
  final int searchTotalCount;

  /// 검색 1회 조회 상한. 0 이면 미검색 상태.
  final int searchPageLimit;

  /// 주문 이력 그룹 목록
  final List<OrderHistoryGroup> orderHistoryGroups;

  /// 선택된 제품 코드 Set (탭과 무관하게 통합 관리)
  final Set<String> selectedProductCodes;

  /// 다건 선택 여부. false 면 단건 선택(선택 시 기존 선택 대체).
  final bool multiSelect;

  /// 검색어
  final String searchQuery;

  /// 주문 이력 검색 시작일
  final DateTime? historyDateFrom;

  /// 주문 이력 검색 종료일
  final DateTime? historyDateTo;

  /// 주문 이력 조회용 거래처가 선택되었는지 여부.
  /// false 면 주문 이력 탭은 "거래처를 먼저 선택" 안내를 노출한다(빈 이력과 구분).
  final bool hasOrderHistoryAccount;

  /// 이미 조회 완료된 주문 이력의 조회 조건 키(거래처 + 기간).
  /// 같은 키로 탭을 다시 열면 재조회하지 않는다(null 이면 미조회).
  final String? orderHistoryLoadedKey;

  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 성공 메시지
  final String? successMessage;

  const AddProductState({
    this.currentTab = AddProductTab.favorites,
    this.favoriteProducts = const [],
    this.searchResults = const [],
    this.searchTotalCount = 0,
    this.searchPageLimit = 0,
    this.orderHistoryGroups = const [],
    this.selectedProductCodes = const {},
    this.multiSelect = true,
    this.searchQuery = '',
    this.historyDateFrom,
    this.historyDateTo,
    this.hasOrderHistoryAccount = false,
    this.orderHistoryLoadedKey,
    this.isLoading = false,
    this.errorMessage,
    this.successMessage,
  });

  /// 초기 상태
  factory AddProductState.initial() {
    final now = DateTime.now();
    return AddProductState(
      historyDateFrom: now.subtract(const Duration(days: 3)),
      historyDateTo: now,
    );
  }

  /// 로딩 상태
  AddProductState toLoading() {
    return copyWith(
      isLoading: true,
      clearError: true,
    );
  }

  /// 에러 상태
  AddProductState toError(String message) {
    return copyWith(
      isLoading: false,
      errorMessage: message,
    );
  }

  // --- Computed Getters ---

  /// 선택된 제품 수
  int get selectedCount => selectedProductCodes.length;

  /// 제품이 선택되었는지 여부
  bool get hasSelection => selectedProductCodes.isNotEmpty;

  /// 현재 탭의 제품 목록
  List<ProductForOrder> get currentTabProducts {
    switch (currentTab) {
      case AddProductTab.favorites:
        return favoriteProducts;
      case AddProductTab.search:
        return searchResults;
      case AddProductTab.orderHistory:
        return orderHistoryGroups
            .expand((group) => group.products)
            .toList();
    }
  }

  /// 검색 결과가 1회 조회 상한을 초과해 일부만 노출되고 있는지 여부.
  /// 상한을 넘으면 화면에서 "검색어를 좁혀 달라" 안내를 노출한다.
  bool get isSearchTruncated =>
      searchPageLimit > 0 && searchTotalCount > searchPageLimit;

  /// 특정 제품이 선택되었는지 확인
  bool isProductSelected(String productCode) {
    return selectedProductCodes.contains(productCode);
  }

  AddProductState copyWith({
    AddProductTab? currentTab,
    List<ProductForOrder>? favoriteProducts,
    List<ProductForOrder>? searchResults,
    int? searchTotalCount,
    int? searchPageLimit,
    List<OrderHistoryGroup>? orderHistoryGroups,
    Set<String>? selectedProductCodes,
    bool? multiSelect,
    String? searchQuery,
    DateTime? historyDateFrom,
    DateTime? historyDateTo,
    bool? hasOrderHistoryAccount,
    String? orderHistoryLoadedKey,
    bool? isLoading,
    String? errorMessage,
    String? successMessage,
    bool clearError = false,
    bool clearSuccess = false,
    bool clearOrderHistoryLoadedKey = false,
  }) {
    return AddProductState(
      currentTab: currentTab ?? this.currentTab,
      favoriteProducts: favoriteProducts ?? this.favoriteProducts,
      searchResults: searchResults ?? this.searchResults,
      searchTotalCount: searchTotalCount ?? this.searchTotalCount,
      searchPageLimit: searchPageLimit ?? this.searchPageLimit,
      orderHistoryGroups: orderHistoryGroups ?? this.orderHistoryGroups,
      selectedProductCodes: selectedProductCodes ?? this.selectedProductCodes,
      multiSelect: multiSelect ?? this.multiSelect,
      searchQuery: searchQuery ?? this.searchQuery,
      historyDateFrom: historyDateFrom ?? this.historyDateFrom,
      historyDateTo: historyDateTo ?? this.historyDateTo,
      hasOrderHistoryAccount:
          hasOrderHistoryAccount ?? this.hasOrderHistoryAccount,
      // 캐시 무효화(clear)는 명시 플래그로만 — `?? this` 는 null 삭제를 무시한다.
      orderHistoryLoadedKey: clearOrderHistoryLoadedKey
          ? null
          : (orderHistoryLoadedKey ?? this.orderHistoryLoadedKey),
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      successMessage:
          clearSuccess ? null : (successMessage ?? this.successMessage),
    );
  }
}
