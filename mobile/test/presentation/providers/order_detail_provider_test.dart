import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/order_mock_repository.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/usecases/get_order_detail.dart';
import 'package:mobile/domain/usecases/resend_order.dart';
import 'package:mobile/presentation/providers/order_detail_provider.dart';
import 'package:mobile/presentation/providers/order_detail_state.dart';

/// 테스트용 OrderDetail 생성 헬퍼
OrderDetail _createOrderDetail({
  int id = 1,
  String orderRequestNumber = 'OP00000001',
  int clientId = 1,
  String clientName = 'Test Client',
  String? clientDeadlineTime,
  DateTime? orderDate,
  DateTime? deliveryDate,
  int totalAmount = 100000,
  int? totalApprovedAmount,
  ApprovalStatus approvalStatus = ApprovalStatus.approved,
  bool isClosed = false,
  int orderedItemCount = 2,
  List<OrderedItem>? orderedItems,
  OrderProcessingStatus? orderProcessingStatus,
  List<RejectedItem>? rejectedItems,
}) {
  return OrderDetail(
    id: id,
    orderRequestNumber: orderRequestNumber,
    clientId: clientId,
    clientName: clientName,
    clientDeadlineTime: clientDeadlineTime,
    orderDate: orderDate ?? DateTime(2026, 1, 15),
    deliveryDate: deliveryDate ?? DateTime(2026, 1, 16),
    totalAmount: totalAmount,
    totalApprovedAmount: totalApprovedAmount,
    approvalStatus: approvalStatus,
    isClosed: isClosed,
    orderedItemCount: orderedItemCount,
    orderedItems: orderedItems ?? [],
    orderProcessingStatus: orderProcessingStatus,
    rejectedItems: rejectedItems,
  );
}

