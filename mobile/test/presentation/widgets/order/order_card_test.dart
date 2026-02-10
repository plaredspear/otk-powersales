import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/presentation/widgets/order/approval_status_badge.dart';
import 'package:mobile/presentation/widgets/order/order_card.dart';
import 'package:mobile/presentation/widgets/order/order_sort_bottom_sheet.dart';

void main() {
  setUpAll(() async {
    await initializeDateFormatting('ko_KR', null);
  });

  // Test data
  final testOrder = Order(
    id: 1,
    orderRequestNumber: 'OP00000074',
    clientId: 123,
    clientName: '천사푸드',
    orderDate: DateTime(2025, 1, 15), // 수요일
    deliveryDate: DateTime(2025, 1, 20), // 월요일
    totalAmount: 612000000,
    approvalStatus: ApprovalStatus.approved,
    isClosed: false,
  );

  group('OrderCard', () {
    testWidgets('renders order request number text', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OrderCard(order: testOrder),
          ),
        ),
      );

      expect(find.text('주문 요청번호 : OP00000074'), findsOneWidget);
    });

    testWidgets('renders client name', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OrderCard(order: testOrder),
          ),
        ),
      );

      expect(find.text('천사푸드'), findsOneWidget);
    });

    testWidgets('renders order date with weekday format', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OrderCard(order: testOrder),
          ),
        ),
      );

      // 2025-01-15는 수요일
      expect(find.text('2025-01-15(수)'), findsOneWidget);
    });

    testWidgets('renders delivery date with weekday format', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OrderCard(order: testOrder),
          ),
        ),
      );

      // 2025-01-20는 월요일
      expect(find.text('2025-01-20(월)'), findsOneWidget);
    });

    testWidgets('renders formatted total amount with comma separators',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OrderCard(order: testOrder),
          ),
        ),
      );

      expect(find.text('612,000,000원'), findsOneWidget);
    });

    testWidgets('renders approval status badge', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OrderCard(order: testOrder),
          ),
        ),
      );

      expect(find.byType(ApprovalStatusBadge), findsOneWidget);
    });

    testWidgets('calls onTap callback when tapped', (WidgetTester tester) async {
      bool tapped = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OrderCard(
              order: testOrder,
              onTap: () {
                tapped = true;
              },
            ),
          ),
        ),
      );

      await tester.tap(find.byType(OrderCard));
      await tester.pumpAndSettle();

      expect(tapped, isTrue);
    });

    testWidgets('works without onTap (null)', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OrderCard(order: testOrder, onTap: null),
          ),
        ),
      );

      // Should not throw error when tapped
      await tester.tap(find.byType(OrderCard));
      await tester.pumpAndSettle();

      // No exception means test passed
    });
  });

  group('ApprovalStatusBadge', () {
    testWidgets('shows correct displayName for each status',
        (WidgetTester tester) async {
      for (final status in ApprovalStatus.values) {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ApprovalStatusBadge(status: status),
            ),
          ),
        );

        expect(find.text(status.displayName), findsOneWidget);

        // Clean up for next iteration
        await tester.pumpWidget(Container());
      }
    });

    testWidgets('shows badge for approved status', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ApprovalStatusBadge(status: ApprovalStatus.approved),
          ),
        ),
      );

      expect(find.text('승인완료'), findsOneWidget);
      expect(find.byType(ApprovalStatusBadge), findsOneWidget);
    });

    testWidgets('shows badge for pending status', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ApprovalStatusBadge(status: ApprovalStatus.pending),
          ),
        ),
      );

      expect(find.text('승인상태'), findsOneWidget);
      expect(find.byType(ApprovalStatusBadge), findsOneWidget);
    });

    testWidgets('shows badge for sendFailed status', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ApprovalStatusBadge(status: ApprovalStatus.sendFailed),
          ),
        ),
      );

      expect(find.text('전송실패'), findsOneWidget);
      expect(find.byType(ApprovalStatusBadge), findsOneWidget);
    });

    testWidgets('shows badge for resend status', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ApprovalStatusBadge(status: ApprovalStatus.resend),
          ),
        ),
      );

      expect(find.text('재전송'), findsOneWidget);
      expect(find.byType(ApprovalStatusBadge), findsOneWidget);
    });
  });

  group('OrderSortBottomSheet', () {
    testWidgets('shows all 6 sort options', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OrderSortBottomSheet(
              currentSortType: OrderSortType.latestOrder,
              onSortChanged: (_) {},
            ),
          ),
        ),
      );

      // Verify all 6 sort options are shown
      expect(find.text('최신주문일순'), findsOneWidget);
      expect(find.text('오래된주문일순'), findsOneWidget);
      expect(find.text('최신납기일순'), findsOneWidget);
      expect(find.text('오래된납기일순'), findsOneWidget);
      expect(find.text('금액높은순'), findsOneWidget);
      expect(find.text('금액낮은순'), findsOneWidget);
    });

    testWidgets('shows check mark on current sort type',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OrderSortBottomSheet(
              currentSortType: OrderSortType.amountHigh,
              onSortChanged: (_) {},
            ),
          ),
        ),
      );

      // Check mark should be present (Icons.check)
      expect(find.byIcon(Icons.check), findsOneWidget);
    });

    testWidgets('calls onSortChanged when option tapped',
        (WidgetTester tester) async {
      OrderSortType? selectedSortType;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OrderSortBottomSheet(
              currentSortType: OrderSortType.latestOrder,
              onSortChanged: (sortType) {
                selectedSortType = sortType;
              },
            ),
          ),
        ),
      );

      // Tap on a different sort option
      await tester.tap(find.text('금액높은순'));
      await tester.pumpAndSettle();

      // Verify callback was called with correct sort type
      expect(selectedSortType, equals(OrderSortType.amountHigh));
    });
  });
}
