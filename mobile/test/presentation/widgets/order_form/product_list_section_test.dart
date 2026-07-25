import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/presentation/widgets/order_form/product_list_section.dart';

OrderDraftItem _item(int index) {
  return OrderDraftItem(
    productCode: 'P${index.toString().padLeft(4, '0')}',
    productName: '테스트제품 $index',
    quantityBoxes: 0,
    quantityPieces: 0,
    unitPrice: 1000,
    boxSize: 10,
    totalPrice: 0,
  );
}

Widget _host(List<OrderDraftItem> items, ScrollController controller) {
  return MaterialApp(
    home: Scaffold(
      body: CustomScrollView(
        controller: controller,
        slivers: [
          const SliverToBoxAdapter(child: SizedBox(height: 200)),
          ProductListSection(
            items: items,
            validationErrors: const {},
            allItemsSelected: false,
            onToggleSelection: (_) {},
            onToggleSelectAll: () {},
            onAddProduct: () {},
            onBarcodeScan: () {},
            onRemoveSelected: () {},
            onQuantityChanged: (_, __, ___) {},
          ),
        ],
      ),
    ),
  );
}

void main() {
  group('ProductListSection 툴바 고정', () {
    testWidgets('목록을 스크롤해도 선택 삭제/전체 선택/검색이 화면에 남는다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);
      final items = List.generate(30, _item);

      await tester.pumpWidget(_host(items, controller));
      expect(find.text('선택 삭제'), findsOneWidget);
      expect(find.text('전체 선택'), findsOneWidget);
      expect(find.text('추가한 제품 검색 (제품명·코드)'), findsOneWidget);

      // Act — 헤더가 화면 밖으로 나갈 만큼 스크롤
      controller.jumpTo(1200);
      await tester.pump();

      // Assert — 툴바는 pinned 라 그대로 보이고, 스크롤되는 헤더는 사라진다
      expect(find.text('선택 삭제'), findsOneWidget);
      expect(find.text('전체 선택'), findsOneWidget);
      expect(find.text('추가한 제품 검색 (제품명·코드)'), findsOneWidget);
      expect(find.text('품목 추가는 100개 이하로 하시는 것을 권장합니다.'), findsNothing);
    });

    testWidgets('검색어 입력 시 목록이 필터되고 건수 안내가 툴바에 표시된다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);
      final items = List.generate(30, _item);

      await tester.pumpWidget(_host(items, controller));
      // 제품 카드에도 수량 입력 필드가 있어 툴바의 검색창(첫 필드)을 지정한다.
      await tester.enterText(find.byType(TextField).first, '제품 7');
      await tester.pump();

      expect(find.text('1개 표시 중 (전체 30개)'), findsOneWidget);
      expect(find.textContaining('테스트제품 7'), findsOneWidget);
    });
  });
}
