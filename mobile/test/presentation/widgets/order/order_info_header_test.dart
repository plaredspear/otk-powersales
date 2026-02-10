import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/widgets/order/order_info_header.dart';

void main() {
  group('OrderInfoHeader Widget Tests', () {
    testWidgets('주문요청번호 표시', (WidgetTester tester) async {
      // given
      final orderDetail = _createOrderDetail();
      final widget = _buildWidget(orderDetail);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('OP00000001'), findsOneWidget);
    });

    testWidgets('거래처명 + 마감시간 표시', (WidgetTester tester) async {
      // given
      final orderDetail = _createOrderDetail(clientDeadlineTime: '13:40');
      final widget = _buildWidget(orderDetail);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('롯데마트 응암점 (13:40 마감)'), findsOneWidget);
    });

    testWidgets('거래처명만 표시 (마감시간 null)', (WidgetTester tester) async {
      // given
      final orderDetail = _createOrderDetail(clientDeadlineTime: null);
      final widget = _buildWidget(orderDetail);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('롯데마트 응암점'), findsOneWidget);
      expect(find.textContaining('마감'), findsNothing);
    });

    testWidgets('주문일 표시 (YYYY-MM-DD (요일) format)', (WidgetTester tester) async {
      // given
      final orderDetail = _createOrderDetail();
      final widget = _buildWidget(orderDetail);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.textContaining('2026-05-11'), findsOneWidget);
    });

    testWidgets('납기일 표시', (WidgetTester tester) async {
      // given
      final orderDetail = _createOrderDetail();
      final widget = _buildWidget(orderDetail);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.textContaining('2026-05-12'), findsOneWidget);
    });

    testWidgets('총 주문금액 표시', (WidgetTester tester) async {
      // given
      final orderDetail = _createOrderDetail();
      final widget = _buildWidget(orderDetail);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('100,000원'), findsOneWidget);
    });

    testWidgets('마감전에는 총 승인금액 미표시', (WidgetTester tester) async {
      // given
      final orderDetail = _createOrderDetail(isClosed: false);
      final widget = _buildWidget(orderDetail);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('총 승인 금액'), findsNothing);
    });

    testWidgets('마감후에는 총 승인금액 표시', (WidgetTester tester) async {
      // given
      final orderDetail = _createOrderDetail(
        isClosed: true,
        totalApprovedAmount: 85000,
      );
      final widget = _buildWidget(orderDetail);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('총 승인 금액'), findsOneWidget);
      expect(find.text('85,000원'), findsOneWidget);
    });

    testWidgets('승인상태 뱃지 표시', (WidgetTester tester) async {
      // given
      final orderDetail = _createOrderDetail(
        approvalStatus: ApprovalStatus.approved,
      );
      final widget = _buildWidget(orderDetail);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('승인완료'), findsOneWidget);
    });

    testWidgets('마감전에는 주문한 제품 수 미표시', (WidgetTester tester) async {
      // given
      final orderDetail = _createOrderDetail(
        isClosed: false,
        orderedItemCount: 3,
      );
      final widget = _buildWidget(orderDetail);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('주문한 제품 수'), findsNothing);
      expect(find.text('3개'), findsNothing);
    });

    testWidgets('마감후에는 주문한 제품 수 표시', (WidgetTester tester) async {
      // given
      final orderDetail = _createOrderDetail(
        isClosed: true,
        orderedItemCount: 3,
      );
      final widget = _buildWidget(orderDetail);

      // when
      await tester.pumpWidget(widget);
      await tester.pumpAndSettle();

      // then
      expect(find.text('주문한 제품 수'), findsOneWidget);
      expect(find.text('3개'), findsOneWidget);
    });
  });
}

OrderDetail _createOrderDetail({
  bool isClosed = false,
  String? clientDeadlineTime = '13:40',
  int? totalApprovedAmount,
  ApprovalStatus approvalStatus = ApprovalStatus.approved,
  int orderedItemCount = 3,
}) {
  return OrderDetail(
    id: 1,
    orderRequestNumber: 'OP00000001',
    clientId: 1,
    clientName: '롯데마트 응암점',
    clientDeadlineTime: clientDeadlineTime,
    orderDate: DateTime(2026, 5, 11),
    deliveryDate: DateTime(2026, 5, 12),
    totalAmount: 100000,
    totalApprovedAmount: totalApprovedAmount,
    approvalStatus: approvalStatus,
    isClosed: isClosed,
    orderedItemCount: orderedItemCount,
    orderedItems: [],
  );
}

Widget _buildWidget(OrderDetail detail) {
  return MaterialApp(
    home: Scaffold(
      body: SingleChildScrollView(
        child: OrderInfoHeader(orderDetail: detail),
      ),
    ),
  );
}
