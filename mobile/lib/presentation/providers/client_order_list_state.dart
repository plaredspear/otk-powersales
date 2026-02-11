import '../../domain/entities/client_order.dart';

/// 거래처별 주문 목록 화면 상태
///
/// 필터 조건, 페이지네이션, 로딩/에러 상태를 포함합니다.
class ClientOrderListState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 주문 목록
  final List<ClientOrder> orders;

  /// 전체 결과 수
  final int totalElements;

  /// 전체 페이지 수
  final int totalPages;

  /// 현재 페이지 번호
  final int currentPage;

  /// 첫 번째 페이지 여부
  final bool isFirst;

  /// 마지막 페이지 여부
  final bool isLast;

  /// 검색 실행 여부 (빈 결과 vs 초기 상태 구분)
  final bool hasSearched;

  // --- 필터 조건 ---

  /// 선택된 거래처 ID (null이면 선택 안 함)
  final int? selectedStoreId;

  /// 선택된 거래처명 (드롭다운 표시용)
  final String? selectedStoreName;

  /// 선택된 납기일 (YYYY-MM-DD)
  final String selectedDeliveryDate;

  // --- 거래처 목록 (필터 드롭다운용) ---

  /// 거래처 목록 (id -> name)
  final Map<int, String> stores;

  const ClientOrderListState({
    this.isLoading = false,
    this.errorMessage,
    this.orders = const [],
    this.totalElements = 0,
    this.totalPages = 0,
    this.currentPage = 0,
    this.isFirst = true,
    this.isLast = true,
    this.hasSearched = false,
    this.selectedStoreId,
    this.selectedStoreName,
    required this.selectedDeliveryDate,
    this.stores = const {},
  });

  /// 초기 상태
  factory ClientOrderListState.initial() {
    // 기본 납기일: 오늘
    final now = DateTime.now();
    final todayStr =
        '${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';

    return ClientOrderListState(
      selectedDeliveryDate: todayStr,
    );
  }

  /// 로딩 상태로 전환
  ClientOrderListState toLoading() {
    return copyWith(
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  ClientOrderListState toError(String message) {
    return copyWith(
      isLoading: false,
      errorMessage: message,
    );
  }

  /// 검색 결과가 있는지 여부
  bool get hasResults => orders.isNotEmpty;

  /// 검색 결과가 없는지 (검색 후)
  bool get isEmpty => hasSearched && orders.isEmpty;

  /// 다음 페이지가 있는지 여부
  bool get hasNextPage => !isLast;

  /// 이전 페이지가 있는지 여부
  bool get hasPreviousPage => !isFirst;

  /// 검색 가능 여부 (거래처 선택 필수)
  bool get canSearch => selectedStoreId != null;

  ClientOrderListState copyWith({
    bool? isLoading,
    String? errorMessage,
    List<ClientOrder>? orders,
    int? totalElements,
    int? totalPages,
    int? currentPage,
    bool? isFirst,
    bool? isLast,
    bool? hasSearched,
    int? selectedStoreId,
    String? selectedStoreName,
    String? selectedDeliveryDate,
    Map<int, String>? stores,
    bool clearStoreFilter = false,
  }) {
    return ClientOrderListState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      orders: orders ?? this.orders,
      totalElements: totalElements ?? this.totalElements,
      totalPages: totalPages ?? this.totalPages,
      currentPage: currentPage ?? this.currentPage,
      isFirst: isFirst ?? this.isFirst,
      isLast: isLast ?? this.isLast,
      hasSearched: hasSearched ?? this.hasSearched,
      selectedStoreId:
          clearStoreFilter ? null : (selectedStoreId ?? this.selectedStoreId),
      selectedStoreName: clearStoreFilter
          ? null
          : (selectedStoreName ?? this.selectedStoreName),
      selectedDeliveryDate: selectedDeliveryDate ?? this.selectedDeliveryDate,
      stores: stores ?? this.stores,
    );
  }
}
