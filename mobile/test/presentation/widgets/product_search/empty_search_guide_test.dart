import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/product_search/empty_search_guide.dart';

void main() {
  group('EmptySearchGuide', () {
    Widget buildTestWidget({
      bool hasSearched = false,
      VoidCallback? onBarcodeTap,
    }) {
      return MaterialApp(
        home: Scaffold(
          body: EmptySearchGuide(
            hasSearched: hasSearched,
            onBarcodeTap: onBarcodeTap,
          ),
        ),
      );
    }

    group('초기 상태 (hasSearched = false)', () {
      testWidgets('검색 안내 메시지가 표시된다', (tester) async {
        await tester.pumpWidget(buildTestWidget());

        expect(
          find.text('제품명 또는 제품코드를 입력 후\n검색하세요'),
          findsOneWidget,
        );
      });

      testWidgets('검색 아이콘이 표시된다', (tester) async {
        await tester.pumpWidget(buildTestWidget());

        expect(find.byIcon(Icons.search), findsOneWidget);
      });

      testWidgets('바코드 검색 버튼이 표시된다', (tester) async {
        await tester.pumpWidget(
          buildTestWidget(onBarcodeTap: () {}),
        );

        expect(find.text('바코드로 검색'), findsOneWidget);
      });

      testWidgets('바코드 콜백이 null이면 버튼이 표시되지 않는다', (tester) async {
        await tester.pumpWidget(buildTestWidget());

        expect(find.text('바코드로 검색'), findsNothing);
      });

      testWidgets('바코드 검색 버튼 탭 시 콜백이 호출된다', (tester) async {
        var barcodeTapped = false;
        await tester.pumpWidget(
          buildTestWidget(onBarcodeTap: () => barcodeTapped = true),
        );

        await tester.tap(find.text('바코드로 검색'));
        expect(barcodeTapped, isTrue);
      });
    });

    group('검색 후 빈 결과 (hasSearched = true)', () {
      testWidgets('검색 결과 없음 메시지가 표시된다', (tester) async {
        await tester.pumpWidget(
          buildTestWidget(hasSearched: true),
        );

        expect(find.text('검색 결과가 없습니다'), findsOneWidget);
      });

      testWidgets('검색 없음 아이콘이 표시된다', (tester) async {
        await tester.pumpWidget(
          buildTestWidget(hasSearched: true),
        );

        expect(find.byIcon(Icons.search_off), findsOneWidget);
      });

      testWidgets('바코드 검색 버튼이 표시되지 않는다', (tester) async {
        await tester.pumpWidget(
          buildTestWidget(hasSearched: true, onBarcodeTap: () {}),
        );

        expect(find.text('바코드로 검색'), findsNothing);
      });
    });
  });
}
