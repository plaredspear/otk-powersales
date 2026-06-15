/// 제품추가(유통기한) 팝업 탭.
enum ProductAddTab {
  search('제품 검색'),
  orderHistory('주문 이력');

  final String label;
  const ProductAddTab(this.label);
}

/// 중분류→소분류 카테고리.
class ProductCategory {
  /// 중분류 (category2).
  final String middle;

  /// 소분류 (category3) 목록.
  final List<String> subs;

  const ProductCategory({required this.middle, required this.subs});

  factory ProductCategory.fromJson(Map<String, dynamic> json) {
    return ProductCategory(
      middle: json['middle'] as String,
      subs: (json['subs'] as List<dynamic>?)?.cast<String>() ?? const [],
    );
  }
}

/// 제품추가 검색 결과 제품 1건.
///
/// 레거시 제품 목록(productList)이 표시하는 필드를 담는다.
class ProductAddItem {
  final String productCode;
  final String productName;
  final String? barcode;
  final String? storageCondition;
  final String? shelfLife;
  final String? shelfLifeUnit;

  const ProductAddItem({
    required this.productCode,
    required this.productName,
    this.barcode,
    this.storageCondition,
    this.shelfLife,
    this.shelfLifeUnit,
  });

  factory ProductAddItem.fromJson(Map<String, dynamic> json) {
    return ProductAddItem(
      productCode: (json['productCode'] as String?) ?? '',
      productName: (json['productName'] as String?) ?? '',
      barcode: json['barcode'] as String?,
      storageCondition: json['storageCondition'] as String?,
      shelfLife: json['shelfLife'] as String?,
      shelfLifeUnit: json['shelfLifeUnit'] as String?,
    );
  }

  /// "9개월" 형태의 유통기한 표시값 (없으면 null).
  String? get shelfLifeText {
    if (shelfLife == null || shelfLife!.isEmpty) return null;
    return '$shelfLife${shelfLifeUnit ?? ''}';
  }
}

/// 주문이력 그룹 (주문일별).
class OrderHistoryDateGroup {
  final String orderDate;
  final List<ProductAddItem> products;

  const OrderHistoryDateGroup({required this.orderDate, required this.products});

  factory OrderHistoryDateGroup.fromJson(Map<String, dynamic> json) {
    final list = (json['products'] as List<dynamic>?) ?? const [];
    return OrderHistoryDateGroup(
      orderDate: json['orderDate'] as String,
      products: list
          .map((e) => ProductAddItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

/// 제품추가 팝업 상태.
class ProductAddState {
  /// 카테고리 목록 (중분류→소분류).
  final List<ProductCategory> categories;

  /// 선택된 중분류 (null = 전체).
  final String? selectedMiddle;

  /// 선택된 소분류 (null = 전체).
  final String? selectedSub;

  /// 제품 검색 결과.
  final List<ProductAddItem> searchResults;

  /// 검색 실행 여부 (검색 전/후 빈 상태 메시지 구분).
  final bool hasSearched;

  /// 검색 총 건수 (레거시 totalCnt — "제품 (N)" 표시값).
  final int totalCount;

  /// 현재 로드된 페이지 번호 (0부터).
  final int currentPage;

  /// 마지막 페이지 여부 (무한 스크롤 종료 판단).
  final bool isLastPage;

  /// 다음 페이지 로딩 중 여부 (하단 로더 표시).
  final bool isLoadingMore;

  /// 적용된 검색 조건 스냅샷 — loadNextPage 가 동일 조건으로 다음 페이지를 조회한다.
  /// '' 는 미적용(전체)을 의미한다(검색 후 드롭다운만 바뀌어도 페이지 조건이 흔들리지 않게 분리).
  final String appliedName;
  final String appliedBarcode;
  final String appliedMiddle;
  final String appliedSub;

  /// 주문이력 그룹.
  final List<OrderHistoryDateGroup> orderHistoryGroups;

  /// 주문이력 조회 시작일.
  final DateTime historyFrom;

  /// 주문이력 조회 종료일.
  final DateTime historyTo;

  /// 주문이력 조회 실행 여부.
  final bool hasSearchedHistory;

  /// 로딩 상태.
  final bool isLoading;

  /// 에러 메시지.
  final String? errorMessage;

  const ProductAddState({
    this.categories = const [],
    this.selectedMiddle,
    this.selectedSub,
    this.searchResults = const [],
    this.hasSearched = false,
    this.totalCount = 0,
    this.currentPage = 0,
    this.isLastPage = false,
    this.isLoadingMore = false,
    this.appliedName = '',
    this.appliedBarcode = '',
    this.appliedMiddle = '',
    this.appliedSub = '',
    this.orderHistoryGroups = const [],
    required this.historyFrom,
    required this.historyTo,
    this.hasSearchedHistory = false,
    this.isLoading = false,
    this.errorMessage,
  });

  factory ProductAddState.initial() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    return ProductAddState(
      historyFrom: today.subtract(const Duration(days: 3)),
      historyTo: today,
    );
  }

  /// 선택된 중분류의 소분류 목록.
  List<String> get subsForSelectedMiddle {
    if (selectedMiddle == null) return const [];
    final match = categories.where((c) => c.middle == selectedMiddle);
    return match.isEmpty ? const [] : match.first.subs;
  }

  /// 검색 총 건수 (레거시 totalCnt — 로드된 건수가 아니라 전체 매칭 건수).
  int get resultCount => totalCount;

  ProductAddState copyWith({
    List<ProductCategory>? categories,
    String? selectedMiddle,
    bool clearMiddle = false,
    String? selectedSub,
    bool clearSub = false,
    List<ProductAddItem>? searchResults,
    bool? hasSearched,
    int? totalCount,
    int? currentPage,
    bool? isLastPage,
    bool? isLoadingMore,
    String? appliedName,
    String? appliedBarcode,
    String? appliedMiddle,
    String? appliedSub,
    List<OrderHistoryDateGroup>? orderHistoryGroups,
    DateTime? historyFrom,
    DateTime? historyTo,
    bool? hasSearchedHistory,
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
  }) {
    return ProductAddState(
      categories: categories ?? this.categories,
      selectedMiddle: clearMiddle ? null : (selectedMiddle ?? this.selectedMiddle),
      selectedSub: clearSub ? null : (selectedSub ?? this.selectedSub),
      searchResults: searchResults ?? this.searchResults,
      hasSearched: hasSearched ?? this.hasSearched,
      totalCount: totalCount ?? this.totalCount,
      currentPage: currentPage ?? this.currentPage,
      isLastPage: isLastPage ?? this.isLastPage,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
      appliedName: appliedName ?? this.appliedName,
      appliedBarcode: appliedBarcode ?? this.appliedBarcode,
      appliedMiddle: appliedMiddle ?? this.appliedMiddle,
      appliedSub: appliedSub ?? this.appliedSub,
      orderHistoryGroups: orderHistoryGroups ?? this.orderHistoryGroups,
      historyFrom: historyFrom ?? this.historyFrom,
      historyTo: historyTo ?? this.historyTo,
      hasSearchedHistory: hasSearchedHistory ?? this.hasSearchedHistory,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }
}
