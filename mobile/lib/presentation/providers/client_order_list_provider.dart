import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/mock/order_mock_repository.dart';
import '../../domain/usecases/get_client_orders_usecase.dart';
import 'client_order_list_state.dart';
import 'order_list_provider.dart';

// --- Dependency Providers ---

/// GetClientOrders UseCase Provider
final getClientOrdersUseCaseProvider = Provider<GetClientOrdersUseCase>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return GetClientOrdersUseCase(repository);
});

// --- ClientOrderListNotifier ---

/// 거래처별 주문 목록 상태 관리 Notifier
///
/// 거래처 선택, 납기일 선택, 페이지네이션 기능을 관리합니다.
class ClientOrderListNotifier extends StateNotifier<ClientOrderListState> {
  final GetClientOrdersUseCase _getClientOrders;
  final OrderMockRepository? _mockRepository;

  ClientOrderListNotifier({
    required GetClientOrdersUseCase getClientOrders,
    OrderMockRepository? mockRepository,
  })  : _getClientOrders = getClientOrders,
        _mockRepository = mockRepository,
        super(ClientOrderListState.initial());

  /// 초기 데이터 로딩
  ///
  /// 거래처 목록 로딩
  Future<void> initialize() async {
    // Mock Repository에서 거래처 목록 로딩
    if (_mockRepository != null) {
      state = state.copyWith(stores: _mockRepository.mockClients);
    }
  }

  /// 거래처 선택
  void selectStore(int? storeId, String? storeName) {
    if (storeId == null) {
      state = state.copyWith(clearStoreFilter: true);
    } else {
      state = state.copyWith(
        selectedStoreId: storeId,
        selectedStoreName: storeName,
      );
    }
  }

  /// 납기일 변경
  void updateDeliveryDate(String date) {
    state = state.copyWith(selectedDeliveryDate: date);
  }

  /// 주문 목록 검색 실행
  ///
  /// 선택된 거래처와 납기일로 첫 페이지부터 검색합니다.
  /// 거래처 선택은 필수입니다.
  Future<void> searchOrders() async {
    if (state.selectedStoreId == null) return;

    state = state.toLoading();

    try {
      final result = await _getClientOrders.call(
        clientId: state.selectedStoreId!,
        deliveryDate: state.selectedDeliveryDate,
        page: 0,
      );

      state = state.copyWith(
        isLoading: false,
        orders: result.orders,
        totalElements: result.totalElements,
        totalPages: result.totalPages,
        currentPage: result.currentPage,
        isFirst: result.isFirst,
        isLast: result.isLast,
        hasSearched: true,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 특정 페이지로 이동
  Future<void> goToPage(int page) async {
    if (state.selectedStoreId == null) return;

    state = state.toLoading();

    try {
      final result = await _getClientOrders.call(
        clientId: state.selectedStoreId!,
        deliveryDate: state.selectedDeliveryDate,
        page: page,
      );

      state = state.copyWith(
        isLoading: false,
        orders: result.orders,
        totalElements: result.totalElements,
        totalPages: result.totalPages,
        currentPage: result.currentPage,
        isFirst: result.isFirst,
        isLast: result.isLast,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

/// ClientOrderList StateNotifier Provider
final clientOrderListProvider =
    StateNotifierProvider<ClientOrderListNotifier, ClientOrderListState>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  final useCase = ref.watch(getClientOrdersUseCaseProvider);

  // Mock Repository인 경우 거래처 목록 접근용으로 전달
  final mockRepo = repository is OrderMockRepository ? repository : null;

  return ClientOrderListNotifier(
    getClientOrders: useCase,
    mockRepository: mockRepo,
  );
});
