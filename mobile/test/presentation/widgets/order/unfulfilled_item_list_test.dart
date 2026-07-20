import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/widgets/order/unfulfilled_item_list.dart';

void main() {
  Widget buildList(List<UnfulfilledItem> items) {
    return MaterialApp(
      home: Scaffold(body: UnfulfilledItemList(unfulfilledItems: items)),
    );
  }

  group('UnfulfilledItemList - 미납 제품 섹션 (신규 정책)', () {
    testWidgets('헤더에 미납 제품 건수 + 제품명(코드) + 수량 + 미납사유 표시', (tester) async {
      await tester.pumpWidget(buildList([
        const UnfulfilledItem(
          productCode: '1000023',
          productName: '진라면 매운맛',
          orderQuantityBoxes: 7,
          reason: '배차 미확정',
        ),
        const UnfulfilledItem(
          productCode: '2000045',
          productName: '참기름',
          orderQuantityBoxes: 2.5,
          reason: '부분 납품 보류',
        ),
      ]));

      expect(find.text('미납 제품 (2)'), findsOneWidget);
      expect(find.text('진라면 매운맛 (1000023)'), findsOneWidget);
      expect(find.text('7 BOX'), findsOneWidget);
      expect(find.text('미납사유: 배차 미확정'), findsOneWidget);
      // 소수 박스는 후행 소수 유지 (RejectedItemList 포맷 정합)
      expect(find.text('2.5 BOX'), findsOneWidget);
      expect(find.text('미납사유: 부분 납품 보류'), findsOneWidget);
    });
  });
}
