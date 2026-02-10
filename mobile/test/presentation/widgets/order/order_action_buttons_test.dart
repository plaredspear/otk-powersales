import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/widgets/order/order_action_buttons.dart';

void main() {
  group('OrderActionButtons Widget Tests', () {
    testWidgets('주문취소 버튼 표시', (WidgetTester tester) async {
      // given
      final widget = _buildWidget(
        showCancelButton: true,
        showResendButton: false,
      );

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('주문 취소'), findsOneWidget);
      expect(find.text('재전송'), findsNothing);
    });

    testWidgets('재전송 버튼 표시', (WidgetTester tester) async {
      // given
      final widget = _buildWidget(
        showCancelButton: false,
        showResendButton: true,
      );

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('재전송'), findsOneWidget);
      expect(find.text('주문 취소'), findsNothing);
    });

    testWidgets('두 버튼 모두 미표시', (WidgetTester tester) async {
      // given
      final widget = _buildWidget(
        showCancelButton: false,
        showResendButton: false,
      );

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('주문 취소'), findsNothing);
      expect(find.text('재전송'), findsNothing);
    });

    testWidgets('주문취소 버튼 탭 콜백 호출', (WidgetTester tester) async {
      // given
      var callbackCalled = false;
      final widget = _buildWidget(
        showCancelButton: true,
        showResendButton: false,
        onCancel: () => callbackCalled = true,
      );

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();
      await tester.tap(find.text('주문 취소'));
      await tester.pumpAndSettle();

      // then
      expect(callbackCalled, isTrue);
    });

    testWidgets('재전송 버튼 탭 콜백 호출', (WidgetTester tester) async {
      // given
      var callbackCalled = false;
      final widget = _buildWidget(
        showCancelButton: false,
        showResendButton: true,
        onResend: () => callbackCalled = true,
      );

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();
      await tester.tap(find.text('재전송'));
      await tester.pumpAndSettle();

      // then
      expect(callbackCalled, isTrue);
    });

    testWidgets('재전송 중에는 로딩 인디케이터 표시', (WidgetTester tester) async {
      // given
      final widget = _buildWidget(
        showCancelButton: false,
        showResendButton: true,
        isResending: true,
      );

      // when
      await tester.pumpWidget(widget);
      await tester.pump();

      // then
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      expect(find.text('재전송'), findsNothing);
    });

    testWidgets('재전송 중에는 버튼 비활성화', (WidgetTester tester) async {
      // given
      var callbackCalled = false;
      final widget = _buildWidget(
        showCancelButton: false,
        showResendButton: true,
        isResending: true,
        onResend: () => callbackCalled = true,
      );

      // when
      await tester.pumpWidget(widget);
      await tester.pump();

      // then
      final button = tester.widget<ElevatedButton>(find.byType(ElevatedButton));
      expect(button.onPressed, isNull);
      expect(callbackCalled, isFalse);
    });
  });
}

OrderDetail _createOrderDetail() {
  return OrderDetail(
    id: 1,
    orderRequestNumber: 'OP00000001',
    clientId: 1,
    clientName: '롯데마트 응암점',
    clientDeadlineTime: '13:40',
    orderDate: DateTime(2026, 5, 11),
    deliveryDate: DateTime(2026, 5, 12),
    totalAmount: 100000,
    approvalStatus: ApprovalStatus.approved,
    isClosed: false,
    orderedItemCount: 3,
    orderedItems: [],
  );
}

Widget _buildWidget({
  required bool showCancelButton,
  required bool showResendButton,
  bool isResending = false,
  VoidCallback? onCancel,
  VoidCallback? onResend,
}) {
  return MaterialApp(
    home: Scaffold(
      body: OrderActionButtons(
        orderDetail: _createOrderDetail(),
        showCancelButton: showCancelButton,
        showResendButton: showResendButton,
        isResending: isResending,
        onCancel: onCancel,
        onResend: onResend,
      ),
    ),
  );
}
