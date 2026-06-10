import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/utils/error_utils.dart';
import '../../domain/usecases/get_client_orders_usecase.dart';
import 'client_order_list_state.dart';
import 'order_request_list_provider.dart';

// --- Dependency Providers ---

/// GetClientOrders UseCase Provider
final getClientOrdersUseCaseProvider = Provider<GetClientOrdersUseCase>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
  return GetClientOrdersUseCase(repository);
});

// --- ClientOrderListNotifier ---

/// 거래처별 주문 목록 상태 관리 Notifier
///
/// 거래처 선택, 납기일 선택, 페이지네이션 기능을 관리합니다.
class ClientOrderListNotifier extends StateNotifier<ClientOrderListState> {
  final GetClientOrdersUseCase _getClientOrders;

  ClientOrderListNotifier({
    required GetClientOrdersUseCase getClientOrders,
  })  : _getClientOrders = getClientOrders,
        super(ClientOrderListState.initial());

  /// 거래처 선택
  void selectAccount(int? accountId, String? accountName) {
    if (accountId == null) {
      state = state.copyWith(clearAccountFilter: true);
    } else {
      state = state.copyWith(
        selectedAccountId: accountId,
        selectedAccountName: accountName,
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
    if (state.selectedAccountId == null) return;

    state = state.toLoading();

    try {
      final result = await _getClientOrders.call(
        clientId: state.selectedAccountId!,
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
        extractErrorMessage(e),
      );
    }
  }

  /// 특정 페이지로 이동
  Future<void> goToPage(int page) async {
    if (state.selectedAccountId == null) return;

    state = state.toLoading();

    try {
      final result = await _getClientOrders.call(
        clientId: state.selectedAccountId!,
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
        extractErrorMessage(e),
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
  final useCase = ref.watch(getClientOrdersUseCaseProvider);

  return ClientOrderListNotifier(
    getClientOrders: useCase,
  );
});
