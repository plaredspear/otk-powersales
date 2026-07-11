import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/order_request_api_datasource.dart';
import '../../data/repositories/order_request_repository_impl.dart';
import '../../domain/entities/order_request.dart';
import '../../domain/repositories/order_request_repository.dart';
import '../../domain/usecases/get_my_order_requests.dart';
import 'order_request_list_state.dart';

// --- Dependency Providers ---

/// OrderRequest Repository Provider
final orderRequestRepositoryProvider = Provider<OrderRequestRepository>((ref) {
  final dio = ref.watch(dioProvider);
  final remoteDataSource = OrderRequestApiDataSource(dio);
  return OrderRequestRepositoryImpl(
    remoteDataSource: remoteDataSource,
  );
});

/// GetMyOrderRequests UseCase Provider
final getMyOrderRequestsUseCaseProvider = Provider<GetMyOrderRequests>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
  return GetMyOrderRequests(repository);
});

// --- OrderRequestListNotifier ---

/// 본인 주문요청 목록 상태 관리 Notifier (클라이언트 슬라이스 패턴).
///
/// API 1회 호출 → 전체 배열 보관 → 페이지 클릭 시 슬라이스 재계산.
class OrderRequestListNotifier extends StateNotifier<OrderRequestListState> {
  final GetMyOrderRequests _getMyOrderRequests;

  OrderRequestListNotifier({
    required GetMyOrderRequests getMyOrderRequests,
  })  : _getMyOrderRequests = getMyOrderRequests,
        super(OrderRequestListState.initial());

  /// 초기 데이터 로딩 — 첫 fetch.
  ///
  /// 거래처 목록은 [AccountSelectorField] 바텀시트가 온디맨드로 조회하므로 프리로드하지 않는다.
  Future<void> initialize() async {
    await searchOrders();
  }

  /// 주문요청 목록 검색 — 1회 fetch + currentPage 0 리셋.
  Future<void> searchOrders() async {
    state = state.toLoading();

    try {
      final result = await _getMyOrderRequests.call(
        clientId: state.selectedClientId,
        status: state.selectedStatus,
        deliveryDateFrom: state.deliveryDateFrom,
        deliveryDateTo: state.deliveryDateTo,
        sortBy: state.sortType.sortBy,
        sortDir: state.sortType.sortDir,
      );

      state = state.copyWith(
        isLoading: false,
        allOrderRequests: result.orders,
        truncated: result.truncated,
        fetchedAt: result.fetchedAt,
        currentPage: 0,
        hasSearched: true,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(extractErrorMessage(e));
    }
  }

  /// 무음 갱신 — 로딩 스피너/페이지 리셋 없이 현재 조건으로 재조회.
  ///
  /// 등록 직후 과도상태(SENT) 주문의 상태 전이를 한시적 자동 폴링으로 반영하기 위한 백그라운드 갱신.
  /// 현재 페이지를 보존하되, 결과가 줄어 페이지 범위를 벗어나면 0으로 클램프한다.
  /// 폴링 중 에러는 화면을 흔들지 않도록 조용히 무시(다음 폴링/수동 새로고침에서 복구).
  Future<void> refreshSilently() async {
    try {
      final result = await _getMyOrderRequests.call(
        clientId: state.selectedClientId,
        status: state.selectedStatus,
        deliveryDateFrom: state.deliveryDateFrom,
        deliveryDateTo: state.deliveryDateTo,
        sortBy: state.sortType.sortBy,
        sortDir: state.sortType.sortDir,
      );

      final newTotalPages = result.orders.isEmpty
          ? 0
          : (result.orders.length / state.pageSize).ceil();
      final clampedPage =
          state.currentPage >= newTotalPages ? 0 : state.currentPage;

      state = state.copyWith(
        allOrderRequests: result.orders,
        truncated: result.truncated,
        fetchedAt: result.fetchedAt,
        currentPage: clampedPage,
        errorMessage: null,
      );
    } catch (_) {
      // 백그라운드 폴링 실패는 무시 — 화면 상태 유지.
    }
  }

  /// 페이지 이동 (추가 API 호출 없음 — 클라이언트 슬라이스).
  void goToPage(int page) {
    if (page < 0 || page >= state.totalPages) return;
    state = state.copyWith(currentPage: page);
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

  /// 정렬 변경 (재조회).
  Future<void> updateSortType(OrderSortType sortType) async {
    state = state.copyWith(sortType: sortType);
    await searchOrders();
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

/// OrderRequestList StateNotifier Provider
final orderRequestListProvider =
    StateNotifierProvider<OrderRequestListNotifier, OrderRequestListState>((ref) {
  final useCase = ref.watch(getMyOrderRequestsUseCaseProvider);

  return OrderRequestListNotifier(
    getMyOrderRequests: useCase,
  );
});
