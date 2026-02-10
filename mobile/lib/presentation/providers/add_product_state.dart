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

  /// 검색 결과 제품 목록
  final List<ProductForOrder> searchResults;

  /// 주문 이력 그룹 목록
  final List<OrderHistoryGroup> orderHistoryGroups;

  /// 선택된 제품 코드 Set (탭과 무관하게 통합 관리)
  final Set<String> selectedProductCodes;

  /// 검색어
  final String searchQuery;

  /// 주문 이력 검색 시작일
  final DateTime? historyDateFrom;

  /// 주문 이력 검색 종료일
  final DateTime? historyDateTo;

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
    this.orderHistoryGroups = const [],
    this.selectedProductCodes = const {},
    this.searchQuery = '',
    this.historyDateFrom,
    this.historyDateTo,
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

  /// 특정 제품이 선택되었는지 확인
  bool isProductSelected(String productCode) {
    return selectedProductCodes.contains(productCode);
  }

  AddProductState copyWith({
    AddProductTab? currentTab,
    List<ProductForOrder>? favoriteProducts,
    List<ProductForOrder>? searchResults,
    List<OrderHistoryGroup>? orderHistoryGroups,
    Set<String>? selectedProductCodes,
    String? searchQuery,
    DateTime? historyDateFrom,
    DateTime? historyDateTo,
    bool? isLoading,
    String? errorMessage,
    String? successMessage,
    bool clearError = false,
    bool clearSuccess = false,
  }) {
    return AddProductState(
      currentTab: currentTab ?? this.currentTab,
      favoriteProducts: favoriteProducts ?? this.favoriteProducts,
      searchResults: searchResults ?? this.searchResults,
      orderHistoryGroups: orderHistoryGroups ?? this.orderHistoryGroups,
      selectedProductCodes: selectedProductCodes ?? this.selectedProductCodes,
      searchQuery: searchQuery ?? this.searchQuery,
      historyDateFrom: historyDateFrom ?? this.historyDateFrom,
      historyDateTo: historyDateTo ?? this.historyDateTo,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      successMessage:
          clearSuccess ? null : (successMessage ?? this.successMessage),
    );
  }
}
