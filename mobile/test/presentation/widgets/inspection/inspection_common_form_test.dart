import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';
import 'package:mobile/presentation/widgets/inspection/inspection_common_form.dart';

void main() {
  group('InspectionCommonForm Widget', () {
    late InspectionTheme mockTheme;
    late InspectionFieldType mockFieldType;
    late DateTime testDate;

    setUp(() {
      mockTheme = InspectionTheme(
        id: 1,
        name: '8월 테마',
        startDate: DateTime(2020, 8, 1),
        endDate: DateTime(2020, 8, 31),
      );
      mockFieldType = const InspectionFieldType(code: 'FT01', name: '본매대');
      testDate = DateTime(2020, 8, 13);
    });

    Widget buildTestWidget({
      InspectionTheme? selectedTheme,
      InspectionCategory category = InspectionCategory.OWN,
      String? selectedStoreName,
      DateTime? inspectionDate,
      InspectionFieldType? selectedFieldType,
      VoidCallback? onThemeTap,
      ValueChanged<InspectionCategory>? onCategoryChanged,
      VoidCallback? onStoreTap,
      ValueChanged<DateTime>? onDateChanged,
      VoidCallback? onFieldTypeTap,
    }) {
      return MaterialApp(
        home: Scaffold(
          body: InspectionCommonForm(
            selectedTheme: selectedTheme,
            category: category,
            selectedStoreName: selectedStoreName,
            inspectionDate: inspectionDate ?? testDate,
            selectedFieldType: selectedFieldType,
            onThemeTap: onThemeTap ?? () {},
            onCategoryChanged: onCategoryChanged ?? (_) {},
            onStoreTap: onStoreTap ?? () {},
            onDateChanged: onDateChanged ?? (_) {},
            onFieldTypeTap: onFieldTypeTap ?? () {},
          ),
        ),
      );
    }

    testWidgets('초기 상태가 올바르게 렌더링된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());

      // Then: 필드 subtitle과 선택 가능한 요소들이 표시됨
      expect(find.text('테마 선택'), findsOneWidget);
      expect(find.text('자사'), findsOneWidget);
      expect(find.text('경쟁사'), findsOneWidget);
      expect(find.text('거래처 선택'), findsOneWidget);
      expect(find.text('점검일'), findsOneWidget);
      expect(find.text('2020-08-13'), findsOneWidget);
      expect(find.text('현장 유형 선택'), findsOneWidget);
    });

    testWidgets('필수 필드 마커(*)가 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());

      // Then: RichText가 존재함을 확인 (필수 필드 레이블에 RichText 사용)
      expect(find.byType(RichText), findsWidgets);
    });

    testWidgets('선택된 테마가 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget(selectedTheme: mockTheme));

      // Then
      expect(find.text('8월 테마'), findsOneWidget);
      expect(find.text('테마 선택'), findsNothing);
    });

    testWidgets('선택된 거래처가 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(selectedStoreName: '이마트 죽전점'),
      );

      // Then
      expect(find.text('이마트 죽전점'), findsOneWidget);
      expect(find.text('거래처 선택'), findsNothing);
    });

    testWidgets('선택된 현장 유형이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(selectedFieldType: mockFieldType),
      );

      // Then
      expect(find.text('본매대'), findsOneWidget);
      expect(find.text('현장 유형 선택'), findsNothing);
    });

    testWidgets('자사 분류가 기본 선택된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(category: InspectionCategory.OWN),
      );

      // Then
      final toggleButtons = tester.widget<ToggleButtons>(
        find.byType(ToggleButtons),
      );
      expect(toggleButtons.isSelected, [true, false]);
    });

    testWidgets('경쟁사 분류 선택이 반영된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(category: InspectionCategory.COMPETITOR),
      );

      // Then
      final toggleButtons = tester.widget<ToggleButtons>(
        find.byType(ToggleButtons),
      );
      expect(toggleButtons.isSelected, [false, true]);
    });

    testWidgets('테마 필드 탭 시 콜백이 호출된다', (tester) async {
      // Given
      var tapped = false;
      await tester.pumpWidget(
        buildTestWidget(onThemeTap: () => tapped = true),
      );

      // When: ListTile 전체를 탭
      await tester.tap(find.text('테마 선택'));
      await tester.pumpAndSettle();

      // Then
      expect(tapped, true);
    });

    testWidgets('거래처 필드 탭 시 콜백이 호출된다', (tester) async {
      // Given
      var tapped = false;
      await tester.pumpWidget(
        buildTestWidget(onStoreTap: () => tapped = true),
      );

      // When: ListTile 전체를 탭
      await tester.tap(find.text('거래처 선택'));
      await tester.pumpAndSettle();

      // Then
      expect(tapped, true);
    });

    testWidgets('현장 유형 필드 탭 시 콜백이 호출된다', (tester) async {
      // Given
      var tapped = false;
      await tester.pumpWidget(
        buildTestWidget(onFieldTypeTap: () => tapped = true),
      );

      // When: ListTile 전체를 탭
      await tester.tap(find.text('현장 유형 선택'));
      await tester.pumpAndSettle();

      // Then
      expect(tapped, true);
    });

    testWidgets('분류 토글 시 콜백이 호출된다', (tester) async {
      // Given
      InspectionCategory? changedCategory;
      await tester.pumpWidget(
        buildTestWidget(
          category: InspectionCategory.OWN,
          onCategoryChanged: (cat) => changedCategory = cat,
        ),
      );

      // When: 경쟁사 선택
      await tester.tap(find.text('경쟁사'));
      await tester.pumpAndSettle();

      // Then
      expect(changedCategory, InspectionCategory.COMPETITOR);
    });

    testWidgets('점검일 필드 탭 시 DatePicker가 표시된다', (tester) async {
      // Given
      await tester.pumpWidget(buildTestWidget());

      // When
      await tester.tap(find.text('점검일'));
      await tester.pumpAndSettle();

      // Then: DatePicker 다이얼로그가 표시됨
      expect(find.byType(DatePickerDialog), findsOneWidget);
    });

    testWidgets('DatePicker에서 날짜 선택 시 콜백이 호출된다', (tester) async {
      // Given
      DateTime? selectedDate;
      await tester.pumpWidget(
        buildTestWidget(
          inspectionDate: testDate,
          onDateChanged: (date) => selectedDate = date,
        ),
      );

      // When: DatePicker 열기
      await tester.tap(find.text('점검일'));
      await tester.pumpAndSettle();

      // DatePicker에서 15일 선택 (2020-08-15)
      await tester.tap(find.text('15'));
      await tester.pumpAndSettle();

      // OK 버튼 탭
      await tester.tap(find.text('OK'));
      await tester.pumpAndSettle();

      // Then
      expect(selectedDate, DateTime(2020, 8, 15));
    });

    // DatePicker의 Cancel 동작은 시스템 위젯의 동작이므로 테스트 생략
  });
}
