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

  /// 주문 상세 조회는 SD03052 SAP 를 동기 호출해 latency 가 크다. 사용자가 pull-to-refresh 로
  /// 짧은 간격에 반복 당기면 SAP 왕복이 누적되므로, 마지막 성공 조회로부터 [_refreshCooldown]
  /// 이내의 **수동** 새로고침은 API 호출 없이 억제한다(자동 폴링/최초 진입/재전송 후 재조회는 예외).
  static const Duration _refreshCooldown = Duration(seconds: 30);

  /// 마지막으로 SAP 상세 조회에 성공한 시각. 쿨다운 판정 기준(수동 새로고침에만 적용).
  DateTime? _lastLoadedAt;

  OrderRequestDetailNotifier({
    required GetOrderDetail getOrderRequestDetail,
    required ResendOrder resendOrderRequest,
  })  : _getOrderDetail = getOrderRequestDetail,
        _resendOrder = resendOrderRequest,
        super(OrderRequestDetailState.initial());

  /// 주문 상세 조회
  ///
  /// [orderId]: 조회할 주문 ID
  /// [respectCooldown]: true 이면 마지막 성공 조회로부터 30초 이내 재조회를 억제한다
  ///   (pull-to-refresh 전용). 최초 진입/재전송 후 재조회는 false 로 항상 조회한다.
  Future<void> loadOrderDetail({
    required int orderId,
    bool respectCooldown = false,
  }) async {
    if (respectCooldown && _isWithinCooldown()) {
      // SAP 호출을 건너뛰고 안내만 노출 — 기존 화면 데이터는 그대로 유지.
      state = state.copyWith(
        isLoading: false,
        errorMessage: '방금 조회했습니다. 잠시 후 다시 시도해주세요.',
      );
      return;
    }

    state = state.toLoading();

    try {
      final detail = await _getOrderDetail.call(orderId: orderId);
      _lastLoadedAt = DateTime.now();
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

  /// 마지막 성공 조회로부터 쿨다운(30초) 이내인지 여부.
  bool _isWithinCooldown() {
    final last = _lastLoadedAt;
    if (last == null) return false;
    return DateTime.now().difference(last) < _refreshCooldown;
  }

  /// 무음 갱신 — 로딩 스피너 없이 현재 주문 상세를 재조회.
  ///
  /// 등록 직후 과도상태(SENT / 등록 SAP 전송 in-flight) 주문의 상태 전이를 한시적 자동 폴링으로
  /// 반영하기 위한 백그라운드 갱신. 폴링 중 에러는 화면을 흔들지 않도록 조용히 무시한다.
  Future<void> refreshSilently({required int orderId}) async {
    try {
      final detail = await _getOrderDetail.call(orderId: orderId);
      // 자동 폴링도 SAP 를 왕복했으므로 쿨다운 기준을 갱신 — 폴링 직후 수동 새로고침 억제.
      _lastLoadedAt = DateTime.now();
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
