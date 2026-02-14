import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/monthly_schedule_day.dart';
import 'package:mobile/presentation/widgets/my_schedule/calendar_widget.dart';

void main() {
  group('CalendarWidget', () {
    testWidgets('요일 헤더를 렌더링한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CalendarWidget(
              year: 2026,
              month: 2,
              workDays: const [],
              onDateTap: (_) {},
            ),
          ),
        ),
      );

      expect(find.text('일'), findsOneWidget);
      expect(find.text('월'), findsOneWidget);
      expect(find.text('화'), findsOneWidget);
      expect(find.text('수'), findsOneWidget);
      expect(find.text('목'), findsOneWidget);
      expect(find.text('금'), findsOneWidget);
      expect(find.text('토'), findsOneWidget);
    });

    testWidgets('날짜 셀을 렌더링한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CalendarWidget(
              year: 2026,
              month: 2,
              workDays: const [],
              onDateTap: (_) {},
            ),
          ),
        ),
      );

      // 2월 1일부터 28일까지 존재 확인
      expect(find.text('1'), findsOneWidget);
      expect(find.text('15'), findsOneWidget);
      expect(find.text('28'), findsOneWidget);
    });

    testWidgets('근무일에 "근무" 마커를 표시한다', (tester) async {
      final workDays = [
        MonthlyScheduleDay(
          date: DateTime(2026, 2, 3),
          hasWork: true,
        ),
        MonthlyScheduleDay(
          date: DateTime(2026, 2, 5),
          hasWork: true,
        ),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CalendarWidget(
              year: 2026,
              month: 2,
              workDays: workDays,
              onDateTap: (_) {},
            ),
          ),
        ),
      );

      // "근무" 텍스트가 근무일 수만큼 표시되어야 함
      expect(find.text('근무'), findsNWidgets(2));
    });

    testWidgets('hasWork=false인 날짜에는 "근무" 마커를 표시하지 않는다', (tester) async {
      final workDays = [
        MonthlyScheduleDay(
          date: DateTime(2026, 2, 3),
          hasWork: false,
        ),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CalendarWidget(
              year: 2026,
              month: 2,
              workDays: workDays,
              onDateTap: (_) {},
            ),
          ),
        ),
      );

      expect(find.text('근무'), findsNothing);
    });

    testWidgets('근무일을 탭하면 onDateTap 콜백이 호출된다', (tester) async {
      DateTime? tappedDate;
      final workDays = [
        MonthlyScheduleDay(
          date: DateTime(2026, 2, 3),
          hasWork: true,
        ),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CalendarWidget(
              year: 2026,
              month: 2,
              workDays: workDays,
              onDateTap: (date) {
                tappedDate = date;
              },
            ),
          ),
        ),
      );

      // 3일 날짜 셀을 탭
      await tester.tap(find.text('3'));
      await tester.pumpAndSettle();

      expect(tappedDate, isNotNull);
      expect(tappedDate!.year, 2026);
      expect(tappedDate!.month, 2);
      expect(tappedDate!.day, 3);
    });

    testWidgets('근무가 없는 날짜를 탭하면 콜백이 호출되지 않는다', (tester) async {
      DateTime? tappedDate;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CalendarWidget(
              year: 2026,
              month: 2,
              workDays: const [], // 근무일 없음
              onDateTap: (date) {
                tappedDate = date;
              },
            ),
          ),
        ),
      );

      // 5일 날짜 셀을 탭 (근무일 아님)
      await tester.tap(find.text('5'));
      await tester.pumpAndSettle();

      expect(tappedDate, isNull);
    });
  });
}
