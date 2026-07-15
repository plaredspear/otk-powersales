import '../../domain/entities/order_detail.dart';

/// 주문 취소 화면 상태
///
/// 취소 가능한 제품 목록, 선택 상태, 로딩/에러 상태를 관리합니다.
class OrderCancelState {
  /// 주문 ID
  final int orderId;

  /// 취소 가능한 제품 목록 (isCancelled=false인 제품만)
  final List<OrderedItem> cancellableItems;

  /// 선택된 주문 라인 PK Set (`OrderRequestProduct.id`)
  final Set<int> selectedOrderProductIds;

  /// 취소 API 호출 중 여부
  final bool isCancelling;

  /// 에러 메시지
  final String? errorMessage;

  /// 취소 성공 여부 (성공 시 화면 닫기 트리거)
  final bool cancelSuccess;

  /// 취소 결과 미확정 여부 (timeout / 게이트웨이 5xx).
  ///
  /// 백엔드/SAP 는 요청을 마저 처리했을 수 있어 "실패" 로 단정할 수 없다. 이 경우
  /// 화면을 닫고 상세를 재조회해 실제 취소 반영 여부를 확인한다 (중복 전송/오표시 방지).
  final bool cancelInconclusive;

  const OrderCancelState({
    required this.orderId,
    this.cancellableItems = const [],
    this.selectedOrderProductIds = const {},
    this.isCancelling = false,
    this.errorMessage,
    this.cancelSuccess = false,
    this.cancelInconclusive = false,
  });

  /// 초기 상태
  factory OrderCancelState.initial({
    required int orderId,
    required List<OrderedItem> allItems,
  }) {
    final cancellable =
        allItems.where((item) => !item.isCancelled).toList();
    return OrderCancelState(
      orderId: orderId,
      cancellableItems: cancellable,
    );
  }

  /// 전체 선택 여부 (파생 상태)
  bool get isAllSelected =>
      cancellableItems.isNotEmpty &&
      selectedOrderProductIds.length == cancellableItems.length;

  /// 선택된 제품 수
  int get selectedCount => selectedOrderProductIds.length;

  /// 취소 버튼 활성화 여부
  bool get canCancel => selectedCount > 0 && !isCancelling;

  /// 취소 가능한 제품이 없는 경우
  bool get hasNoCancellableItems => cancellableItems.isEmpty;

  /// 로딩 상태로 전환
  OrderCancelState toLoading() {
    return copyWith(
      isCancelling: true,
      clearError: true,
    );
  }

  /// 에러 상태로 전환
  OrderCancelState toError(String message) {
    return copyWith(
      isCancelling: false,
      errorMessage: message,
    );
  }

  /// 취소 성공 상태로 전환
  OrderCancelState toSuccess() {
    return copyWith(
      isCancelling: false,
      cancelSuccess: true,
      clearError: true,
    );
  }

  /// 취소 결과 미확정 상태로 전환 (timeout / 게이트웨이 5xx).
  OrderCancelState toInconclusive() {
    return copyWith(
      isCancelling: false,
      cancelInconclusive: true,
      clearError: true,
    );
  }

  OrderCancelState copyWith({
    int? orderId,
    List<OrderedItem>? cancellableItems,
    Set<int>? selectedOrderProductIds,
    bool? isCancelling,
    String? errorMessage,
    bool? cancelSuccess,
    bool? cancelInconclusive,
    bool clearError = false,
  }) {
    return OrderCancelState(
      orderId: orderId ?? this.orderId,
      cancellableItems: cancellableItems ?? this.cancellableItems,
      selectedOrderProductIds:
          selectedOrderProductIds ?? this.selectedOrderProductIds,
      isCancelling: isCancelling ?? this.isCancelling,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      cancelSuccess: cancelSuccess ?? this.cancelSuccess,
      cancelInconclusive: cancelInconclusive ?? this.cancelInconclusive,
    );
  }
}
