import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_summary.dart';
import 'package:mobile/domain/entities/schedule.dart';
import 'package:mobile/presentation/widgets/home/schedule_card.dart';

void main() {
  Widget buildTestWidget({
    List<Schedule> schedules = const [],
    String currentDate = '2026-03-01',
    AttendanceSummary? attendanceSummary,
    VoidCallback? onRegisterTap,
    void Function(Schedule)? onScheduleTap,
  }) {
    return MaterialApp(
      home: Scaffold(
        body: SingleChildScrollView(
          child: ScheduleCard(
            schedules: schedules,
            currentDate: currentDate,
            attendanceSummary: attendanceSummary ??
                const AttendanceSummary(totalCount: 0, registeredCount: 0),
            onRegisterTap: onRegisterTap,
            onScheduleTap: onScheduleTap,
          ),
        ),
      ),
    );
  }

  group('ScheduleCard', () {
    group('날짜 헤더', () {
      testWidgets('날짜를 MM월 dd일 (요일) 형식으로 표시해야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(currentDate: '2026-03-01'));

        expect(find.text('03월 01일 (일)'), findsOneWidget);
      });
    });

    group('출근 카운트 배지', () {
      testWidgets('T1: totalCount == 0 이면 배지를 숨겨야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          attendanceSummary:
              const AttendanceSummary(totalCount: 0, registeredCount: 0),
        ));

        // 배지 텍스트가 없어야 함
        expect(find.text('0/0'), findsNothing);
      });

      testWidgets('T2: totalCount > 0, registeredCount == 0 이면 "0/N" 배지 표시',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(8, registered: 0),
          attendanceSummary:
              const AttendanceSummary(totalCount: 8, registeredCount: 0),
        ));

        expect(find.text('0/8'), findsOneWidget);
        expect(find.byIcon(Icons.check), findsOneWidget);
      });

      testWidgets('T3: 부분 출근 시 "X/N" 배지 표시', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(8, registered: 3),
          attendanceSummary:
              const AttendanceSummary(totalCount: 8, registeredCount: 3),
        ));

        expect(find.text('3/8'), findsOneWidget);
      });

      testWidgets('T4: 전원 출근 시 "N/N" 배지 표시', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(8, registered: 8),
          attendanceSummary:
              const AttendanceSummary(totalCount: 8, registeredCount: 8),
        ));

        expect(find.text('8/8'), findsOneWidget);
      });
    });

    group('본문 영역', () {
      testWidgets('T1: totalCount == 0 이면 "오늘 등록된 스케줄이 없습니다." 표시',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          attendanceSummary:
              const AttendanceSummary(totalCount: 0, registeredCount: 0),
        ));

        expect(find.text('오늘 등록된 스케줄이 없습니다.'), findsOneWidget);
      });

      testWidgets('T2: totalCount > 0, registeredCount == 0 이면 미출근 안내 표시',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(8, registered: 0),
          attendanceSummary:
              const AttendanceSummary(totalCount: 8, registeredCount: 0),
        ));

        expect(find.text('출근 후 등록을 누르세요.'), findsOneWidget);
      });

      testWidgets('T3: 부분 출근 시 스케줄 목록 표시', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(3, registered: 1),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 1),
        ));

        // 스케줄 아이템이 표시되어야 함
        expect(find.text('매장 0'), findsOneWidget);
        expect(find.text('매장 1'), findsOneWidget);
        expect(find.text('매장 2'), findsOneWidget);
      });
    });

    group('등록 버튼', () {
      testWidgets('T1: totalCount == 0 이면 등록 버튼 숨김', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          attendanceSummary:
              const AttendanceSummary(totalCount: 0, registeredCount: 0),
        ));

        // "등록" 또는 "등록 완료" 버튼이 없어야 함
        final registerButtons = find.widgetWithText(ElevatedButton, '등록');
        final completeButtons = find.widgetWithText(ElevatedButton, '등록 완료');
        expect(registerButtons, findsNothing);
        expect(completeButtons, findsNothing);
      });

      testWidgets('T2: registeredCount < totalCount 이면 "등록" 버튼 활성',
          (tester) async {
        var tapped = false;
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(8, registered: 0),
          attendanceSummary:
              const AttendanceSummary(totalCount: 8, registeredCount: 0),
          onRegisterTap: () => tapped = true,
        ));

        final button = find.widgetWithText(ElevatedButton, '등록');
        expect(button, findsOneWidget);
        await tester.tap(button);
        expect(tapped, isTrue);
      });

      testWidgets('T4: registeredCount == totalCount 이면 "등록 완료" 비활성',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(8, registered: 8),
          attendanceSummary:
              const AttendanceSummary(totalCount: 8, registeredCount: 8),
          onRegisterTap: () {},
        ));

        final button = find.widgetWithText(ElevatedButton, '등록 완료');
        expect(button, findsOneWidget);

        // 비활성이므로 ElevatedButton의 onPressed가 null
        final elevatedButton =
            tester.widget<ElevatedButton>(find.byType(ElevatedButton));
        expect(elevatedButton.onPressed, isNull);
      });
    });

    group('경계 케이스', () {
      testWidgets('T5: totalCount 1, registeredCount 0', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(1, registered: 0),
          attendanceSummary:
              const AttendanceSummary(totalCount: 1, registeredCount: 0),
        ));

        expect(find.text('0/1'), findsOneWidget);
        expect(find.text('출근 후 등록을 누르세요.'), findsOneWidget);
      });

      testWidgets('T6: totalCount 1, registeredCount 1', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(1, registered: 1),
          attendanceSummary:
              const AttendanceSummary(totalCount: 1, registeredCount: 1),
        ));

        expect(find.text('1/1'), findsOneWidget);
        expect(find.widgetWithText(ElevatedButton, '등록 완료'), findsOneWidget);
      });
    });
  });
}

/// 테스트용 Schedule 목록 생성
List<Schedule> _makeSchedules(int count, {required int registered}) {
  return List.generate(count, (i) {
    return Schedule(
      scheduleId: 'SCH-$i',
      employeeName: '테스트',
      employeeSfid: 'EMP-001',
      storeName: '매장 $i',
      workCategory: '행사',
      isCommuteRegistered: i < registered,
    );
  });
}
