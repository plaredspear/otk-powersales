import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/presentation/widgets/order_form/order_product_card.dart';
import 'package:mobile/presentation/widgets/order_form/product_list_section.dart';

/// 20번째만 수량 0 인 53개 목록 — 사용자 제보 상황을 그대로 재현한다.
List<OrderDraftItem> _items({int count = 53, int zeroAt = 19}) {
  return List.generate(count, (i) {
    final boxes = i == zeroAt ? 0.0 : 1.0;
    return OrderDraftItem(
      productCode: 'P${i.toString().padLeft(3, '0')}',
      productName: '제품 $i',
      quantityBoxes: boxes,
      quantityPieces: 0,
      unitPrice: 1000,
      boxSize: 10,
      totalPrice: (boxes * 10 * 1000).round(),
    );
  });
}

/// 실제 화면과 같은 sliver 구성 (CustomScrollView + ProductListSection).
Widget _host({
  required List<OrderDraftItem> items,
  required ScrollController controller,
  required GlobalKey<ProductListSectionState> sectionKey,
  String? highlightedProductCode,
}) {
  return MaterialApp(
    home: Scaffold(
      body: CustomScrollView(
        controller: controller,
        slivers: [
          ProductListSection(
            key: sectionKey,
            items: items,
            validationErrors: const {},
            allItemsSelected: false,
            highlightedProductCode: highlightedProductCode,
            onToggleSelection: (_) {},
            onToggleSelectAll: () {},
            onAddProduct: () {},
            onBarcodeScan: () {},
            onRemoveSelected: () {},
            onQuantityChanged: (_, _, _) {},
          ),
        ],
      ),
    ),
  );
}

void main() {
  group('제품 목록 lazy build 전제', () {
    testWidgets('53개 중 화면 밖 20번 카드는 초기에 트리에 없다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);
      final sectionKey = GlobalKey<ProductListSectionState>();

      await tester.pumpWidget(
        _host(items: _items(), controller: controller, sectionKey: sectionKey),
      );

      // 이 전제가 깨지면(전부 build 되면) 페이지의 "내려가며 기다리는" 로직은 불필요해진다.
      expect(
        find.byKey(const ValueKey('order-product-P019')),
        findsNothing,
        reason: 'SliverList.builder 는 화면 밖 카드를 만들지 않는다',
      );
      expect(find.byKey(const ValueKey('order-product-P000')), findsOneWidget);
    });

    testWidgets('스크롤을 내리면 대상 카드가 트리에 올라온다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);
      final sectionKey = GlobalKey<ProductListSectionState>();

      await tester.pumpWidget(
        _host(items: _items(), controller: controller, sectionKey: sectionKey),
      );

      // 페이지의 _scrollToFirstZeroQuantity 와 같은 방식으로 한 화면씩 내려간다.
      final targetKey = const ValueKey('order-product-P019');
      var found = false;
      for (var attempt = 0; attempt < 60; attempt++) {
        if (find.byKey(targetKey).evaluate().isNotEmpty) {
          found = true;
          break;
        }
        final position = controller.position;
        if (position.pixels >= position.maxScrollExtent) break;
        controller.jumpTo(
          (position.pixels + position.viewportDimension * 0.8)
              .clamp(0.0, position.maxScrollExtent),
        );
        await tester.pump();
      }

      expect(found, isTrue, reason: '내려가면 20번 카드가 build 되어야 한다');
    });

    testWidgets('검색 중이면 대상이 필터에서 빠지고, clearSearch 로 복원된다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);
      final sectionKey = GlobalKey<ProductListSectionState>();

      await tester.pumpWidget(
        _host(items: _items(), controller: controller, sectionKey: sectionKey),
      );

      // 20번(제품 19)과 겹치지 않는 검색어로 목록을 좁힌다.
      // 카드마다 수량 입력 TextField 가 있으므로 툴바의 검색창을 hintText 로 특정한다.
      final searchField = find.byWidgetPredicate(
        (w) =>
            w is TextField &&
            w.decoration?.hintText == '추가한 제품 검색 (제품명·코드)',
      );
      expect(searchField, findsOneWidget);
      await tester.enterText(searchField, '제품 5');
      await tester.pump();
      expect(find.byKey(const ValueKey('order-product-P019')), findsNothing);

      sectionKey.currentState!.clearSearch();
      await tester.pump();

      // 필터가 풀려 첫 카드가 다시 보인다 (P019 는 여전히 화면 밖이라 lazy).
      expect(find.byKey(const ValueKey('order-product-P000')), findsOneWidget);
    });

    testWidgets('강조 대상 카드는 주황 배경으로 그려진다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);
      final sectionKey = GlobalKey<ProductListSectionState>();

      await tester.pumpWidget(
        _host(
          items: _items(),
          controller: controller,
          sectionKey: sectionKey,
          highlightedProductCode: 'P000',
        ),
      );

      // Card 는 OrderProductCard(키 보유) 의 자식이다.
      final card = tester.widget<Card>(
        find
            .descendant(
              of: find.byKey(const ValueKey('order-product-P000')),
              matching: find.byType(Card),
            )
            .first,
      );
      expect(card.color, AppColors.warningLight);
    });
  });
}
