import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/common/search_filter_widget.dart';

void main() {
  group('SearchFilterWidget - Dropdown', () {
    final testOptions = [
      const FilterOption(label: '이마트', value: 'emart'),
      const FilterOption(label: '홈플러스', value: 'homeplus'),
      const FilterOption(label: '롯데마트', value: 'lotte'),
    ];

    testWidgets('드롭다운 위젯이 올바르게 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SearchFilterWidget(
              label: '매장',
              filterType: FilterType.dropdown,
              options: testOptions,
            ),
          ),
        ),
      );

      // 레이블이 표시되는지 확인
      expect(find.text('매장'), findsOneWidget);

      // 드롭다운 버튼이 표시되는지 확인
      expect(find.byType(DropdownButton<String>), findsOneWidget);
    });

    testWidgets('초기값이 올바르게 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SearchFilterWidget(
              label: '매장',
              filterType: FilterType.dropdown,
              options: testOptions,
              initialValue: 'emart',
            ),
          ),
        ),
      );

      // 초기값이 표시되는지 확인
      expect(find.text('이마트'), findsOneWidget);
    });

    testWidgets('드롭다운을 탭하면 옵션 목록이 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SearchFilterWidget(
              label: '매장',
              filterType: FilterType.dropdown,
              options: testOptions,
            ),
          ),
        ),
      );

      // 드롭다운 버튼 탭
      await tester.tap(find.byType(DropdownButton<String>));
      await tester.pumpAndSettle();

      // 모든 옵션이 표시되는지 확인
      expect(find.text('이마트'), findsWidgets);
      expect(find.text('홈플러스'), findsOneWidget);
      expect(find.text('롯데마트'), findsOneWidget);
    });

    testWidgets('옵션 선택 시 onChanged 콜백이 호출된다', (WidgetTester tester) async {
      String? selectedValue;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SearchFilterWidget(
              label: '매장',
              filterType: FilterType.dropdown,
              options: testOptions,
              onChanged: (value) {
                selectedValue = value;
              },
            ),
          ),
        ),
      );

      // 드롭다운 열기
      await tester.tap(find.byType(DropdownButton<String>));
      await tester.pumpAndSettle();

      // '홈플러스' 선택
      await tester.tap(find.text('홈플러스').last);
      await tester.pumpAndSettle();

      // 콜백이 호출되고 값이 올바른지 확인
      expect(selectedValue, 'homeplus');
      expect(find.text('홈플러스'), findsOneWidget);
    });

    testWidgets('힌트 텍스트가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SearchFilterWidget(
              label: '매장',
              filterType: FilterType.dropdown,
              options: testOptions,
              hintText: '매장을 선택하세요',
            ),
          ),
        ),
      );

      // 힌트 텍스트가 표시되는지 확인
      expect(find.text('매장을 선택하세요'), findsOneWidget);
    });
  });

  group('SearchFilterWidget - TextInput', () {
    testWidgets('텍스트 입력 위젯이 올바르게 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SearchFilterWidget(
              label: '제품명',
              filterType: FilterType.textInput,
            ),
          ),
        ),
      );

      // 레이블이 표시되는지 확인
      expect(find.text('제품명'), findsOneWidget);

      // TextField가 표시되는지 확인
      expect(find.byType(TextField), findsOneWidget);

      // 검색 아이콘이 표시되는지 확인
      expect(find.byIcon(Icons.search), findsOneWidget);
    });

    testWidgets('초기값이 올바르게 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SearchFilterWidget(
              label: '제품명',
              filterType: FilterType.textInput,
              initialValue: '진라면',
            ),
          ),
        ),
      );

      // 초기값이 TextField에 표시되는지 확인
      expect(find.text('진라면'), findsOneWidget);
    });

    testWidgets('힌트 텍스트가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SearchFilterWidget(
              label: '제품명',
              filterType: FilterType.textInput,
              hintText: '제품명을 입력하세요',
            ),
          ),
        ),
      );

      // TextField 찾기
      final textField = tester.widget<TextField>(find.byType(TextField));

      // 힌트 텍스트 확인
      expect(textField.decoration!.hintText, '제품명을 입력하세요');
    });

    testWidgets('텍스트 입력 시 onChanged 콜백이 호출된다', (WidgetTester tester) async {
      String? changedValue;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SearchFilterWidget(
              label: '제품명',
              filterType: FilterType.textInput,
              onChanged: (value) {
                changedValue = value;
              },
            ),
          ),
        ),
      );

      // 텍스트 입력
      await tester.enterText(find.byType(TextField), '진라면');
      await tester.pump();

      // 콜백이 호출되고 값이 올바른지 확인
      expect(changedValue, '진라면');
    });

    testWidgets('검색 아이콘 클릭 시 onSearch 콜백이 호출된다', (WidgetTester tester) async {
      String? searchedValue;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SearchFilterWidget(
              label: '제품명',
              filterType: FilterType.textInput,
              onSearch: (value) {
                searchedValue = value;
              },
            ),
          ),
        ),
      );

      // 텍스트 입력
      await tester.enterText(find.byType(TextField), '진라면');
      await tester.pump();

      // 검색 아이콘 클릭
      await tester.tap(find.byIcon(Icons.search));
      await tester.pump();

      // onSearch 콜백이 호출되고 값이 올바른지 확인
      expect(searchedValue, '진라면');
    });

    testWidgets('엔터 키 입력 시 onSearch 콜백이 호출된다', (WidgetTester tester) async {
      String? searchedValue;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SearchFilterWidget(
              label: '제품명',
              filterType: FilterType.textInput,
              onSearch: (value) {
                searchedValue = value;
              },
            ),
          ),
        ),
      );

      // 텍스트 입력 후 엔터 키 입력 (testTextInput은 onSubmitted를 트리거)
      await tester.enterText(find.byType(TextField), '진라면');
      await tester.testTextInput.receiveAction(TextInputAction.done);
      await tester.pump();

      // onSearch 콜백이 호출되고 값이 올바른지 확인
      expect(searchedValue, '진라면');
    });
  });

  group('FilterOption', () {
    test('FilterOption equality가 올바르게 동작한다', () {
      const option1 = FilterOption(label: '이마트', value: 'emart');
      const option2 = FilterOption(label: '이마트', value: 'emart');
      const option3 = FilterOption(label: '홈플러스', value: 'homeplus');

      expect(option1 == option2, true);
      expect(option1 == option3, false);
    });

    test('FilterOption hashCode가 올바르게 동작한다', () {
      const option1 = FilterOption(label: '이마트', value: 'emart');
      const option2 = FilterOption(label: '이마트', value: 'emart');

      expect(option1.hashCode == option2.hashCode, true);
    });
  });
}
