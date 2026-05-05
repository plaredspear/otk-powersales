import '../../domain/entities/order_request.dart';

/// 본인 주문요청 목록 화면 상태 (클라이언트 슬라이스 패턴).
///
/// 백엔드에서 받은 전체 배열은 [allOrderRequests] 에 보관하고,
/// 사용자가 선택한 [currentPage] 에 해당하는 슬라이스만 [pagedItems] getter 로 반환한다.
class OrderRequestListState {
  /// 페이지당 행 수
  static const int defaultPageSize = 20;

  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 백엔드에서 받은 전체 주문요청 배열 (페이징 없음)
  final List<OrderRequest> allOrderRequests;

  /// 현재 페이지 (0-indexed)
  final int currentPage;

  /// 페이지당 행 수
  final int pageSize;

  /// 결과가 응답 라인 수 상한(2000건)에 도달해 잘렸는지 여부
  final bool truncated;

  /// 서버 조회 시각
  final DateTime? fetchedAt;

  /// 검색 실행 여부 (빈 결과 vs 초기 상태 구분)
  final bool hasSearched;

  // --- 필터 조건 ---

  /// 선택된 거래처 ID (null이면 전체)
  final int? selectedClientId;

  /// 선택된 거래처명 (드롭다운 표시용)
  final String? selectedClientName;

  /// 선택된 주문요청 상태 코드 (null이면 전체)
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

  const OrderRequestListState({
    this.isLoading = false,
    this.errorMessage,
    this.allOrderRequests = const [],
    this.currentPage = 0,
    this.pageSize = defaultPageSize,
    this.truncated = false,
    this.fetchedAt,
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
  factory OrderRequestListState.initial() {
    // 기본 납기일: 오늘 ~ 오늘+7일 (old_source 기준)
    final now = DateTime.now();
    final sevenDaysLater = now.add(const Duration(days: 7));
    final fromStr =
        '${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';
    final toStr =
        '${sevenDaysLater.year}-${sevenDaysLater.month.toString().padLeft(2, '0')}-${sevenDaysLater.day.toString().padLeft(2, '0')}';

    return OrderRequestListState(
      deliveryDateFrom: fromStr,
      deliveryDateTo: toStr,
    );
  }

  /// 로딩 상태로 전환
  OrderRequestListState toLoading() => copyWith(isLoading: true, errorMessage: null);

  /// 에러 상태로 전환
  OrderRequestListState toError(String message) =>
      copyWith(isLoading: false, errorMessage: message);

  /// 현재 페이지에 해당하는 슬라이스
  List<OrderRequest> get pagedItems =>
      allOrderRequests.skip(currentPage * pageSize).take(pageSize).toList();

  /// 전체 페이지 수
  int get totalPages =>
      allOrderRequests.isEmpty ? 0 : (allOrderRequests.length / pageSize).ceil();

  /// 검색 결과가 있는지 여부
  bool get hasResults => allOrderRequests.isNotEmpty;

  /// 검색 결과가 없는지 (검색 후)
  bool get isEmpty => hasSearched && allOrderRequests.isEmpty;

  /// 필터가 적용되어 있는지 여부
  bool get hasActiveFilter =>
      selectedClientId != null || selectedStatus != null;

  OrderRequestListState copyWith({
    bool? isLoading,
    String? errorMessage,
    List<OrderRequest>? allOrderRequests,
    int? currentPage,
    int? pageSize,
    bool? truncated,
    DateTime? fetchedAt,
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
    return OrderRequestListState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      allOrderRequests: allOrderRequests ?? this.allOrderRequests,
      currentPage: currentPage ?? this.currentPage,
      pageSize: pageSize ?? this.pageSize,
      truncated: truncated ?? this.truncated,
      fetchedAt: fetchedAt ?? this.fetchedAt,
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
