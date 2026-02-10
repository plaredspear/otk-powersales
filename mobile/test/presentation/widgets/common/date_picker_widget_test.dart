import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/intl.dart';
import 'package:mobile/presentation/widgets/common/date_picker_widget.dart';

void main() {
  group('DatePickerWidget', () {
    final dateFormat = DateFormat('yyyy-MM-dd');

    testWidgets('위젯이 올바르게 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: DatePickerWidget(),
          ),
        ),
      );

      // 시작일과 종료일 레이블이 표시되는지 확인
      expect(find.text('시작일'), findsOneWidget);
      expect(find.text('종료일'), findsOneWidget);

      // 캘린더 아이콘이 2개 표시되는지 확인
      expect(find.byIcon(Icons.calendar_today), findsNWidgets(2));
    });

    testWidgets('초기 날짜가 올바르게 표시된다', (WidgetTester tester) async {
      final startDate = DateTime(2026, 1, 1);
      final endDate = DateTime(2026, 1, 31);

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: DatePickerWidget(
              initialStartDate: startDate,
              initialEndDate: endDate,
            ),
          ),
        ),
      );

      // 초기 날짜가 올바르게 표시되는지 확인
      expect(find.text(dateFormat.format(startDate)), findsOneWidget);
      expect(find.text(dateFormat.format(endDate)), findsOneWidget);
    });

    testWidgets('커스텀 레이블이 올바르게 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: DatePickerWidget(
              startDateLabel: '시작',
              endDateLabel: '종료',
            ),
          ),
        ),
      );

      expect(find.text('시작'), findsOneWidget);
      expect(find.text('종료'), findsOneWidget);
    });

    testWidgets('시작일 버튼을 탭하면 DatePicker가 열린다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: DatePickerWidget(),
          ),
        ),
      );

      // 시작일 컨테이너를 찾아서 탭
      final startDateButton = find.ancestor(
        of: find.text('시작일'),
        matching: find.byType(InkWell),
      );

      await tester.tap(startDateButton);
      await tester.pumpAndSettle();

      // DatePicker 다이얼로그가 표시되는지 확인
      expect(find.byType(DatePickerDialog), findsOneWidget);
    });

    testWidgets('종료일 버튼을 탭하면 DatePicker가 열린다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: DatePickerWidget(),
          ),
        ),
      );

      // 종료일 컨테이너를 찾아서 탭
      final endDateButton = find.ancestor(
        of: find.text('종료일'),
        matching: find.byType(InkWell),
      ).last;

      await tester.tap(endDateButton);
      await tester.pumpAndSettle();

      // DatePicker 다이얼로그가 표시되는지 확인
      expect(find.byType(DatePickerDialog), findsOneWidget);
    });

    testWidgets('시작일 선택 시 콜백이 호출된다', (WidgetTester tester) async {
      DateTime? selectedStartDate;
      DateTime? callbackStartDate;
      DateTime? callbackEndDate;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: DatePickerWidget(
              initialStartDate: DateTime(2026, 1, 15),
              initialEndDate: DateTime(2026, 1, 31),
              onStartDateChanged: (date) {
                selectedStartDate = date;
              },
              onDateRangeChanged: (start, end) {
                callbackStartDate = start;
                callbackEndDate = end;
              },
            ),
          ),
        ),
      );

      // 시작일 버튼 탭
      final startDateButton = find.ancestor(
        of: find.text('시작일'),
        matching: find.byType(InkWell),
      );

      await tester.tap(startDateButton);
      await tester.pumpAndSettle();

      // DatePicker에서 날짜 선택 (10일 선택)
      await tester.tap(find.text('10'));
      await tester.pumpAndSettle();

      // OK 버튼 클릭
      await tester.tap(find.text('OK'));
      await tester.pumpAndSettle();

      // 콜백이 호출되었는지 확인
      expect(selectedStartDate, isNotNull);
      expect(selectedStartDate!.day, 10);
      expect(callbackStartDate, isNotNull);
      expect(callbackEndDate, isNotNull);
    });

    testWidgets('종료일 선택 시 콜백이 호출된다', (WidgetTester tester) async {
      DateTime? selectedEndDate;
      DateTime? callbackStartDate;
      DateTime? callbackEndDate;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: DatePickerWidget(
              initialStartDate: DateTime(2026, 1, 1),
              initialEndDate: DateTime(2026, 1, 15),
              onEndDateChanged: (date) {
                selectedEndDate = date;
              },
              onDateRangeChanged: (start, end) {
                callbackStartDate = start;
                callbackEndDate = end;
              },
            ),
          ),
        ),
      );

      // 종료일 버튼 탭
      final endDateButton = find.ancestor(
        of: find.text('종료일'),
        matching: find.byType(InkWell),
      ).last;

      await tester.tap(endDateButton);
      await tester.pumpAndSettle();

      // DatePicker에서 날짜 선택 (20일 선택)
      await tester.tap(find.text('20'));
      await tester.pumpAndSettle();

      // OK 버튼 클릭
      await tester.tap(find.text('OK'));
      await tester.pumpAndSettle();

      // 콜백이 호출되었는지 확인
      expect(selectedEndDate, isNotNull);
      expect(selectedEndDate!.day, 20);
      expect(callbackStartDate, isNotNull);
      expect(callbackEndDate, isNotNull);
    });

    testWidgets('날짜 범위 validation - 시작일은 종료일보다 이전이어야 함',
        (WidgetTester tester) async {
      final startDate = DateTime(2026, 1, 1);
      final endDate = DateTime(2026, 1, 31);

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: DatePickerWidget(
              initialStartDate: startDate,
              initialEndDate: endDate,
            ),
          ),
        ),
      );

      // 시작일 선택 다이얼로그 열기
      final startDateButton = find.ancestor(
        of: find.text('시작일'),
        matching: find.byType(InkWell),
      );

      await tester.tap(startDateButton);
      await tester.pumpAndSettle();

      // DatePicker의 lastDate가 종료일로 제한되는지 확인
      // (종료일 이후의 날짜는 비활성화되어 있어야 함)
      final datePickerDialog =
          tester.widget<DatePickerDialog>(find.byType(DatePickerDialog));
      expect(
        datePickerDialog.lastDate.isBefore(endDate.add(const Duration(days: 1))),
        true,
      );
    });

    testWidgets('날짜 범위 validation - 종료일은 시작일보다 이후여야 함',
        (WidgetTester tester) async {
      final startDate = DateTime(2026, 1, 1);
      final endDate = DateTime(2026, 1, 31);

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: DatePickerWidget(
              initialStartDate: startDate,
              initialEndDate: endDate,
            ),
          ),
        ),
      );

      // 종료일 선택 다이얼로그 열기
      final endDateButton = find.ancestor(
        of: find.text('종료일'),
        matching: find.byType(InkWell),
      ).last;

      await tester.tap(endDateButton);
      await tester.pumpAndSettle();

      // DatePicker의 firstDate가 시작일로 제한되는지 확인
      // (시작일 이전의 날짜는 비활성화되어 있어야 함)
      final datePickerDialog =
          tester.widget<DatePickerDialog>(find.byType(DatePickerDialog));
      expect(
        datePickerDialog.firstDate.isAfter(startDate.subtract(const Duration(days: 1))),
        true,
      );
    });

    testWidgets('minDate와 maxDate 제약이 올바르게 적용된다', (WidgetTester tester) async {
      final minDate = DateTime(2025, 1, 1);
      final maxDate = DateTime(2026, 12, 31);

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: DatePickerWidget(
              minDate: minDate,
              maxDate: maxDate,
            ),
          ),
        ),
      );

      // 시작일 다이얼로그 열기
      final startDateButton = find.ancestor(
        of: find.text('시작일'),
        matching: find.byType(InkWell),
      );

      await tester.tap(startDateButton);
      await tester.pumpAndSettle();

      // minDate 확인
      final datePickerDialog =
          tester.widget<DatePickerDialog>(find.byType(DatePickerDialog));
      expect(datePickerDialog.firstDate, minDate);
    });
  });
}
