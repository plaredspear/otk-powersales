import '../../domain/entities/order_request.dart';
import '../../domain/entities/order_detail.dart';

/// 주문 상세 화면 상태
///
/// 주문 상세 데이터, 로딩/에러 상태, UI 상호작용 상태를 포함합니다.
class OrderRequestDetailState {
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

  const OrderRequestDetailState({
    this.orderDetail,
    this.isLoading = false,
    this.isResending = false,
    this.errorMessage,
    this.isItemsExpanded = false,
  });

  /// 초기 상태
  factory OrderRequestDetailState.initial() {
    return const OrderRequestDetailState();
  }

  /// 데이터가 로드되었는지 여부
  bool get hasData => orderDetail != null;

  /// 마감전인지 여부
  bool get isBeforeClose => hasData && !orderDetail!.isClosed;

  /// 마감후인지 여부
  bool get isAfterClose => hasData && orderDetail!.isClosed;

  /// 반려제품 존재 여부 — 레거시(view.jsp:284-322 before / 449-486 after) 동등으로
  /// 반려 섹션은 마감 전후 모두 표시하므로 마감 여부 게이트 없음.
  bool get hasRejectedItems => hasData && orderDetail!.hasRejectedItems;

  /// 주문취소 버튼 표시 여부
  /// 마감전 + 전송실패 아님 + 전체 취소 아님 + 서버 취소 가능(cancelable).
  /// `cancelable` 은 서버 취소 가드(상태+마감+등록 SAP 전송 not in-flight)와 정합하는 단일 진실원 —
  /// 등록 전송 진행 중(in-flight)엔 false 라 "버튼은 떴는데 409" 불일치를 차단한다.
  bool get showCancelButton =>
      isBeforeClose &&
      orderDetail!.orderRequestStatus != OrderStatusCode.sendFailed &&
      !orderDetail!.allItemsCancelled &&
      orderDetail!.cancelable;

  /// 전송 처리 중 안내 표시 여부 — 등록 SAP 전송이 진행 중이라 아직 취소할 수 없는 상태.
  /// 취소 버튼 대신 "전송 처리 중, 잠시 후 취소 가능" 안내를 노출하기 위함.
  bool get showRegistrationInFlightNotice =>
      isBeforeClose &&
      !orderDetail!.allItemsCancelled &&
      orderDetail!.registrationInFlight;

  /// 전송 처리 중(과도상태)인지 여부 — 한시적 자동 폴링 판정에 사용.
  /// 등록 SAP 전송이 outbox in-flight(registrationInFlight) 이거나 주문 상태가 SENT(전송 중) 이면
  /// 아직 APPROVED/SEND_FAILED 로 확정되지 않은 구간이라 폴링으로 전이를 반영한다.
  bool get hasTransientRegistration =>
      hasData &&
      (orderDetail!.registrationInFlight ||
          orderDetail!.orderRequestStatus == OrderStatusCode.sent);

  /// 재전송 버튼 표시 여부
  /// 마감전 + 전송실패 상태
  bool get showResendButton =>
      isBeforeClose &&
      orderDetail!.orderRequestStatus == OrderStatusCode.sendFailed;

  /// 로딩 상태로 전환
  OrderRequestDetailState toLoading() {
    return copyWith(
      isLoading: true,
      clearError: true,
    );
  }

  /// 에러 상태로 전환
  OrderRequestDetailState toError(String message) {
    return copyWith(
      isLoading: false,
      isResending: false,
      errorMessage: message,
    );
  }

  OrderRequestDetailState copyWith({
    OrderDetail? orderDetail,
    bool? isLoading,
    bool? isResending,
    String? errorMessage,
    bool? isItemsExpanded,
    bool clearError = false,
  }) {
    return OrderRequestDetailState(
      orderDetail: orderDetail ?? this.orderDetail,
      isLoading: isLoading ?? this.isLoading,
      isResending: isResending ?? this.isResending,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      isItemsExpanded: isItemsExpanded ?? this.isItemsExpanded,
    );
  }
}
