import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/usecases/get_order_request_detail.dart';
import '../../domain/usecases/resend_order_request.dart';
import 'order_request_detail_state.dart';
import 'order_request_list_provider.dart';

// --- Dependency Providers ---

/// GetOrderDetail UseCase Provider
final getOrderDetailUseCaseProvider = Provider<GetOrderDetail>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
  return GetOrderDetail(repository);
});

/// ResendOrder UseCase Provider
final resendOrderUseCaseProvider = Provider<ResendOrder>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
  return ResendOrder(repository);
});

// --- OrderRequestDetailNotifier ---

/// 주문 상세 상태 관리 Notifier
///
/// 주문 상세 조회, 재전송, 제품 목록 접기/펼치기를 관리합니다.
class OrderRequestDetailNotifier extends StateNotifier<OrderRequestDetailState> {
  final GetOrderDetail _getOrderDetail;
  final ResendOrder _resendOrder;

  OrderRequestDetailNotifier({
    required GetOrderDetail getOrderRequestDetail,
    required ResendOrder resendOrderRequest,
  })  : _getOrderDetail = getOrderRequestDetail,
        _resendOrder = resendOrderRequest,
        super(OrderRequestDetailState.initial());

  /// 주문 상세 조회
  ///
  /// [orderId]: 조회할 주문 ID
  Future<void> loadOrderDetail({required int orderId}) async {
    state = state.toLoading();

    try {
      final detail = await _getOrderDetail.call(orderId: orderId);
      state = state.copyWith(
        orderDetail: detail,
        isLoading: false,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 무음 갱신 — 로딩 스피너 없이 현재 주문 상세를 재조회.
  ///
  /// 등록 직후 과도상태(SENT / 등록 SAP 전송 in-flight) 주문의 상태 전이를 한시적 자동 폴링으로
  /// 반영하기 위한 백그라운드 갱신. 폴링 중 에러는 화면을 흔들지 않도록 조용히 무시한다.
  Future<void> refreshSilently({required int orderId}) async {
    try {
      final detail = await _getOrderDetail.call(orderId: orderId);
      state = state.copyWith(orderDetail: detail, errorMessage: null);
    } catch (_) {
      // 백그라운드 폴링 실패는 무시 — 화면 상태 유지.
    }
  }

  /// 주문 재전송
  ///
  /// 전송실패 상태의 주문을 재전송합니다.
  /// 성공 시 상세를 새로고침합니다.
  /// Returns: 성공 여부
  Future<bool> resendOrderRequest({required int orderId}) async {
    state = state.copyWith(isResending: true, clearError: true);

    try {
      await _resendOrder.call(orderId: orderId);
      // 재전송 성공 시 isResending 해제 후 상세 새로고침
      state = state.copyWith(isResending: false);
      await loadOrderDetail(orderId: orderId);
      return true;
    } catch (e) {
      state = state.copyWith(
        isResending: false,
        errorMessage: extractErrorMessage(e),
      );
      return false;
    }
  }

  /// 제품 목록 접기/펼치기 토글
  void toggleItemsExpanded() {
    state = state.copyWith(isItemsExpanded: !state.isItemsExpanded);
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(clearError: true);
  }
}

/// OrderDetail StateNotifier Provider
///
/// orderId를 family 파라미터로 받아 주문 상세를 관리합니다.
final orderRequestDetailProvider = StateNotifierProvider.family<OrderRequestDetailNotifier,
    OrderRequestDetailState, int>((ref, orderId) {
  final getOrderRequestDetail = ref.watch(getOrderDetailUseCaseProvider);
  final resendOrderRequest = ref.watch(resendOrderUseCaseProvider);

  final notifier = OrderRequestDetailNotifier(
    getOrderRequestDetail: getOrderRequestDetail,
    resendOrderRequest: resendOrderRequest,
  );

  // 자동으로 주문 상세 로딩
  notifier.loadOrderDetail(orderId: orderId);

  return notifier;
});
