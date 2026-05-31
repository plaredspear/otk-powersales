import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/product.dart';
import 'package:mobile/presentation/widgets/product_search/product_card.dart';

void main() {
  const product = Product(
    productId: 'P001',
    productName: '진라면',
    productCode: 'C001',
    barcode: '8800000000001',
    storageType: '상온',
    shelfLife: '12개월',
  );

  Widget wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

  group('ProductCard.showActions', () {
    testWidgets('기본(showActions=true)이면 클레임/주문서 버튼이 보인다', (tester) async {
      await tester.pumpWidget(wrap(const ProductCard(product: product)));

      expect(find.text('클레임 등록'), findsOneWidget);
      expect(find.text('주문서 등록'), findsOneWidget);
    });

    testWidgets('선택 모드(showActions=false)면 액션 버튼이 숨겨진다', (tester) async {
      await tester.pumpWidget(
        wrap(const ProductCard(product: product, showActions: false)),
      );

      expect(find.text('클레임 등록'), findsNothing);
      expect(find.text('주문서 등록'), findsNothing);
      // 제품 정보는 그대로 표시
      expect(find.text('진라면'), findsOneWidget);
    });

    testWidgets('카드 본문 탭 시 onTap 콜백이 호출된다', (tester) async {
      var tapped = false;
      await tester.pumpWidget(
        wrap(ProductCard(
          product: product,
          showActions: false,
          onTap: () => tapped = true,
        )),
      );

      await tester.tap(find.text('진라면'));
      expect(tapped, isTrue);
    });
  });
}
