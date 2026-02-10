import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/product_search/product_search_app_bar.dart';

void main() {
  group('ProductSearchAppBar', () {
    late TextEditingController controller;

    setUp(() {
      controller = TextEditingController();
    });

    tearDown(() {
      controller.dispose();
    });

    Widget buildTestWidget({
      VoidCallback? onSearch,
      VoidCallback? onBack,
      ValueChanged<String>? onChanged,
      bool autofocus = false,
    }) {
      return MaterialApp(
        home: Scaffold(
          appBar: ProductSearchAppBar(
            controller: controller,
            onSearch: onSearch,
            onBack: onBack ?? () {},
            onChanged: onChanged,
            autofocus: autofocus,
          ),
        ),
      );
    }

    testWidgets('뒤로가기 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(buildTestWidget());

      expect(find.byIcon(Icons.arrow_back_ios), findsOneWidget);
    });

    testWidgets('검색 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(buildTestWidget());

      expect(find.byIcon(Icons.search), findsOneWidget);
    });

    testWidgets('검색어 입력 필드가 표시된다', (tester) async {
      await tester.pumpWidget(buildTestWidget());

      expect(find.byType(TextField), findsOneWidget);
    });

    testWidgets('힌트 텍스트가 표시된다', (tester) async {
      await tester.pumpWidget(buildTestWidget());

      expect(find.text('검색어 입력'), findsOneWidget);
    });

    testWidgets('뒤로가기 버튼 탭 시 콜백이 호출된다', (tester) async {
      var backPressed = false;
      await tester.pumpWidget(
        buildTestWidget(onBack: () => backPressed = true),
      );

      await tester.tap(find.byIcon(Icons.arrow_back_ios));
      expect(backPressed, isTrue);
    });

    testWidgets('검색 버튼 활성화 시 탭하면 콜백이 호출된다', (tester) async {
      var searchPressed = false;
      await tester.pumpWidget(
        buildTestWidget(onSearch: () => searchPressed = true),
      );

      await tester.tap(find.byIcon(Icons.search));
      expect(searchPressed, isTrue);
    });

    testWidgets('검색 버튼 비활성화 시 탭해도 콜백이 호출되지 않는다', (tester) async {
      var searchPressed = false;
      await tester.pumpWidget(
        buildTestWidget(onSearch: null),
      );

      await tester.tap(find.byIcon(Icons.search));
      expect(searchPressed, isFalse);
    });

    testWidgets('텍스트 입력 시 onChanged 콜백이 호출된다', (tester) async {
      String? changedValue;
      await tester.pumpWidget(
        buildTestWidget(onChanged: (v) => changedValue = v),
      );

      await tester.enterText(find.byType(TextField), '열라면');
      expect(changedValue, '열라면');
    });

    testWidgets('preferredSize가 56 높이를 반환한다', (tester) async {
      final appBar = ProductSearchAppBar(
        controller: controller,
        onBack: () {},
      );

      expect(appBar.preferredSize.height, 56);
    });
  });
}
