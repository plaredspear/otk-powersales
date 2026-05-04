import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_summary.dart';
import 'package:mobile/domain/entities/schedule.dart';
import 'package:mobile/presentation/widgets/home/schedule_card.dart';

/// ScheduleCard 골든 테스트
///
/// 베이스라인 생성: `flutter test --update-goldens test/presentation/widgets/home/schedule_card_golden_test.dart`
void main() {
  Widget wrap(Widget child) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        backgroundColor: const Color(0xFFF7F7F7),
        body: SizedBox(
          width: 393, // iPhone 14 width
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: child,
          ),
        ),
      ),
    );
  }

  ScheduleCard buildCard({
    required String userRole,
    required int totalCount,
    required int registeredCount,
    List<Schedule> schedules = const [],
  }) {
    return ScheduleCard(
      schedules: schedules,
      currentDate: '2026-05-04',
      attendanceSummary: AttendanceSummary(
        totalCount: totalCount,
        registeredCount: registeredCount,
      ),
      userRole: userRole,
    );
  }

  testWidgets('user_empty (일정 0건)', (tester) async {
    await tester.pumpWidget(wrap(buildCard(
      userRole: 'USER',
      totalCount: 0,
      registeredCount: 0,
    )));
    await expectLater(
      find.byType(ScheduleCard),
      matchesGoldenFile('../../../goldens/home/schedule_card_user_empty.png'),
    );
  });

  testWidgets('user_unregistered (0/5)', (tester) async {
    await tester.pumpWidget(wrap(buildCard(
      userRole: 'USER',
      totalCount: 5,
      registeredCount: 0,
      schedules: _makeSchedules(5, registered: 0),
    )));
    await expectLater(
      find.byType(ScheduleCard),
      matchesGoldenFile(
          '../../../goldens/home/schedule_card_user_unregistered.png'),
    );
  });

  testWidgets('user_partial (2/5)', (tester) async {
    await tester.pumpWidget(wrap(buildCard(
      userRole: 'USER',
      totalCount: 5,
      registeredCount: 2,
      schedules: _makeSchedules(5, registered: 2),
    )));
    await expectLater(
      find.byType(ScheduleCard),
      matchesGoldenFile('../../../goldens/home/schedule_card_user_partial.png'),
    );
  });

  testWidgets('user_complete (5/5)', (tester) async {
    await tester.pumpWidget(wrap(buildCard(
      userRole: 'USER',
      totalCount: 5,
      registeredCount: 5,
      schedules: _makeSchedules(5, registered: 5),
    )));
    await expectLater(
      find.byType(ScheduleCard),
      matchesGoldenFile(
          '../../../goldens/home/schedule_card_user_complete.png'),
    );
  });

  testWidgets('leaderView', (tester) async {
    await tester.pumpWidget(wrap(buildCard(
      userRole: 'LEADER',
      totalCount: 3,
      registeredCount: 1,
      schedules: _makeLeaderSchedules(),
    )));
    await expectLater(
      find.byType(ScheduleCard),
      matchesGoldenFile('../../../goldens/home/schedule_card_leader_view.png'),
    );
  });
}

List<Schedule> _makeSchedules(int count, {required int registered}) {
  return List.generate(count, (i) {
    return Schedule(
      scheduleId: i + 1,
      employeeName: '테스트',
      employeeCode: 'EMP-001',
      accountName: '매장 ${i + 1}',
      workCategory: '행사',
      isCommuteRegistered: i < registered,
      commuteRegisteredAt:
          i < registered ? DateTime(2026, 5, 4, 9, 0) : null,
    );
  });
}

List<Schedule> _makeLeaderSchedules() {
  return [
    Schedule(
      scheduleId: 1,
      employeeName: '김영미',
      employeeCode: 'EMP-001',
      accountName: '이마트 강남점',
      workCategory: '행사',
      isCommuteRegistered: true,
      commuteRegisteredAt: DateTime(2026, 5, 4, 9, 30),
    ),
    const Schedule(
      scheduleId: 2,
      employeeName: '박수진',
      employeeCode: 'EMP-002',
      accountName: '홈플러스 잠실점',
      workCategory: '진열',
      isCommuteRegistered: false,
    ),
    const Schedule(
      scheduleId: 3,
      employeeName: '이지현',
      employeeCode: 'EMP-003',
      accountName: '롯데마트 서초점',
      workCategory: '행사',
      isCommuteRegistered: false,
    ),
  ];
}
