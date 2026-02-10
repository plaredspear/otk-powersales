import '../../domain/entities/order.dart';

/// 주문 목록 화면 상태
///
/// 필터 조건, 정렬, 페이지네이션, 로딩/에러 상태를 포함합니다.
class OrderListState {
  /// 로딩 상태
  final bool isLoading;

  /// 추가 페이지 로딩 상태
  final bool isLoadingMore;

  /// 에러 메시지
  final String? errorMessage;

  /// 주문 목록
  final List<Order> orders;

  /// 전체 결과 수
  final int totalElements;

  /// 현재 페이지 번호
  final int currentPage;

  /// 마지막 페이지 여부
  final bool isLastPage;

  /// 검색 실행 여부 (빈 결과 vs 초기 상태 구분)
  final bool hasSearched;

  // --- 필터 조건 ---

  /// 선택된 거래처 ID (null이면 전체)
  final int? selectedClientId;

  /// 선택된 거래처명 (드롭다운 표시용)
  final String? selectedClientName;

  /// 선택된 승인상태 (null이면 전체)
  final String? selectedStatus;

  /// 납기일 시작 (YYYY-MM-DD)
  final String? deliveryDateFrom;

  /// 납기일 종료 (YYYY-MM-DD)
  final String? deliveryDateTo;

  // --- 정렬 조건 ---

  /// 현재 정렬 타입
  final OrderSortType sortType;

  // --- 거래처 목록 (필터 드롭다운용) ---

  /// 거래처 목록 (id -> name)
  final Map<int, String> clients;

  const OrderListState({
    this.isLoading = false,
    this.isLoadingMore = false,
    this.errorMessage,
    this.orders = const [],
    this.totalElements = 0,
    this.currentPage = 0,
    this.isLastPage = false,
    this.hasSearched = false,
    this.selectedClientId,
    this.selectedClientName,
    this.selectedStatus,
    this.deliveryDateFrom,
    this.deliveryDateTo,
    this.sortType = OrderSortType.latestOrder,
    this.clients = const {},
  });

  /// 초기 상태
  factory OrderListState.initial() {
    // 기본 납기일: 최근 7일
    final now = DateTime.now();
    final sevenDaysAgo = now.subtract(const Duration(days: 7));
    final fromStr =
        '${sevenDaysAgo.year}-${sevenDaysAgo.month.toString().padLeft(2, '0')}-${sevenDaysAgo.day.toString().padLeft(2, '0')}';
    final toStr =
        '${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';

    return OrderListState(
      deliveryDateFrom: fromStr,
      deliveryDateTo: toStr,
    );
  }

  /// 로딩 상태로 전환
  OrderListState toLoading() {
    return copyWith(
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 추가 로딩 상태로 전환
  OrderListState toLoadingMore() {
    return copyWith(
      isLoadingMore: true,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  OrderListState toError(String message) {
    return copyWith(
      isLoading: false,
      isLoadingMore: false,
      errorMessage: message,
    );
  }

  /// 검색 결과가 있는지 여부
  bool get hasResults => orders.isNotEmpty;

  /// 검색 결과가 없는지 (검색 후)
  bool get isEmpty => hasSearched && orders.isEmpty;

  /// 다음 페이지가 있는지 여부
  bool get hasNextPage => !isLastPage;

  /// 필터가 적용되어 있는지 여부
  bool get hasActiveFilter =>
      selectedClientId != null || selectedStatus != null;

  OrderListState copyWith({
    bool? isLoading,
    bool? isLoadingMore,
    String? errorMessage,
    List<Order>? orders,
    int? totalElements,
    int? currentPage,
    bool? isLastPage,
    bool? hasSearched,
    int? selectedClientId,
    String? selectedClientName,
    String? selectedStatus,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    OrderSortType? sortType,
    Map<int, String>? clients,
    bool clearClientFilter = false,
    bool clearStatusFilter = false,
    bool clearDateFilter = false,
  }) {
    return OrderListState(
      isLoading: isLoading ?? this.isLoading,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
      errorMessage: errorMessage,
      orders: orders ?? this.orders,
      totalElements: totalElements ?? this.totalElements,
      currentPage: currentPage ?? this.currentPage,
      isLastPage: isLastPage ?? this.isLastPage,
      hasSearched: hasSearched ?? this.hasSearched,
      selectedClientId:
          clearClientFilter ? null : (selectedClientId ?? this.selectedClientId),
      selectedClientName: clearClientFilter
          ? null
          : (selectedClientName ?? this.selectedClientName),
      selectedStatus:
          clearStatusFilter ? null : (selectedStatus ?? this.selectedStatus),
      deliveryDateFrom: clearDateFilter
          ? null
          : (deliveryDateFrom ?? this.deliveryDateFrom),
      deliveryDateTo:
          clearDateFilter ? null : (deliveryDateTo ?? this.deliveryDateTo),
      sortType: sortType ?? this.sortType,
      clients: clients ?? this.clients,
    );
  }
}
