import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/order_api_datasource.dart';
import '../../data/datasources/order_local_datasource.dart';
import '../../data/datasources/order_remote_datasource.dart';
import '../../data/models/my_store_model.dart';
import '../../data/repositories/order_repository_impl.dart';
import '../../domain/entities/order.dart';
import '../../domain/repositories/order_repository.dart';
import '../../domain/usecases/get_my_orders.dart';
import 'order_list_state.dart';

// --- Dependency Providers ---

/// Order Repository Provider
final orderRepositoryProvider = Provider<OrderRepository>((ref) {
  final dio = ref.watch(dioProvider);
  final remoteDataSource = OrderApiDataSource(dio);
  final localDataSource = OrderLocalDataSource();
  return OrderRepositoryImpl(
    remoteDataSource: remoteDataSource,
    localDataSource: localDataSource,
  );
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
  final Dio _dio;

  OrderListNotifier({
    required GetMyOrders getMyOrders,
    required Dio dio,
  })  : _getMyOrders = getMyOrders,
        _dio = dio,
        super(OrderListState.initial());

  /// 초기 데이터 로딩
  ///
  /// 거래처 목록 API 로딩 + 기본 필터로 주문 목록 조회
  Future<void> initialize() async {
    // GET /api/v1/stores/my 에서 거래처 목록 로딩
    try {
      final response = await _dio.get('/api/v1/stores/my');
      final data = response.data['data'] as Map<String, dynamic>;
      final storesJson = data['stores'] as List<dynamic>;
      final clientMap = <int, String>{};
      for (final store in storesJson) {
        final storeMap = store as Map<String, dynamic>;
        clientMap[storeMap['store_id'] as int] =
            storeMap['store_name'] as String;
      }
      state = state.copyWith(clients: clientMap);
    } catch (e) {
      // 거래처 목록 로드 실패: 빈 맵으로 설정 (드롭다운은 빈 상태)
      state = state.copyWith(clients: const {});
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
        extractErrorMessage(e),
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
        extractErrorMessage(e),
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
  final useCase = ref.watch(getMyOrdersUseCaseProvider);
  final dio = ref.watch(dioProvider);

  return OrderListNotifier(
    getMyOrders: useCase,
    dio: dio,
  );
});
