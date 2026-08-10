import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/presentation/widgets/order_form/order_product_card.dart';

/// 박스입수 10 → 총 EA = boxes × 10.
OrderDraftItem _item({double boxes = 1}) {
  return OrderDraftItem(
    productCode: 'P001',
    productName: '테스트제품',
    quantityBoxes: boxes,
    quantityPieces: 0,
    unitPrice: 1000,
    boxSize: 10,
    totalPrice: (boxes * 10 * 1000).round(),
  );
}

Widget _host(
  ValidationError? error, {
  double boxes = 1,
  bool highlighted = false,
}) {
  return MaterialApp(
    home: Scaffold(
      body: OrderProductCard(
        index: 0,
        item: _item(boxes: boxes),
        validationError: error,
        highlighted: highlighted,
        onSelectionChanged: (_) {},
        onQuantityChanged: (_, _) {},
      ),
    ),
  );
}

Color _cardBorderColor(WidgetTester tester) {
  final card = tester.widget<Card>(find.byType(Card));
  final shape = card.shape as RoundedRectangleBorder;
  return shape.side.color;
}

/// 사유 블록(카드 하단 배경) 색. 에러가 없으면 블록 자체가 없다.
Color? _reasonBlockColor(WidgetTester tester) {
  final finder = find.ancestor(
    of: find.text('공급제한수량 초과'),
    matching: find.byType(DecoratedBox),
  );
  if (finder.evaluate().isEmpty) return null;
  final box = tester.widget<DecoratedBox>(finder.first);
  return (box.decoration as BoxDecoration).color;
}

void main() {
  group('OrderProductCard 에러 표시', () {
    // 에러 시점 요청 수량 = 총 10개 (1박스 × 박스입수 10).
    const error = ValidationError(
      errorType: ValidationErrorType.supplyQuantity,
      message: '공급제한수량 초과',
      minOrderQuantity: 20,
      supplyQuantity: 0,
      requestedQuantity: 10,
    );

    testWidgets('에러 없음 → 기본 테두리', (tester) async {
      await tester.pumpWidget(_host(null));

      expect(_cardBorderColor(tester), AppColors.border);
      expect(find.text('공급제한수량 초과'), findsNothing);
    });

    testWidgets('현재 수량 == 에러 시점 수량 → 붉은 테두리 + 분홍 사유 블록', (tester) async {
      await tester.pumpWidget(_host(error, boxes: 1));

      expect(_cardBorderColor(tester), AppColors.error);
      expect(_reasonBlockColor(tester), AppColors.errorLight);
      expect(find.text('공급제한수량 초과'), findsOneWidget);
      expect(find.text('최소주문단위 20개  |  공급 0개'), findsOneWidget);
    });

    testWidgets('현재 수량 != 에러 시점 수량 → 테두리는 빨강 유지, 사유 블록만 연한 주황', (tester) async {
      await tester.pumpWidget(_host(error, boxes: 2));

      expect(_cardBorderColor(tester), AppColors.error);
      expect(_reasonBlockColor(tester), AppColors.warningLight);
      // 메시지/지표는 사라지지 않는다 — 배경색만 바뀐다.
      expect(find.text('공급제한수량 초과'), findsOneWidget);
      expect(find.text('최소주문단위 20개  |  공급 0개'), findsOneWidget);
    });

    testWidgets('수량을 줄여도(값이 다르면) 연한 주황', (tester) async {
      await tester.pumpWidget(_host(error, boxes: 0));

      expect(_reasonBlockColor(tester), AppColors.warningLight);
    });

    testWidgets('에러 시점 수량을 모르면(null) 분홍 유지', (tester) async {
      await tester.pumpWidget(
        _host(
          const ValidationError(
            errorType: ValidationErrorType.minOrderQuantity,
            message: '공급제한수량 초과',
          ),
          boxes: 3,
        ),
      );

      expect(_cardBorderColor(tester), AppColors.error);
      expect(_reasonBlockColor(tester), AppColors.errorLight);
    });
  });

  group('OrderProductCard 이동 강조', () {
    testWidgets('강조 없음 → 기본 테두리', (tester) async {
      await tester.pumpWidget(_host(null));

      expect(_cardBorderColor(tester), AppColors.border);
      expect(tester.widget<Card>(find.byType(Card)).color, isNull);
    });

    testWidgets('강조 → 주황 테두리 + 연한 주황 배경', (tester) async {
      await tester.pumpWidget(_host(null, highlighted: true));

      expect(_cardBorderColor(tester), AppColors.warning);
      expect(
        tester.widget<Card>(find.byType(Card)).color,
        AppColors.warningLight,
      );
    });

    testWidgets('강조는 에러 테두리(빨강)보다 우선한다', (tester) async {
      await tester.pumpWidget(
        _host(
          const ValidationError(
            errorType: ValidationErrorType.minOrderQuantity,
            message: '공급제한수량 초과',
          ),
          highlighted: true,
        ),
      );

      expect(_cardBorderColor(tester), AppColors.warning);
    });
  });
}
