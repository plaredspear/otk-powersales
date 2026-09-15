import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/presentation/widgets/order_form/product_card_for_add.dart';

/// 시식·증정용 차단 — 레거시 `poplayer.js:38` (`tgType == 'x' || tgType == 'X'`) 정합.
///
/// 전용상품과 달리 레거시에 경로 예외도 제품코드 예외도 없으므로,
/// [ProductCardForAdd.blockExclusive] 와 무관하게 **모든 화면**에서 선택이 차단되어야 한다.
void main() {
  ProductForOrder product({
    String? tasteGiftType,
    String? productType,
    String productCode = 'P001',
  }) {
    return ProductForOrder(
      productCode: productCode,
      productName: '진라면_매운맛',
      barcode: '8801234567890',
      storageType: '실온',
      shelfLife: '12개월',
      unitPrice: 1000,
      boxSize: 10,
      isFavorite: false,
      productType: productType,
      tasteGiftType: tasteGiftType,
    );
  }

  Widget host(ProductForOrder p, {bool blockExclusive = false, ValueChanged<bool?>? onChanged}) {
    return MaterialApp(
      home: Scaffold(
        body: ProductCardForAdd(
          product: p,
          isSelected: false,
          onSelectionChanged: onChanged ?? (_) {},
          onFavoriteToggle: () {},
          blockExclusive: blockExclusive,
        ),
      ),
    );
  }

  testWidgets('시식·증정용은 주문서(blockExclusive=true)에서 차단 사유가 표시된다', (tester) async {
    await tester.pumpWidget(
      host(product(tasteGiftType: 'TASTING_GIFT'), blockExclusive: true),
    );

    expect(find.text('시식/증정용 상품은 추가할 수 없습니다.'), findsOneWidget);
    expect(find.text('시식/증정'), findsOneWidget);
    expect(find.byIcon(Icons.block), findsOneWidget);
    expect(find.byType(Checkbox), findsNothing);
  });

  testWidgets(
    '시식·증정용은 비주문 화면(blockExclusive=false)에서도 동일하게 차단된다',
    (tester) async {
      await tester.pumpWidget(host(product(tasteGiftType: 'TASTING_GIFT')));

      expect(find.text('시식/증정용 상품은 추가할 수 없습니다.'), findsOneWidget);
      expect(find.byIcon(Icons.block), findsOneWidget);
      expect(find.byType(Checkbox), findsNothing);
    },
  );

  testWidgets('시식·증정용 카드는 탭해도 선택 콜백이 호출되지 않는다', (tester) async {
    var changed = false;
    await tester.pumpWidget(
      host(
        product(tasteGiftType: 'TASTING_GIFT'),
        onChanged: (_) => changed = true,
      ),
    );

    await tester.tap(find.byType(InkWell).first);
    await tester.pump();

    expect(changed, false);
  });

  testWidgets('시식·증정용 차단에는 예외 제품코드가 없다 — 20010042 도 차단', (tester) async {
    await tester.pumpWidget(
      host(
        product(tasteGiftType: 'TASTING_GIFT', productCode: '20010042'),
        blockExclusive: true,
      ),
    );

    expect(find.text('시식/증정용 상품은 추가할 수 없습니다.'), findsOneWidget);
  });

  testWidgets('일반 제품은 차단되지 않는다 (오차단 방지)', (tester) async {
    await tester.pumpWidget(host(product()));

    expect(find.text('시식/증정용 상품은 추가할 수 없습니다.'), findsNothing);
    expect(find.text('시식/증정'), findsNothing);
    expect(find.byType(Checkbox), findsOneWidget);
  });

  testWidgets(
    '전용상품 + 시식·증정 동시 해당 시 전용상품 사유가 우선 (레거시 핸들러 평가 순서)',
    (tester) async {
      await tester.pumpWidget(
        host(
          product(tasteGiftType: 'TASTING_GIFT', productType: 'EXCLUSIVE'),
          blockExclusive: true,
        ),
      );

      expect(find.text('전용상품은 주문이 불가능합니다.'), findsOneWidget);
      expect(find.text('시식/증정용 상품은 추가할 수 없습니다.'), findsNothing);
      // 배지는 양쪽 모두 노출되어 사유를 식별할 수 있어야 한다.
      expect(find.text('전용상품'), findsOneWidget);
      expect(find.text('시식/증정'), findsOneWidget);
    },
  );
}
