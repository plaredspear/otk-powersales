import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/mock/order_mock_repository.dart';
import '../../domain/entities/order.dart';
import '../../domain/repositories/order_repository.dart';
import '../../domain/usecases/get_my_orders.dart';
import 'order_list_state.dart';

// --- Dependency Providers ---

/// Order Repository Provider
final orderRepositoryProvider = Provider<OrderRepository>((ref) {
  return OrderMockRepository();
});

/// GetMyOrders UseCase Provider
final getMyOrdersUseCaseProvider = Provider<GetMyOrders>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return GetMyOrders(repository);
});

// --- OrderListNotifier ---

/// 주문 목록 상태 관리 Notifier
///
/// 필터링, 정렬, 페이지네이션(무한 스크롤), 검색 기능을 관리합니다.
class OrderListNotifier extends StateNotifier<OrderListState> {
  final GetMyOrders _getMyOrders;
  final OrderMockRepository? _mockRepository;

  OrderListNotifier({
    required GetMyOrders getMyOrders,
    OrderMockRepository? mockRepository,
  })  : _getMyOrders = getMyOrders,
        _mockRepository = mockRepository,
        super(OrderListState.initial());

  /// 초기 데이터 로딩
  ///
  /// 거래처 목록 로딩 + 기본 필터로 주문 목록 조회
  Future<void> initialize() async {
    // Mock Repository에서 거래처 목록 로딩
    if (_mockRepository != null) {
      state = state.copyWith(clients: _mockRepository.mockClients);
    }

    // 기본 필터로 주문 목록 조회
    await searchOrders();
  }

  /// 주문 목록 검색 실행
  ///
  /// 현재 필터 조건으로 첫 페이지부터 검색합니다.
  Future<void> searchOrders() async {
    state = state.toLoading();

    try {
      final result = await _getMyOrders.call(
        clientId: state.selectedClientId,
        status: state.selectedStatus,
        deliveryDateFrom: state.deliveryDateFrom,
        deliveryDateTo: state.deliveryDateTo,
        sortBy: state.sortType.sortBy,
        sortDir: state.sortType.sortDir,
        page: 0,
      );

      state = state.copyWith(
        isLoading: false,
        orders: result.orders,
        totalElements: result.totalElements,
        currentPage: 0,
        isLastPage: result.isLast,
        hasSearched: true,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 다음 페이지 로드 (무한 스크롤)
  Future<void> loadNextPage() async {
    if (state.isLoading || state.isLoadingMore || state.isLastPage) return;

    state = state.toLoadingMore();

    try {
      final nextPage = state.currentPage + 1;
      final result = await _getMyOrders.call(
        clientId: state.selectedClientId,
        status: state.selectedStatus,
        deliveryDateFrom: state.deliveryDateFrom,
        deliveryDateTo: state.deliveryDateTo,
        sortBy: state.sortType.sortBy,
        sortDir: state.sortType.sortDir,
        page: nextPage,
      );

      state = state.copyWith(
        isLoadingMore: false,
        orders: [...state.orders, ...result.orders],
        totalElements: result.totalElements,
        currentPage: nextPage,
        isLastPage: result.isLast,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 거래처 필터 변경
  void updateClientFilter(int? clientId, String? clientName) {
    if (clientId == null) {
      state = state.copyWith(clearClientFilter: true);
    } else {
      state = state.copyWith(
        selectedClientId: clientId,
        selectedClientName: clientName,
      );
    }
  }

  /// 상태 필터 변경
  void updateStatusFilter(String? status) {
    if (status == null) {
      state = state.copyWith(clearStatusFilter: true);
    } else {
      state = state.copyWith(selectedStatus: status);
    }
  }

  /// 납기일 범위 변경
  void updateDeliveryDateRange(String? from, String? to) {
    if (from == null && to == null) {
      state = state.copyWith(clearDateFilter: true);
    } else {
      state = state.copyWith(
        deliveryDateFrom: from,
        deliveryDateTo: to,
      );
    }
  }

  /// 정렬 변경
  ///
  /// 정렬 옵션을 변경하고 현재 필터 조건을 유지한 채 재조회합니다.
  Future<void> updateSortType(OrderSortType sortType) async {
    state = state.copyWith(sortType: sortType);
    await searchOrders();
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

/// OrderList StateNotifier Provider
final orderListProvider =
    StateNotifierProvider<OrderListNotifier, OrderListState>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  final useCase = ref.watch(getMyOrdersUseCaseProvider);

  // Mock Repository인 경우 거래처 목록 접근용으로 전달
  final mockRepo = repository is OrderMockRepository ? repository : null;

  return OrderListNotifier(
    getMyOrders: useCase,
    mockRepository: mockRepo,
  );
});
