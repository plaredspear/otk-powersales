import '../../domain/entities/event.dart';

/// 행사매출 목록 화면 상태
///
/// 필터 조건(거래처, 기간), 페이지네이션, 로딩/에러 상태를 포함합니다.
class EventSalesState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 행사 목록
  final List<Event> events;

  /// 현재 페이지 번호 (1부터 시작)
  final int currentPage;

  /// 페이지 크기
  final int pageSize;

  /// 검색 실행 여부 (빈 결과 vs 초기 상태 구분)
  final bool hasSearched;

  // --- 필터 조건 ---

  /// 선택된 거래처 ID (null이면 전체)
  final String? selectedCustomerId;

  /// 검색 시작일
  final DateTime? startDate;

  /// 검색 종료일
  final DateTime? endDate;

  const EventSalesState({
    this.isLoading = false,
    this.errorMessage,
    this.events = const [],
    this.currentPage = 1,
    this.pageSize = 10,
    this.hasSearched = false,
    this.selectedCustomerId,
    this.startDate,
    this.endDate,
  });

  /// 초기 상태
  factory EventSalesState.initial() {
    return EventSalesState(
      startDate: DateTime.now(),
      endDate: DateTime.now(),
    );
  }

  /// 로딩 상태로 전환
  EventSalesState toLoading() {
    return copyWith(
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  EventSalesState toError(String message) {
    return copyWith(
      isLoading: false,
      errorMessage: message,
    );
  }

  /// 검색 결과가 있는지 여부
  bool get hasResults => events.isNotEmpty;

  /// 검색 결과가 없는지 (검색 후)
  bool get isEmpty => hasSearched && events.isEmpty;

  /// 필터가 적용되어 있는지 여부
  bool get hasActiveFilter =>
      selectedCustomerId != null ||
      startDate != null ||
      endDate != null;

  EventSalesState copyWith({
    bool? isLoading,
    String? errorMessage,
    List<Event>? events,
    int? currentPage,
    int? pageSize,
    bool? hasSearched,
    String? selectedCustomerId,
    DateTime? startDate,
    DateTime? endDate,
    bool clearCustomerFilter = false,
    bool clearDateFilter = false,
  }) {
    return EventSalesState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      events: events ?? this.events,
      currentPage: currentPage ?? this.currentPage,
      pageSize: pageSize ?? this.pageSize,
      hasSearched: hasSearched ?? this.hasSearched,
      selectedCustomerId: clearCustomerFilter
          ? null
          : (selectedCustomerId ?? this.selectedCustomerId),
      startDate:
          clearDateFilter ? null : (startDate ?? this.startDate),
      endDate:
          clearDateFilter ? null : (endDate ?? this.endDate),
    );
  }
}
