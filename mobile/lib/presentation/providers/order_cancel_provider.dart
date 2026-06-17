import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/utils/error_utils.dart';
import '../../domain/entities/order_detail.dart';
import '../../domain/usecases/cancel_order_usecase.dart';
import 'order_cancel_state.dart';
import 'order_request_list_provider.dart';

// --- Dependency Providers ---

/// CancelOrderUseCase Provider
final cancelOrderUseCaseProvider = Provider<CancelOrderUseCase>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
  return CancelOrderUseCase(repository);
});

// --- OrderCancelNotifier ---

/// 주문 취소 상태 관리 Notifier
///
/// 제품 선택/해제, 전체 선택, 주문 취소 API 호출을 관리합니다.
class OrderCancelNotifier extends StateNotifier<OrderCancelState> {
  final CancelOrderUseCase _cancelOrder;

  OrderCancelNotifier({
    required CancelOrderUseCase cancelOrderRequest,
    required int orderId,
    required List<OrderedItem> allItems,
  })  : _cancelOrder = cancelOrderRequest,
        super(OrderCancelState.initial(
          orderId: orderId,
          allItems: allItems,
        ));

  /// 개별 제품(라인) 선택/해제 토글
  void toggleProduct(int orderProductId) {
    final newSet = Set<int>.from(state.selectedOrderProductIds);
    if (newSet.contains(orderProductId)) {
      newSet.remove(orderProductId);
    } else {
      newSet.add(orderProductId);
    }
    state = state.copyWith(selectedOrderProductIds: newSet);
  }

  /// 전체 선택/해제 토글
  void toggleSelectAll() {
    if (state.isAllSelected) {
      // 전체 해제
      state = state.copyWith(selectedOrderProductIds: {});
    } else {
      // 전체 선택
      final allIds = state.cancellableItems
          .map((item) => item.orderProductId)
          .toSet();
      state = state.copyWith(selectedOrderProductIds: allIds);
    }
  }

  /// 주문 취소 실행
  ///
  /// 선택된 제품들의 주문을 취소합니다.
  /// Returns: 성공 여부
  Future<bool> cancelOrderRequest() async {
    if (!state.canCancel) return false;

    state = state.toLoading();

    try {
      await _cancelOrder.call(
        orderId: state.orderId,
        orderProductIds: state.selectedOrderProductIds.toList(),
      );
      state = state.toSuccess();
      return true;
    } catch (e) {
      final errorMsg = _parseErrorMessage(e);
      state = state.toError(errorMsg);
      return false;
    }
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(clearError: true);
  }

  /// 에러 메시지 파싱
  ///
  /// 실서버 응답(DioException)은 `error.code`(Spec #597) 를 우선 매핑하고,
  /// 매핑이 없으면 서버가 내려준 한글 메시지를 그대로 사용한다.
  /// 도메인 레벨 예외 등은 메시지 문자열 매칭으로 보강한다.
  String _parseErrorMessage(Object error) {
    final code = extractErrorCode(error);
    final mapped = code == null ? null : _messageForCode(code);
    if (mapped != null) return mapped;

    // DioException 인데 매핑된 코드가 없으면 서버 메시지 우선
    final serverMessage = extractErrorMessage(error);
    final codeMatched = _messageForCode(serverMessage);
    if (codeMatched != null) return codeMatched;

    return serverMessage;
  }

  /// 백엔드 errorCode / 메시지 문자열 → 사용자 메시지
  String? _messageForCode(String value) {
    // 주문 취소(Spec #597) 전용 코드
    if (value.contains('ORD_CANCEL_DEADLINE_PASSED')) {
      return '주문 취소 마감 시각이 지났습니다';
    }
    if (value.contains('ORD_CANCEL_INVALID_STATUS')) {
      return '취소할 수 없는 주문 상태입니다';
    }
    if (value.contains('ORD_CANCEL_LINE_NOT_FOUND')) {
      return '취소할 수 없는 제품이 포함되어 있습니다';
    }
    if (value.contains('ORD_CANCEL_SAP_FAILED')) {
      return '주문 취소 전송에 실패했습니다. 잠시 후 다시 시도해주세요';
    }
    // 공통 / 레거시 코드
    if (value.contains('ALREADY_CANCELLED')) {
      return '이미 취소된 제품이 포함되어 있습니다';
    }
    if (value.contains('ORDER_ALREADY_CLOSED')) {
      return '마감된 주문은 취소할 수 없습니다';
    }
    if (value.contains('ORDER_NOT_FOUND')) {
      return '주문을 찾을 수 없습니다';
    }
    if (value.contains('INVALID_PARAMETER')) {
      return '취소할 제품을 선택해주세요';
    }
    if (value.contains('UNAUTHORIZED')) {
      return '인증이 만료되었습니다. 다시 로그인해주세요';
    }
    if (value.contains('FORBIDDEN')) {
      return '접근 권한이 없습니다';
    }
    if (value.contains('SERVER_ERROR') || value.contains('500')) {
      return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요';
    }
    if (value.contains('네트워크') || value.contains('Network')) {
      return '네트워크 연결을 확인해주세요';
    }
    return null;
  }
}

/// OrderCancel StateNotifier Provider (family)
///
/// orderId와 제품 목록을 파라미터로 받습니다.
/// OrderCancelPage에서 생성 시 사용합니다.
final orderCancelProvider = StateNotifierProvider.autoDispose
    .family<OrderCancelNotifier, OrderCancelState, OrderCancelParams>(
  (ref, params) {
    final cancelOrderRequest = ref.watch(cancelOrderUseCaseProvider);

    return OrderCancelNotifier(
      cancelOrderRequest: cancelOrderRequest,
      orderId: params.orderId,
      allItems: params.allItems,
    );
  },
);

/// OrderCancelProvider 파라미터
class OrderCancelParams {
  final int orderId;
  final List<OrderedItem> allItems;

  const OrderCancelParams({
    required this.orderId,
    required this.allItems,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is OrderCancelParams &&
          runtimeType == other.runtimeType &&
          orderId == other.orderId;

  @override
  int get hashCode => orderId.hashCode;
}
