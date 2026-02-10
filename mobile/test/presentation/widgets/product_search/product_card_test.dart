import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/product.dart';
import 'package:mobile/presentation/widgets/product_search/product_card.dart';

void main() {
  group('ProductCard', () {
    const testProduct = Product(
      productId: '18110014',
      productName: '열라면_용기105G',
      productCode: '18110014',
      barcode: '8801045570716',
      storageType: '상온',
      shelfLife: '7개월',
      categoryMid: '라면',
      categorySub: '용기면',
    );

    Widget buildTestWidget({
      VoidCallback? onClaimTap,
      VoidCallback? onOrderTap,
    }) {
      return MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: ProductCard(
              product: testProduct,
              onClaimTap: onClaimTap,
              onOrderTap: onOrderTap,
            ),
          ),
        ),
      );
    }

    testWidgets('제품명이 표시된다', (tester) async {
      await tester.pumpWidget(buildTestWidget());

      expect(find.text('열라면_용기105G'), findsOneWidget);
    });

    testWidgets('제품코드가 표시된다', (tester) async {
      await tester.pumpWidget(buildTestWidget());

      expect(find.text('18110014'), findsOneWidget);
    });

    testWidgets('바코드가 표시된다', (tester) async {
      await tester.pumpWidget(buildTestWidget());

      expect(find.text('8801045570716'), findsOneWidget);
    });

    testWidgets('보관조건이 표시된다', (tester) async {
      await tester.pumpWidget(buildTestWidget());

      expect(find.text('상온 | 7개월'), findsOneWidget);
    });

    testWidgets('클레임 등록 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(buildTestWidget());

      expect(find.text('클레임 등록'), findsOneWidget);
    });

    testWidgets('주문서 등록 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(buildTestWidget());

      expect(find.text('주문서 등록'), findsOneWidget);
    });

    testWidgets('클레임 등록 버튼 탭 시 콜백이 호출된다', (tester) async {
      var claimTapped = false;
      await tester.pumpWidget(
        buildTestWidget(onClaimTap: () => claimTapped = true),
      );

      await tester.tap(find.text('클레임 등록'));
      expect(claimTapped, isTrue);
    });

    testWidgets('주문서 등록 버튼 탭 시 콜백이 호출된다', (tester) async {
      var orderTapped = false;
      await tester.pumpWidget(
        buildTestWidget(onOrderTap: () => orderTapped = true),
      );

      await tester.tap(find.text('주문서 등록'));
      expect(orderTapped, isTrue);
    });

    testWidgets('라벨 텍스트가 올바르게 표시된다', (tester) async {
      await tester.pumpWidget(buildTestWidget());

      expect(find.text('제품코드'), findsOneWidget);
      expect(find.text('바코드'), findsOneWidget);
      expect(find.text('보관'), findsOneWidget);
    });
  });
}
