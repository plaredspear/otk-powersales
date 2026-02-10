import '../../domain/entities/order.dart';
import '../../domain/entities/order_detail.dart';

/// 주문 상세 화면 상태
///
/// 주문 상세 데이터, 로딩/에러 상태, UI 상호작용 상태를 포함합니다.
class OrderDetailState {
  /// 주문 상세 데이터
  final OrderDetail? orderDetail;

  /// 로딩 상태 (초기 데이터 로딩)
  final bool isLoading;

  /// 재전송 로딩 상태
  final bool isResending;

  /// 에러 메시지
  final String? errorMessage;

  /// 마감후 제품 목록 펼침 상태
  final bool isItemsExpanded;

  const OrderDetailState({
    this.orderDetail,
    this.isLoading = false,
    this.isResending = false,
    this.errorMessage,
    this.isItemsExpanded = false,
  });

  /// 초기 상태
  factory OrderDetailState.initial() {
    return const OrderDetailState();
  }

  /// 데이터가 로드되었는지 여부
  bool get hasData => orderDetail != null;

  /// 마감전인지 여부
  bool get isBeforeClose => hasData && !orderDetail!.isClosed;

  /// 마감후인지 여부
  bool get isAfterClose => hasData && orderDetail!.isClosed;

  /// 마감후 + 반려제품 존재 여부
  bool get hasRejectedItems => isAfterClose && orderDetail!.hasRejectedItems;

  /// 주문취소 버튼 표시 여부
  /// 마감전 + 전송실패 아님 + 전체 취소 아님
  bool get showCancelButton =>
      isBeforeClose &&
      orderDetail!.approvalStatus != ApprovalStatus.sendFailed &&
      !orderDetail!.allItemsCancelled;

  /// 재전송 버튼 표시 여부
  /// 마감전 + 전송실패 상태
  bool get showResendButton =>
      isBeforeClose &&
      orderDetail!.approvalStatus == ApprovalStatus.sendFailed;

  /// 로딩 상태로 전환
  OrderDetailState toLoading() {
    return copyWith(
      isLoading: true,
      clearError: true,
    );
  }

  /// 에러 상태로 전환
  OrderDetailState toError(String message) {
    return copyWith(
      isLoading: false,
      isResending: false,
      errorMessage: message,
    );
  }

  OrderDetailState copyWith({
    OrderDetail? orderDetail,
    bool? isLoading,
    bool? isResending,
    String? errorMessage,
    bool? isItemsExpanded,
    bool clearError = false,
  }) {
    return OrderDetailState(
      orderDetail: orderDetail ?? this.orderDetail,
      isLoading: isLoading ?? this.isLoading,
      isResending: isResending ?? this.isResending,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      isItemsExpanded: isItemsExpanded ?? this.isItemsExpanded,
    );
  }
}
