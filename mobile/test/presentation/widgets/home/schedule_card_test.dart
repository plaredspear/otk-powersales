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
    String userRole = 'USER',
    VoidCallback? onRegisterTap,
    void Function(Schedule)? onScheduleTap,
    VoidCallback? onHeaderTap,
  }) {
    return MaterialApp(
      home: Scaffold(
        body: SingleChildScrollView(
          child: ScheduleCard(
            schedules: schedules,
            currentDate: currentDate,
            attendanceSummary: attendanceSummary ??
                const AttendanceSummary(totalCount: 0, registeredCount: 0),
            userRole: userRole,
            onRegisterTap: onRegisterTap,
            onScheduleTap: onScheduleTap,
            onHeaderTap: onHeaderTap,
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

      testWidgets('"내 일정" 링크가 표시되어야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget());

        expect(find.text('내 일정'), findsOneWidget);
        expect(find.byIcon(Icons.chevron_right), findsOneWidget);
      });

      testWidgets('"내 일정" 탭 시 onHeaderTap 콜백이 호출되어야 한다',
          (tester) async {
        var tapped = false;
        await tester.pumpWidget(buildTestWidget(
          onHeaderTap: () => tapped = true,
        ));

        await tester.tap(find.text('내 일정'));
        expect(tapped, isTrue);
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
      testWidgets('T1: totalCount == 0 이면 "등록" 버튼 비활성', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          attendanceSummary:
              const AttendanceSummary(totalCount: 0, registeredCount: 0),
          onRegisterTap: () {},
        ));

        final button = find.widgetWithText(ElevatedButton, '등록');
        expect(button, findsOneWidget);

        // 비활성이므로 onPressed가 null
        final elevatedButton =
            tester.widget<ElevatedButton>(find.byType(ElevatedButton));
        expect(elevatedButton.onPressed, isNull);
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

      testWidgets('T3: 부분 출근 시 "다음 등록" 버튼 활성', (tester) async {
        var tapped = false;
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(8, registered: 3),
          attendanceSummary:
              const AttendanceSummary(totalCount: 8, registeredCount: 3),
          onRegisterTap: () => tapped = true,
        ));

        final button = find.widgetWithText(ElevatedButton, '다음 등록');
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

    group('조장(LEADER) 뷰', () {
      testWidgets('헤더에 "일정 관리" 텍스트가 표시되어야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          attendanceSummary:
              const AttendanceSummary(totalCount: 5, registeredCount: 3),
        ));

        expect(find.text('일정 관리'), findsOneWidget);
        expect(find.text('내 일정'), findsNothing);
      });

      testWidgets('"팀 출근 현황: N명 중 M명 등록 완료" 텍스트가 표시되어야 한다',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          attendanceSummary:
              const AttendanceSummary(totalCount: 5, registeredCount: 3),
        ));

        expect(find.text('팀 출근 현황: 5명 중 3명 등록 완료'), findsOneWidget);
      });

      testWidgets('스케줄 없음 시 "오늘 등록된 팀 스케줄이 없습니다." 표시',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          attendanceSummary:
              const AttendanceSummary(totalCount: 0, registeredCount: 0),
        ));

        expect(find.text('오늘 등록된 팀 스케줄이 없습니다.'), findsOneWidget);
      });

      testWidgets('팀원 이름이 가나다순으로 정렬되어 표시되어야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          schedules: _makeLeaderSchedules(),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 1),
        ));

        // 가나다순: 김영미 → 박수진 → 이지현
        expect(find.text('김영미'), findsOneWidget);
        expect(find.text('박수진'), findsOneWidget);
        expect(find.text('이지현'), findsOneWidget);
      });

      testWidgets('출근 등록 버튼이 표시되지 않아야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          schedules: _makeLeaderSchedules(),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 1),
          onRegisterTap: () {},
        ));

        expect(find.byType(ElevatedButton), findsNothing);
      });

      testWidgets('출근 완료 시 체크 아이콘과 시간이 표시되어야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          schedules: [
            Schedule(
              scheduleId: 'SCH-1',
              employeeName: '김영미',
              employeeSfid: 'EMP-001',
              storeName: '이마트 강남점',
              workCategory: '행사',
              isCommuteRegistered: true,
              commuteRegisteredAt: DateTime(2026, 3, 3, 9, 30),
            ),
          ],
          attendanceSummary:
              const AttendanceSummary(totalCount: 1, registeredCount: 1),
        ));

        expect(find.text('09:30'), findsOneWidget);
        expect(find.byIcon(Icons.check), findsOneWidget);
      });

      testWidgets('미등록 시 "미등록" 텍스트가 표시되어야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          schedules: [
            const Schedule(
              scheduleId: 'SCH-1',
              employeeName: '박수진',
              employeeSfid: 'EMP-002',
              storeName: '홈플러스 잠실점',
              workCategory: '진열',
              isCommuteRegistered: false,
            ),
          ],
          attendanceSummary:
              const AttendanceSummary(totalCount: 1, registeredCount: 0),
        ));

        expect(find.text('미등록'), findsOneWidget);
      });

      testWidgets('출근 카운트 배지가 숨겨져야 한다 (팀 출근 현황 텍스트로 대체)',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          schedules: _makeLeaderSchedules(),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 1),
        ));

        // 기존 "X/N" 배지가 표시되지 않아야 함
        expect(find.text('1/3'), findsNothing);
      });

      testWidgets('"일정 관리" 탭 시 onHeaderTap 콜백이 호출되어야 한다',
          (tester) async {
        var tapped = false;
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          onHeaderTap: () => tapped = true,
        ));

        await tester.tap(find.text('일정 관리'));
        expect(tapped, isTrue);
      });

      testWidgets('동일 팀원의 복수 스케줄이 독립 표시되어야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          schedules: const [
            Schedule(
              scheduleId: 'SCH-1',
              employeeName: '김영미',
              employeeSfid: 'EMP-001',
              storeName: '이마트 강남점',
              workCategory: '행사',
              isCommuteRegistered: true,
              commuteRegisteredAt: null,
            ),
            Schedule(
              scheduleId: 'SCH-2',
              employeeName: '김영미',
              employeeSfid: 'EMP-001',
              storeName: '롯데마트 서초점',
              workCategory: '진열',
              isCommuteRegistered: false,
            ),
          ],
          attendanceSummary:
              const AttendanceSummary(totalCount: 2, registeredCount: 1),
        ));

        // 김영미가 2번 표시
        expect(find.text('김영미'), findsNWidgets(2));
        expect(find.text('이마트 강남점'), findsOneWidget);
        expect(find.text('롯데마트 서초점'), findsOneWidget);
      });
    });

    group('지점장(ADMIN) 뷰', () {
      testWidgets('ADMIN도 조장과 동일한 UI를 표시해야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'ADMIN',
          schedules: _makeLeaderSchedules(),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 1),
        ));

        expect(find.text('일정 관리'), findsOneWidget);
        expect(find.text('팀 출근 현황: 3명 중 1명 등록 완료'), findsOneWidget);
        expect(find.text('내 일정'), findsNothing);
        expect(find.byType(ElevatedButton), findsNothing);
      });
    });

    group('일반 사원(USER) 뷰 (기존 동작 확인)', () {
      testWidgets('USER는 "내 일정" 링크가 표시되어야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'USER',
        ));

        expect(find.text('내 일정'), findsOneWidget);
        expect(find.text('일정 관리'), findsNothing);
      });

      testWidgets('USER는 등록 버튼이 표시되어야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'USER',
          schedules: _makeSchedules(3, registered: 1),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 1),
          onRegisterTap: () {},
        ));

        expect(find.byType(ElevatedButton), findsOneWidget);
      });

      testWidgets('USER는 팀 출근 현황 텍스트가 표시되지 않아야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'USER',
          attendanceSummary:
              const AttendanceSummary(totalCount: 5, registeredCount: 3),
        ));

        expect(find.textContaining('팀 출근 현황'), findsNothing);
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

      testWidgets('T8: attendanceSummary 기본값(0/0)이면 휴무 상태 표시',
          (tester) async {
        await tester.pumpWidget(buildTestWidget());

        expect(find.text('오늘 등록된 스케줄이 없습니다.'), findsOneWidget);
        expect(find.text('0/0'), findsNothing); // 배지 숨김
      });
    });
  });
}

/// 테스트용 Schedule 목록 생성 (일반 사원)
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

/// 테스트용 Schedule 목록 생성 (조장 뷰 — 팀원별 다른 이름)
List<Schedule> _makeLeaderSchedules() {
  return [
    Schedule(
      scheduleId: 'SCH-1',
      employeeName: '김영미',
      employeeSfid: 'EMP-001',
      storeName: '이마트 강남점',
      workCategory: '행사',
      isCommuteRegistered: true,
      commuteRegisteredAt: DateTime(2026, 3, 3, 9, 30),
    ),
    const Schedule(
      scheduleId: 'SCH-2',
      employeeName: '박수진',
      employeeSfid: 'EMP-002',
      storeName: '홈플러스 잠실점',
      workCategory: '진열',
      isCommuteRegistered: false,
    ),
    Schedule(
      scheduleId: 'SCH-3',
      employeeName: '이지현',
      employeeSfid: 'EMP-003',
      storeName: '롯데마트 서초점',
      workCategory: '행사',
      isCommuteRegistered: false,
      commuteRegisteredAt: null,
    ),
  ];
}
