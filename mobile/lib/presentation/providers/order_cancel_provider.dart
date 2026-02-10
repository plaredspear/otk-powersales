import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/order_detail.dart';
import '../../domain/usecases/cancel_order_usecase.dart';
import 'order_cancel_state.dart';
import 'order_list_provider.dart';

// --- Dependency Providers ---

/// CancelOrderUseCase Provider
final cancelOrderUseCaseProvider = Provider<CancelOrderUseCase>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return CancelOrderUseCase(repository);
});

// --- OrderCancelNotifier ---

/// 주문 취소 상태 관리 Notifier
///
/// 제품 선택/해제, 전체 선택, 주문 취소 API 호출을 관리합니다.
class OrderCancelNotifier extends StateNotifier<OrderCancelState> {
  final CancelOrderUseCase _cancelOrder;

  OrderCancelNotifier({
    required CancelOrderUseCase cancelOrder,
    required int orderId,
    required List<OrderedItem> allItems,
  })  : _cancelOrder = cancelOrder,
        super(OrderCancelState.initial(
          orderId: orderId,
          allItems: allItems,
        ));

  /// 개별 제품 선택/해제 토글
  void toggleProduct(String productCode) {
    final newSet = Set<String>.from(state.selectedProductCodes);
    if (newSet.contains(productCode)) {
      newSet.remove(productCode);
    } else {
      newSet.add(productCode);
    }
    state = state.copyWith(selectedProductCodes: newSet);
  }

  /// 전체 선택/해제 토글
  void toggleSelectAll() {
    if (state.isAllSelected) {
      // 전체 해제
      state = state.copyWith(selectedProductCodes: {});
    } else {
      // 전체 선택
      final allCodes = state.cancellableItems
          .map((item) => item.productCode)
          .toSet();
      state = state.copyWith(selectedProductCodes: allCodes);
    }
  }

  /// 주문 취소 실행
  ///
  /// 선택된 제품들의 주문을 취소합니다.
  /// Returns: 성공 여부
  Future<bool> cancelOrder() async {
    if (!state.canCancel) return false;

    state = state.toLoading();

    try {
      await _cancelOrder.call(
        orderId: state.orderId,
        productCodes: state.selectedProductCodes.toList(),
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
  String _parseErrorMessage(Object error) {
    final message = error.toString().replaceFirst('Exception: ', '');

    if (message.contains('ALREADY_CANCELLED')) {
      return '이미 취소된 제품이 포함되어 있습니다';
    }
    if (message.contains('ORDER_ALREADY_CLOSED')) {
      return '마감된 주문은 취소할 수 없습니다';
    }
    if (message.contains('ORDER_NOT_FOUND')) {
      return '주문을 찾을 수 없습니다';
    }
    if (message.contains('INVALID_PARAMETER')) {
      return '취소할 제품을 선택해주세요';
    }
    if (message.contains('UNAUTHORIZED')) {
      return '인증이 만료되었습니다. 다시 로그인해주세요';
    }
    if (message.contains('FORBIDDEN')) {
      return '접근 권한이 없습니다';
    }
    if (message.contains('SERVER_ERROR') || message.contains('500')) {
      return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요';
    }
    if (message.contains('네트워크') || message.contains('Network')) {
      return '네트워크 연결을 확인해주세요';
    }

    return message;
  }
}

/// OrderCancel StateNotifier Provider (family)
///
/// orderId와 제품 목록을 파라미터로 받습니다.
/// OrderCancelPage에서 생성 시 사용합니다.
final orderCancelProvider = StateNotifierProvider.autoDispose
    .family<OrderCancelNotifier, OrderCancelState, OrderCancelParams>(
  (ref, params) {
    final cancelOrder = ref.watch(cancelOrderUseCaseProvider);

    return OrderCancelNotifier(
      cancelOrder: cancelOrder,
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
