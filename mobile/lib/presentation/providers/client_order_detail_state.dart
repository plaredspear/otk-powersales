import '../../domain/entities/client_order.dart';

/// 거래처별 주문 상세 화면 상태
class ClientOrderDetailState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 주문 상세 정보
  final ClientOrderDetail? orderDetail;

  const ClientOrderDetailState({
    this.isLoading = false,
    this.errorMessage,
    this.orderDetail,
  });

  /// 초기 상태
  factory ClientOrderDetailState.initial() {
    return const ClientOrderDetailState();
  }

  /// 로딩 상태로 전환
  ClientOrderDetailState toLoading() {
    return copyWith(
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  ClientOrderDetailState toError(String message) {
    return copyWith(
      isLoading: false,
      errorMessage: message,
    );
  }

  /// 데이터가 있는지 여부
  bool get hasData => orderDetail != null;

  ClientOrderDetailState copyWith({
    bool? isLoading,
    String? errorMessage,
    ClientOrderDetail? orderDetail,
  }) {
    return ClientOrderDetailState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      orderDetail: orderDetail ?? this.orderDetail,
    );
  }
}
