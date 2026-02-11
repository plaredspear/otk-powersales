import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/usecases/get_client_order_detail_usecase.dart';
import 'client_order_detail_state.dart';
import 'order_list_provider.dart';

// --- Dependency Providers ---

/// GetClientOrderDetail UseCase Provider
final getClientOrderDetailUseCaseProvider =
    Provider<GetClientOrderDetailUseCase>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return GetClientOrderDetailUseCase(repository);
});

// --- ClientOrderDetailNotifier ---

/// 거래처별 주문 상세 상태 관리 Notifier
///
/// SAP 주문번호로 주문 상세 정보를 조회합니다.
class ClientOrderDetailNotifier extends StateNotifier<ClientOrderDetailState> {
  final GetClientOrderDetailUseCase _getDetail;

  ClientOrderDetailNotifier({
    required GetClientOrderDetailUseCase getClientOrderDetail,
  })  : _getDetail = getClientOrderDetail,
        super(ClientOrderDetailState.initial());

  /// 주문 상세 정보 조회
  Future<void> loadDetail(String sapOrderNumber) async {
    state = state.toLoading();

    try {
      final detail = await _getDetail.call(sapOrderNumber: sapOrderNumber);

      state = state.copyWith(
        isLoading: false,
        orderDetail: detail,
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

/// ClientOrderDetail StateNotifier Provider
final clientOrderDetailProvider =
    StateNotifierProvider<ClientOrderDetailNotifier, ClientOrderDetailState>(
        (ref) {
  final useCase = ref.watch(getClientOrderDetailUseCaseProvider);

  return ClientOrderDetailNotifier(
    getClientOrderDetail: useCase,
  );
});
