import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/theme/app_colors.dart';
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
            onQuantityChanged: (_, _, _) {},
          ),
          // 실제 화면(order_form_page)과 동일하게 섹션 뒤 하단 여백을 둔다.
          const SliverToBoxAdapter(child: SizedBox(height: 32)),
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

    testWidgets('그림자는 고정된 상태(스크롤 중)에서만 보인다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);
      final items = List.generate(30, _item);

      await tester.pumpWidget(_host(items, controller));

      BoxDecoration toolbarDecoration() {
        final box = tester.widget<DecoratedBox>(
          find
              .ancestor(
                of: find.text('선택 삭제'),
                matching: find.byType(DecoratedBox),
              )
              .last,
        );
        return box.decoration as BoxDecoration;
      }

      // 최상단 — 고정 전이라 그림자 없음
      expect(toolbarDecoration().boxShadow, isNull);

      // 스크롤 후 — 고정 상태라 그림자 표시
      controller.jumpTo(1200);
      await tester.pump();
      expect(toolbarDecoration().boxShadow, isNotNull);
    });

    testWidgets('스크롤 상태에서 검색해 결과가 줄어도 상단으로 튕기지 않는다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);
      final items = List.generate(30, _item);

      await tester.pumpWidget(_host(items, controller));
      controller.jumpTo(controller.position.maxScrollExtent);
      await tester.pump();

      // Act — 결과가 1건으로 줄어드는 검색어
      await tester.enterText(find.byType(TextField).first, '제품 7');
      await tester.pumpAndSettle();

      // Assert — 툴바는 그대로 고정, 상단 폼/헤더는 여전히 화면 밖
      expect(controller.offset, greaterThan(0));
      expect(find.text('선택 삭제'), findsOneWidget);
      expect(find.text('품목 추가는 100개 이하로 하시는 것을 권장합니다.'), findsNothing);
      expect(find.text('1개 표시 중 (전체 30개)'), findsOneWidget);

      // 결과 카드가 툴바에 가리지 않고 그 아래에서 시작한다.
      final toolbarBottom =
          tester.getBottomLeft(find.text('1개 표시 중 (전체 30개)')).dy;
      final cardTop = tester.getTopLeft(find.textContaining('테스트제품 7')).dy;
      expect(cardTop, greaterThanOrEqualTo(toolbarBottom));
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

  group('ProductListSection 담긴 건수 표시', () {
    testWidgets('검색 중이 아니어도 전체 건수를 상시 노출한다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);

      await tester.pumpWidget(_host(List.generate(30, _item), controller));

      expect(find.text('전체 30개'), findsOneWidget);
    });

    testWidgets('100개 이하면 초과 안내가 붙지 않고 회색으로 표시된다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);

      await tester.pumpWidget(_host(List.generate(100, _item), controller));

      expect(find.text('전체 100개'), findsOneWidget);
      expect(
        tester.widget<Text>(find.text('전체 100개')).style!.color,
        AppColors.textSecondary,
      );
    });

    testWidgets('100개 초과 시 초과 건수 + 빨강으로 알린다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);

      await tester.pumpWidget(_host(List.generate(105, _item), controller));

      const label = '전체 105개 · 5개 초과 — 100개 이하만 승인요청 가능';
      expect(find.text(label), findsOneWidget);
      expect(
        tester.widget<Text>(find.text(label)).style!.color,
        AppColors.error,
      );
    });

    testWidgets('초과 상태에서 검색하면 표시 건수와 초과 안내가 함께 보인다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);

      await tester.pumpWidget(_host(List.generate(105, _item), controller));
      await tester.enterText(find.byType(TextField).first, '제품 77');
      await tester.pump();

      expect(
        find.text('1개 표시 중 (전체 105개) · 5개 초과 — 100개 이하만 승인요청 가능'),
        findsOneWidget,
      );
    });

    testWidgets('제품이 없으면 건수 줄 자체가 없다', (tester) async {
      final controller = ScrollController();
      addTearDown(controller.dispose);

      await tester.pumpWidget(_host(const [], controller));

      // '전체 선택' 체크박스 라벨과 섞이지 않도록 건수 패턴으로 확인한다.
      expect(find.textContaining(RegExp(r'전체 \d+개')), findsNothing);
    });
  });
}
