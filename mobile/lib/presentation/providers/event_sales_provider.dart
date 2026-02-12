import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/mock/event_mock_repository.dart';
import '../../domain/repositories/event_repository.dart';
import '../../domain/usecases/get_events.dart';
import 'event_sales_state.dart';

// --- Dependency Providers ---

/// Event Repository Provider (Mock)
final eventRepositoryProvider = Provider<EventRepository>((ref) {
  return EventMockRepository();
});

/// GetEvents UseCase Provider
final getEventsUseCaseProvider = Provider<GetEventsUseCase>((ref) {
  final repository = ref.watch(eventRepositoryProvider);
  return GetEventsUseCase(repository);
});

// --- EventSalesNotifier ---

/// 행사매출 목록 상태 관리 Notifier
///
/// 거래처/기간 필터, 페이지네이션 기능을 관리합니다.
class EventSalesNotifier extends StateNotifier<EventSalesState> {
  final GetEventsUseCase _getEvents;

  EventSalesNotifier({
    required GetEventsUseCase getEvents,
  })  : _getEvents = getEvents,
        super(EventSalesState.initial());

  /// 초기 데이터 로딩
  Future<void> initialize() async {
    await loadEvents();
  }

  /// 행사 목록 조회
  Future<void> loadEvents({int? page}) async {
    try {
      state = state.toLoading();

      final targetPage = page ?? state.currentPage;

      final result = await _getEvents.call(
        customerId: state.selectedCustomerId,
        startDate: state.startDate,
        endDate: state.endDate,
        page: targetPage,
        size: state.pageSize,
      );

      state = state.copyWith(
        isLoading: false,
        events: result,
        currentPage: targetPage,
        hasSearched: true,
      );
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// 거래처 필터 변경
  Future<void> setCustomer(String? customerId) async {
    state = state.copyWith(
      selectedCustomerId: customerId,
      currentPage: 1,
    );
    await loadEvents(page: 1);
  }

  /// 거래처 필터 초기화
  Future<void> clearCustomerFilter() async {
    state = state.copyWith(
      clearCustomerFilter: true,
      currentPage: 1,
    );
    await loadEvents(page: 1);
  }

  /// 기간 필터 설정
  Future<void> setDateRange({
    DateTime? startDate,
    DateTime? endDate,
  }) async {
    state = state.copyWith(
      startDate: startDate,
      endDate: endDate,
      currentPage: 1,
    );
    await loadEvents(page: 1);
  }

  /// 기간 필터 초기화
  Future<void> clearDateFilter() async {
    state = state.copyWith(
      clearDateFilter: true,
      currentPage: 1,
    );
    await loadEvents(page: 1);
  }

  /// 검색 (필터 적용 후 재조회)
  Future<void> search() async {
    await loadEvents(page: 1);
  }

  /// 다음 페이지 로딩
  Future<void> loadNextPage() async {
    if (state.isLoading) return;
    await loadEvents(page: state.currentPage + 1);
  }

  /// 이전 페이지 로딩
  Future<void> loadPreviousPage() async {
    if (state.currentPage <= 1 || state.isLoading) return;
    await loadEvents(page: state.currentPage - 1);
  }

  /// 새로고침
  Future<void> refresh() async {
    await loadEvents(page: state.currentPage);
  }
}

/// Event Sales Provider
final eventSalesProvider =
    StateNotifierProvider<EventSalesNotifier, EventSalesState>((ref) {
  final getEvents = ref.watch(getEventsUseCaseProvider);
  return EventSalesNotifier(getEvents: getEvents);
});
