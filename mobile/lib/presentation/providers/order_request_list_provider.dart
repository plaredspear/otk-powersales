import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/order_request_api_datasource.dart';
import '../../data/datasources/order_request_local_datasource.dart';
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
  final localDataSource = OrderRequestLocalDataSource();
  return OrderRequestRepositoryImpl(
    remoteDataSource: remoteDataSource,
    localDataSource: localDataSource,
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
  final Dio _dio;

  OrderRequestListNotifier({
    required GetMyOrderRequests getMyOrderRequests,
    required Dio dio,
  })  : _getMyOrderRequests = getMyOrderRequests,
        _dio = dio,
        super(OrderRequestListState.initial());

  /// 초기 데이터 로딩 — 거래처 목록 + 첫 fetch.
  Future<void> initialize() async {
    try {
      final response = await _dio.get('/api/v1/mobile/accounts/my');
      final data = response.data['data'] as Map<String, dynamic>;
      final accountsJson = data['accounts'] as List<dynamic>;
      final clientMap = <int, String>{};
      for (final account in accountsJson) {
        final accountMap = account as Map<String, dynamic>;
        clientMap[accountMap['accountId'] as int] =
            accountMap['accountName'] as String;
      }
      state = state.copyWith(clients: clientMap);
    } catch (e) {
      state = state.copyWith(clients: const {});
    }

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
  final dio = ref.watch(dioProvider);

  return OrderRequestListNotifier(
    getMyOrderRequests: useCase,
    dio: dio,
  );
});
