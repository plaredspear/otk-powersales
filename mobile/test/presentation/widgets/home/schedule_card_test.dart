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
    VoidCallback? onScheduleManageTap,
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
            onScheduleManageTap: onScheduleManageTap,
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

      testWidgets('"내 일정" 링크가 표시되지 않아야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget());

        expect(find.text('내 일정'), findsNothing);
        expect(find.text('일정 관리'), findsNothing);
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
        // 레거시 정렬: 배지 내 체크 아이콘 제거 (텍스트만 표시)
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

      testWidgets(
          'T2: totalCount > 0, registeredCount == 0 이어도 일정(거래처명+근무정보)을 표시',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(8, registered: 0),
          attendanceSummary:
              const AttendanceSummary(totalCount: 8, registeredCount: 0),
        ));

        // 레거시 정합: 출근 전에도 "거래처명 (근무구분)"이 노출된다.
        expect(find.text('출근 후 등록을 누르세요.'), findsNothing);
        expect(find.text('매장 0 (행사/-/-)'), findsOneWidget);
        expect(find.text('매장 7 (행사/-/-)'), findsOneWidget);
      });

      testWidgets('T3: 부분 출근 시 스케줄 목록 표시', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(3, registered: 1),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 1),
        ));

        // 스케줄 아이템이 "거래처명 (근무구분)" 형식으로 표시되어야 함
        expect(find.text('매장 0 (행사/-/-)'), findsOneWidget);
        expect(find.text('매장 1 (행사/-/-)'), findsOneWidget);
        expect(find.text('매장 2 (행사/-/-)'), findsOneWidget);
      });

      testWidgets(
          'T4: wc1/wc2/wc3 모두 있으면 "거래처명 (근무구분/상시임시/근무형태)" 형식으로 표시',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: const [
            Schedule(
              scheduleId: 1,
              employeeName: '테스트',
              employeeCode: 'EMP-001',
              accountName: '가락 알파마트(주)',
              workCategory: '진열',
              workCategory2: '상시',
              workType: '고정',
              isCommuteRegistered: false,
            ),
          ],
          attendanceSummary:
              const AttendanceSummary(totalCount: 1, registeredCount: 0),
        ));

        // 레거시 home.jsp:549 `(wc1/wc2/wc3)` 정합
        expect(find.text('가락 알파마트(주) (진열/상시/고정)'), findsOneWidget);
      });

      testWidgets('T5: 빈 토큰(wc2 null)은 "-" 로 채워 세 슬롯을 항상 표시',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: const [
            Schedule(
              scheduleId: 1,
              employeeName: '테스트',
              employeeCode: 'EMP-001',
              accountName: '가락 알파마트(주)',
              workCategory: '진열',
              workType: '고정',
              isCommuteRegistered: false,
            ),
          ],
          attendanceSummary:
              const AttendanceSummary(totalCount: 1, registeredCount: 0),
        ));

        expect(find.text('가락 알파마트(주) (진열/-/고정)'), findsOneWidget);
      });

      testWidgets('T6: 세 토큰이 모두 비면 "(-/-/-)" 로 표시', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: const [
            Schedule(
              scheduleId: 1,
              employeeName: '테스트',
              employeeCode: 'EMP-001',
              accountName: '가락 알파마트(주)',
              workCategory: '',
              isCommuteRegistered: false,
            ),
          ],
          attendanceSummary:
              const AttendanceSummary(totalCount: 1, registeredCount: 0),
        ));

        expect(find.text('가락 알파마트(주) (-/-/-)'), findsOneWidget);
      });
    });

    group('근무형태 분기 (레거시 home.jsp 정합)', () {
      testWidgets('고정근무자는 출근 전(registeredCount==0)에도 전체 일정을 표시',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(3, registered: 0, workType: '고정'),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 0),
        ));

        expect(find.text('출근 후 등록을 누르세요.'), findsNothing);
        expect(find.text('매장 0 (행사/-/고정)'), findsOneWidget);
        expect(find.text('매장 1 (행사/-/고정)'), findsOneWidget);
        expect(find.text('매장 2 (행사/-/고정)'), findsOneWidget);
      });

      testWidgets('순회근무자는 출근 전에는 일정을 숨기고 안내 문구를 표시',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(3, registered: 0, workType: '순회'),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 0),
        ));

        expect(find.text('출근 후 등록을 누르세요.'), findsOneWidget);
        expect(find.text('매장 0 (행사/-/순회)'), findsNothing);
      });

      testWidgets('격고근무자도 출근 전에는 일정을 숨긴다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(2, registered: 0, workType: '격고'),
          attendanceSummary:
              const AttendanceSummary(totalCount: 2, registeredCount: 0),
        ));

        expect(find.text('출근 후 등록을 누르세요.'), findsOneWidget);
        expect(find.text('매장 0 (행사/-/격고)'), findsNothing);
      });

      testWidgets('순회근무자는 출근 후 첫 일정(list[0])만 표시한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(3, registered: 1, workType: '순회'),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 1),
        ));

        expect(find.text('출근 후 등록을 누르세요.'), findsNothing);
        expect(find.text('매장 0 (행사/-/순회)'), findsOneWidget);
        // 나머지 일정은 숨겨진다 (레거시 home.jsp:575)
        expect(find.text('매장 1 (행사/-/순회)'), findsNothing);
        expect(find.text('매장 2 (행사/-/순회)'), findsNothing);
      });
    });

    group('등록 버튼', () {
      testWidgets('T1: totalCount == 0 이면 "등록" 버튼 비활성', (tester) async {
        var tapped = false;
        await tester.pumpWidget(buildTestWidget(
          attendanceSummary:
              const AttendanceSummary(totalCount: 0, registeredCount: 0),
          onRegisterTap: () => tapped = true,
        ));

        final button = find.text('등록');
        expect(button, findsOneWidget);
        // 비활성이면 InkWell.onTap이 null이라 콜백이 호출되지 않는다
        await tester.tap(button);
        expect(tapped, isFalse);
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

        final button = find.text('등록');
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

        final button = find.text('다음 등록');
        expect(button, findsOneWidget);
        await tester.tap(button);
        expect(tapped, isTrue);
      });

      testWidgets('T4: registeredCount == totalCount 이면 "등록 완료" 비활성',
          (tester) async {
        var tapped = false;
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(8, registered: 8),
          attendanceSummary:
              const AttendanceSummary(totalCount: 8, registeredCount: 8),
          onRegisterTap: () => tapped = true,
        ));

        final button = find.text('등록 완료');
        expect(button, findsOneWidget);
        await tester.tap(button);
        expect(tapped, isFalse);
      });
    });

    group('조장(LEADER) 뷰', () {
      testWidgets('헤더에 "일정 관리" 버튼이 표시되어야 한다 (레거시 근태 영역 대응)',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          attendanceSummary:
              const AttendanceSummary(totalCount: 5, registeredCount: 3),
        ));

        expect(find.text('일정 관리'), findsOneWidget);
        expect(find.text('내 일정'), findsNothing);
      });

      testWidgets('"일정 관리" 버튼 탭 시 onScheduleManageTap 콜백이 호출되어야 한다',
          (tester) async {
        var tapped = false;
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          attendanceSummary:
              const AttendanceSummary(totalCount: 5, registeredCount: 3),
          onScheduleManageTap: () => tapped = true,
        ));

        await tester.tap(find.text('일정 관리'));
        expect(tapped, isTrue);
      });

      testWidgets('"N명 중, M명 등록 완료" 요약 텍스트가 표시되어야 한다',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          attendanceSummary:
              const AttendanceSummary(totalCount: 5, registeredCount: 3),
        ));

        expect(find.text('5명 중, 3명 등록 완료'), findsOneWidget);
      });

      testWidgets('스케줄 없음 시 "0명 중, 0명 등록 완료" + "일정 관리" 표시',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          attendanceSummary:
              const AttendanceSummary(totalCount: 0, registeredCount: 0),
        ));

        // 레거시 정합: 팀원 목록/빈 스케줄 메시지는 카드에 없고
        // 요약 텍스트 + '일정 관리' 진입 버튼만 노출된다.
        expect(find.text('0명 중, 0명 등록 완료'), findsOneWidget);
        expect(find.text('일정 관리'), findsOneWidget);
      });

      testWidgets('팀원 목록/스케줄 상세는 카드에 표시되지 않는다 ("일정 관리" 페이지로 이전)',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          schedules: _makeLeaderSchedules(),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 1),
        ));

        // 팀원 이름/매장/출근상태 등 상세는 '일정 관리' 페이지에서 확인한다.
        expect(find.text('김영미'), findsNothing);
        expect(find.text('박수진'), findsNothing);
        expect(find.text('이지현'), findsNothing);
        expect(find.text('미등록'), findsNothing);
      });

      testWidgets('출근 등록 버튼이 표시되지 않아야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'LEADER',
          schedules: _makeLeaderSchedules(),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 1),
          onRegisterTap: () {},
        ));

        // 등록 버튼 텍스트가 LEADER 뷰에서는 표시되지 않음
        expect(find.text('등록'), findsNothing);
        expect(find.text('다음 등록'), findsNothing);
        expect(find.text('등록 완료'), findsNothing);
      });

      testWidgets('출근 카운트 배지가 숨겨져야 한다 (요약 텍스트로 대체)',
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
    });

    group('지점장(ADMIN) 뷰', () {
      // 레거시 home.jsp:509 근태영역이 `eq '조장'` 정확 일치라, 지점장(ADMIN)은
      // 조장뷰가 아닌 본인(여사원형) 일정 뷰로 폴백한다 (commit 1e8679d8).
      testWidgets('ADMIN은 조장뷰가 아닌 본인 일정 뷰로 폴백한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'ADMIN',
          schedules: _makeLeaderSchedules(),
          attendanceSummary:
              const AttendanceSummary(totalCount: 3, registeredCount: 1),
        ));

        // 조장뷰 전용 요소("일정 관리" 버튼 + 팀 출근현황 요약)는 미표시
        expect(find.text('일정 관리'), findsNothing);
        expect(find.text('3명 중, 1명 등록 완료'), findsNothing);
        expect(find.text('내 일정'), findsNothing);
      });
    });

    group('일반 사원(USER) 뷰 (기존 동작 확인)', () {
      testWidgets('USER는 "내 일정" 링크가 표시되지 않아야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          userRole: 'USER',
        ));

        expect(find.text('내 일정'), findsNothing);
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

        expect(find.text('다음 등록'), findsOneWidget);
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
        // 출근 전에도 일정이 노출된다 (레거시 정합)
        expect(find.text('출근 후 등록을 누르세요.'), findsNothing);
        expect(find.text('매장 0 (행사/-/-)'), findsOneWidget);
      });

      testWidgets('T6: totalCount 1, registeredCount 1', (tester) async {
        await tester.pumpWidget(buildTestWidget(
          schedules: _makeSchedules(1, registered: 1),
          attendanceSummary:
              const AttendanceSummary(totalCount: 1, registeredCount: 1),
        ));

        expect(find.text('1/1'), findsOneWidget);
        expect(find.text('등록 완료'), findsOneWidget);
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
///
/// [workType] 이 null 이면 근무형태 미지정(고정으로 취급)이며 라벨은 "매장 N (행사)".
/// 값이 있으면 라벨은 "매장 N (행사/근무형태)".
List<Schedule> _makeSchedules(
  int count, {
  required int registered,
  String? workType,
}) {
  return List.generate(count, (i) {
    return Schedule(
      scheduleId: i + 1,
      employeeName: '테스트',
      employeeCode: 'EMP-001',
      accountName: '매장 $i',
      workCategory: '행사',
      workType: workType,
      isCommuteRegistered: i < registered,
    );
  });
}

/// 테스트용 Schedule 목록 생성 (조장 뷰 — 팀원별 다른 이름)
List<Schedule> _makeLeaderSchedules() {
  return [
    Schedule(
      scheduleId: 1,
      employeeName: '김영미',
      employeeCode: 'EMP-001',
      accountName: '이마트 강남점',
      workCategory: '행사',
      isCommuteRegistered: true,
      commuteRegisteredAt: DateTime(2026, 3, 3, 9, 30),
    ),
    const Schedule(
      scheduleId: 2,
      employeeName: '박수진',
      employeeCode: 'EMP-002',
      accountName: '홈플러스 잠실점',
      workCategory: '진열',
      isCommuteRegistered: false,
    ),
    Schedule(
      scheduleId: 3,
      employeeName: '이지현',
      employeeCode: 'EMP-003',
      accountName: '롯데마트 서초점',
      workCategory: '행사',
      isCommuteRegistered: false,
      commuteRegisteredAt: null,
    ),
  ];
}
