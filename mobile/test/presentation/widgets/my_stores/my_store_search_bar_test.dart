import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/my_stores/my_store_search_bar.dart';

void main() {
  group('MyStoreSearchBar', () {
    late TextEditingController controller;

    setUp(() {
      controller = TextEditingController();
    });

    tearDown(() {
      controller.dispose();
    });

    Widget buildWidget({
      VoidCallback? onSearch,
      ValueChanged<String>? onChanged,
    }) {
      return MaterialApp(
        home: Scaffold(
          body: MyStoreSearchBar(
            controller: controller,
            onSearch: onSearch ?? () {},
            onChanged: onChanged,
          ),
        ),
      );
    }

    testWidgets('검색 입력 필드가 렌더링된다', (tester) async {
      await tester.pumpWidget(buildWidget());

      expect(find.byType(TextField), findsOneWidget);
    });

    testWidgets('검색 버튼이 렌더링된다', (tester) async {
      await tester.pumpWidget(buildWidget());

      expect(find.text('검색'), findsOneWidget);
    });

    testWidgets('힌트 텍스트가 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget());

      expect(find.text('거래처명, 거래처 코드 입력'), findsOneWidget);
    });

    testWidgets('검색 버튼 탭 시 콜백이 호출된다', (tester) async {
      var searchCalled = false;
      await tester.pumpWidget(buildWidget(
        onSearch: () => searchCalled = true,
      ));

      await tester.tap(find.text('검색'));
      await tester.pump();

      expect(searchCalled, true);
    });

    testWidgets('텍스트 입력 시 onChanged 콜백이 호출된다', (tester) async {
      String? changedValue;
      await tester.pumpWidget(buildWidget(
        onChanged: (value) => changedValue = value,
      ));

      await tester.enterText(find.byType(TextField), '경산');
      await tester.pump();

      expect(changedValue, '경산');
    });

    testWidgets('키보드 submit 시 검색이 실행된다', (tester) async {
      var searchCalled = false;
      await tester.pumpWidget(buildWidget(
        onSearch: () => searchCalled = true,
      ));

      await tester.enterText(find.byType(TextField), '테스트');
      await tester.testTextInput.receiveAction(TextInputAction.done);
      await tester.pump();

      expect(searchCalled, true);
    });
  });
}
