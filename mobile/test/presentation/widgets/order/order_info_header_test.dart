import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/widgets/order/order_info_header.dart';

void main() {
  OrderDetail buildDetail({
    OrderItemCountSummary? summary,
    bool isClosed = true,
  }) {
    return OrderDetail(
      id: 1,
      orderRequestNumber: 'OP00889331',
      clientId: 5,
      clientName: '(주)오동',
      orderDate: DateTime(2026, 5, 4),
      deliveryDate: DateTime(2026, 5, 6),
      totalAmount: 2185427,
      totalApprovedAmount: 0,
      orderRequestStatus: 'APPROVED',
      orderRequestStatusName: '전송완료',
      isClosed: isClosed,
      orderedItemCount: 41,
      orderedItems: const [],
      itemCountSummary: summary,
    );
  }

  Future<void> pumpHeader(WidgetTester tester, OrderDetail detail) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: OrderInfoHeader(orderDetail: detail),
          ),
        ),
      ),
    );
  }

  group('OrderInfoHeader — 승인된 품목 수 (2026-08-04)', () {
    testWidgets('집계가 있으면 "주문한 품목 수" 행 유지 + "승인된 품목 수" 행 추가', (tester) async {
      await pumpHeader(
        tester,
        buildDetail(
          summary: const OrderItemCountSummary(
            orderedCount: 41,
            confirmedCount: 36,
            cancelledCount: 2,
            outOfStockCount: 3,
            rejectedCount: 0,
          ),
        ),
      );

      expect(find.text('주문한 품목 수'), findsOneWidget);
      expect(find.text('41개'), findsOneWidget);
      expect(find.text('승인된 품목 수'), findsOneWidget);
      expect(find.text('36개'), findsOneWidget);
    });

    testWidgets('SAP 데이터 없음(전부 0) → 승인된 품목 수 0개 노출', (tester) async {
      await pumpHeader(
        tester,
        buildDetail(
          summary: const OrderItemCountSummary(
            orderedCount: 41,
            confirmedCount: 0,
            cancelledCount: 0,
            outOfStockCount: 0,
            rejectedCount: 0,
          ),
        ),
      );

      expect(find.text('주문한 품목 수'), findsOneWidget);
      expect(find.text('승인된 품목 수'), findsOneWidget);
      expect(find.text('0개'), findsOneWidget);
    });

    testWidgets('집계 없음(구버전 서버) → 승인된 품목 수 행 미표시', (tester) async {
      await pumpHeader(tester, buildDetail());

      expect(find.text('주문한 품목 수'), findsOneWidget);
      expect(find.text('승인된 품목 수'), findsNothing);
    });

    testWidgets('info 아이콘 탭 → 집계 내역 바텀시트 (0 분류는 미표시)', (tester) async {
      await pumpHeader(
        tester,
        buildDetail(
          summary: const OrderItemCountSummary(
            orderedCount: 41,
            confirmedCount: 36,
            cancelledCount: 2,
            outOfStockCount: 3,
            rejectedCount: 0,
          ),
        ),
      );

      // 헤더에는 info 아이콘이 2개(주문 요청 상태 / 승인된 품목 수) — 뒤쪽이 품목 수 행.
      await tester.tap(find.byIcon(Icons.info_outline).last);
      await tester.pumpAndSettle();

      expect(find.text('승인된 품목 수 안내'), findsOneWidget);
      expect(find.text('주문 41개 중 출고 확정 36개'), findsOneWidget);
      expect(find.text('- 취소: 2개'), findsOneWidget);
      expect(find.text('- 미납: 3개'), findsOneWidget);
      // 반려 0 → 행 없음(각주의 "미납·반려" 문구와 구분되도록 행 포맷으로 매칭). 미집계 0 → 잔여 행 없음.
      expect(find.textContaining('- 반려:'), findsNothing);
      expect(find.textContaining('- 출고 확정 전:'), findsNothing);
      expect(find.text('※ 납품문서가 생성된 품목만 집계됩니다'), findsOneWidget);
    });

    testWidgets('납품문서 미생성 잔여분 → "출고 확정 전" 행으로 차이 설명', (tester) async {
      await pumpHeader(
        tester,
        buildDetail(
          summary: const OrderItemCountSummary(
            orderedCount: 41,
            confirmedCount: 30,
            cancelledCount: 2,
            outOfStockCount: 3,
            rejectedCount: 1,
          ),
        ),
      );

      await tester.tap(find.byIcon(Icons.info_outline).last);
      await tester.pumpAndSettle();

      expect(find.text('- 반려: 1개'), findsOneWidget);
      expect(find.text('- 출고 확정 전: 5개'), findsOneWidget);
    });
  });
}