void main() {
  group('OrderDetailState', () {
    test('initial state has correct defaults', () {
      final state = OrderDetailState.initial();

      expect(state.orderDetail, isNull);
      expect(state.isLoading, false);
      expect(state.isResending, false);
      expect(state.errorMessage, isNull);
      expect(state.isItemsExpanded, false);
    });

    test('toLoading sets isLoading true and clears error', () {
      final state = const OrderDetailState(
        isLoading: false,
        errorMessage: 'Some error',
      );

      final newState = state.toLoading();

      expect(newState.isLoading, true);
      expect(newState.errorMessage, isNull);
    });

    test('toError sets error message and clears loading', () {
      final state = const OrderDetailState(
        isLoading: true,
        isResending: true,
      );

      final newState = state.toError('Test error');

      expect(newState.isLoading, false);
      expect(newState.isResending, false);
      expect(newState.errorMessage, 'Test error');
    });

    test('copyWith works correctly', () {
      final orderDetail = _createOrderDetail();

      final state = OrderDetailState.initial();
      final newState = state.copyWith(
        orderDetail: orderDetail,
        isLoading: true,
        isResending: true,
        errorMessage: 'Error',
        isItemsExpanded: true,
      );

      expect(newState.orderDetail, orderDetail);
      expect(newState.isLoading, true);
      expect(newState.isResending, true);
      expect(newState.errorMessage, 'Error');
      expect(newState.isItemsExpanded, true);
    });

    test('copyWith with clearError removes error message', () {
      final state = const OrderDetailState(errorMessage: 'Some error');

      final newState = state.copyWith(clearError: true);

      expect(newState.errorMessage, isNull);
    });

    test('hasData is true when orderDetail is not null', () {
      final stateWithData =
          OrderDetailState(orderDetail: _createOrderDetail());
      final stateWithoutData = OrderDetailState.initial();

      expect(stateWithData.hasData, true);
      expect(stateWithoutData.hasData, false);
    });

    test('isBeforeClose is true when isClosed is false', () {
      final state = OrderDetailState(
        orderDetail: _createOrderDetail(isClosed: false),
      );

      expect(state.isBeforeClose, true);
      expect(state.isAfterClose, false);
    });

    test('isAfterClose is true when isClosed is true', () {
      final state = OrderDetailState(
        orderDetail: _createOrderDetail(isClosed: true),
      );

      expect(state.isAfterClose, true);
      expect(state.isBeforeClose, false);
    });

    test('hasRejectedItems is true when closed and has rejected items', () {
      final stateWithRejected = OrderDetailState(
        orderDetail: _createOrderDetail(
          isClosed: true,
          rejectedItems: [
            const RejectedItem(
              productCode: 'P001',
              productName: 'Product 1',
              orderQuantityBoxes: 10,
              rejectionReason: 'Out of stock',
            ),
          ],
        ),
      );

      final stateWithoutRejected = OrderDetailState(
        orderDetail: _createOrderDetail(isClosed: true),
      );

      expect(stateWithRejected.hasRejectedItems, true);
      expect(stateWithoutRejected.hasRejectedItems, false);
    });

    test('showCancelButton logic works correctly', () {
      // Should show: before close + not sendFailed + not all cancelled
      final stateShowCancel = OrderDetailState(
        orderDetail: _createOrderDetail(
          isClosed: false,
          approvalStatus: ApprovalStatus.approved,
          orderedItems: [
            const OrderedItem(
              productCode: 'P001',
              productName: 'Product 1',
              totalQuantityBoxes: 5,
              totalQuantityPieces: 100,
              isCancelled: false,
            ),
          ],
        ),
      );

      // Should not show: sendFailed status
      final stateSendFailed = OrderDetailState(
        orderDetail: _createOrderDetail(
          isClosed: false,
          approvalStatus: ApprovalStatus.sendFailed,
        ),
      );

      // Should not show: all items cancelled
      final stateAllCancelled = OrderDetailState(
        orderDetail: _createOrderDetail(
          isClosed: false,
          approvalStatus: ApprovalStatus.approved,
          orderedItems: [
            const OrderedItem(
              productCode: 'P001',
              productName: 'Product 1',
              totalQuantityBoxes: 5,
              totalQuantityPieces: 100,
              isCancelled: true,
            ),
          ],
        ),
      );

      // Should not show: after close
      final stateClosed = OrderDetailState(
        orderDetail: _createOrderDetail(
          isClosed: true,
          approvalStatus: ApprovalStatus.approved,
        ),
      );

      expect(stateShowCancel.showCancelButton, true);
      expect(stateSendFailed.showCancelButton, false);
      expect(stateAllCancelled.showCancelButton, false);
      expect(stateClosed.showCancelButton, false);
    });

    test('showResendButton logic works correctly', () {
      // Should show: before close + sendFailed
      final stateShowResend = OrderDetailState(
        orderDetail: _createOrderDetail(
          isClosed: false,
          approvalStatus: ApprovalStatus.sendFailed,
        ),
      );

      // Should not show: not sendFailed
      final stateNotSendFailed = OrderDetailState(
        orderDetail: _createOrderDetail(
          isClosed: false,
          approvalStatus: ApprovalStatus.approved,
        ),
      );

      // Should not show: after close
      final stateClosed = OrderDetailState(
        orderDetail: _createOrderDetail(
          isClosed: true,
          approvalStatus: ApprovalStatus.sendFailed,
        ),
      );

      expect(stateShowResend.showResendButton, true);
      expect(stateNotSendFailed.showResendButton, false);
      expect(stateClosed.showResendButton, false);
    });
  });

  group('OrderDetailNotifier', () {
    late OrderMockRepository mockRepository;
    late OrderDetailNotifier notifier;

    setUp(() {
      mockRepository = OrderMockRepository();
      final getOrderDetail = GetOrderDetail(mockRepository);
      final resendOrder = ResendOrder(mockRepository);
      notifier = OrderDetailNotifier(
        getOrderDetail: getOrderDetail,
        resendOrder: resendOrder,
      );
    });

    test('loadOrderDetail loads data correctly', () async {
      expect(notifier.state.orderDetail, isNull);
      expect(notifier.state.isLoading, false);

      await notifier.loadOrderDetail(orderId: 1);

      expect(notifier.state.orderDetail, isNotNull);
      expect(notifier.state.orderDetail!.id, 1);
      expect(notifier.state.isLoading, false);
      expect(notifier.state.errorMessage, isNull);
    });

    test('loadOrderDetail sets loading state during fetch', () async {
      final loadFuture = notifier.loadOrderDetail(orderId: 1);

      // Check loading state immediately
      expect(notifier.state.isLoading, true);
      expect(notifier.state.errorMessage, isNull);

      await loadFuture;

      expect(notifier.state.isLoading, false);
    });

    test('loadOrderDetail handles error correctly for non-existent order',
        () async {
      await notifier.loadOrderDetail(orderId: 9999);

      expect(notifier.state.orderDetail, isNull);
      expect(notifier.state.isLoading, false);
      expect(notifier.state.errorMessage, isNotNull);
      expect(notifier.state.errorMessage, contains('ORDER_NOT_FOUND'));
    });

    test('loadOrderDetail for closed order with processing status', () async {
      // orderId 6: 명품식자재, isClosed=true, approved → has processing status
      await notifier.loadOrderDetail(orderId: 6);

      expect(notifier.state.orderDetail, isNotNull);
      expect(notifier.state.orderDetail!.id, 6);
      expect(notifier.state.orderDetail!.isClosed, true);
      expect(notifier.state.orderDetail!.orderProcessingStatus, isNotNull);
      expect(notifier.state.isAfterClose, true);
    });

    test('loadOrderDetail for order with rejected items', () async {
      await notifier.loadOrderDetail(orderId: 1);

      expect(notifier.state.orderDetail, isNotNull);
      expect(notifier.state.orderDetail!.id, 1);
      expect(notifier.state.orderDetail!.isClosed, true);
      expect(notifier.state.orderDetail!.rejectedItems, isNotNull);
      expect(notifier.state.orderDetail!.rejectedItems!.isNotEmpty, true);
      expect(notifier.state.hasRejectedItems, true);
    });

    test('loadOrderDetail for sendFailed order', () async {
      await notifier.loadOrderDetail(orderId: 4);

      expect(notifier.state.orderDetail, isNotNull);
      expect(notifier.state.orderDetail!.id, 4);
      expect(notifier.state.orderDetail!.approvalStatus,
          ApprovalStatus.sendFailed);
      expect(notifier.state.orderDetail!.isClosed, false);
      expect(notifier.state.showResendButton, true);
    });

    test('resendOrder succeeds for sendFailed order and refreshes detail',
        () async {
      // First load the sendFailed order
      await notifier.loadOrderDetail(orderId: 4);
      expect(notifier.state.orderDetail!.approvalStatus,
          ApprovalStatus.sendFailed);

      // Resend the order
      final result = await notifier.resendOrder(orderId: 4);

      expect(result, true);
      expect(notifier.state.isResending, false);
      expect(notifier.state.errorMessage, isNull);
      // After resend, the order detail should be refreshed
      expect(notifier.state.orderDetail, isNotNull);
      expect(notifier.state.orderDetail!.id, 4);
    });

    test('resendOrder fails for non-sendFailed order and sets error',
        () async {
      // Load a normal order (not sendFailed)
      await notifier.loadOrderDetail(orderId: 1);
      expect(notifier.state.orderDetail!.approvalStatus,
          isNot(ApprovalStatus.sendFailed));

      // Try to resend
      final result = await notifier.resendOrder(orderId: 1);

      expect(result, false);
      expect(notifier.state.isResending, false);
      expect(notifier.state.errorMessage, isNotNull);
      expect(notifier.state.errorMessage, contains('INVALID_STATUS'));
    });

    test('resendOrder returns false on error', () async {
      // Try to resend non-existent order
      final result = await notifier.resendOrder(orderId: 9999);

      expect(result, false);
      expect(notifier.state.errorMessage, isNotNull);
    });

    test('toggleItemsExpanded toggles expansion state', () {
      expect(notifier.state.isItemsExpanded, false);

      notifier.toggleItemsExpanded();
      expect(notifier.state.isItemsExpanded, true);

      notifier.toggleItemsExpanded();
      expect(notifier.state.isItemsExpanded, false);
    });

    test('clearError clears error message', () async {
      // Trigger an error
      await notifier.loadOrderDetail(orderId: 9999);
      expect(notifier.state.errorMessage, isNotNull);

      // Clear the error
      notifier.clearError();
      expect(notifier.state.errorMessage, isNull);
    });

    test('full workflow: load, toggle, resend for sendFailed order', () async {
      // Step 1: Load sendFailed order
      await notifier.loadOrderDetail(orderId: 4);
      expect(notifier.state.orderDetail, isNotNull);
      expect(notifier.state.orderDetail!.approvalStatus,
          ApprovalStatus.sendFailed);
      expect(notifier.state.showResendButton, true);

      // Step 2: Toggle items expansion
      expect(notifier.state.isItemsExpanded, false);
      notifier.toggleItemsExpanded();
      expect(notifier.state.isItemsExpanded, true);

      // Step 3: Resend the order
      final resendResult = await notifier.resendOrder(orderId: 4);
      expect(resendResult, true);
      expect(notifier.state.errorMessage, isNull);

      // Verify state after resend
      expect(notifier.state.orderDetail, isNotNull);
      expect(notifier.state.orderDetail!.id, 4);
      expect(notifier.state.isResending, false);
    });
  });
}
