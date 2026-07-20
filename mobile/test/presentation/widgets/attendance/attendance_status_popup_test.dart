import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_status.dart';
import 'package:mobile/presentation/widgets/attendance/attendance_status_popup.dart';

/// 오늘의 등록 현황 팝업 (레거시 home.jsp `#popPlace3`) 위젯 테스트
void main() {
  Widget buildTestWidget({
    required List<AttendanceStatus> statusList,
    int totalCount = 0,
    int registeredCount = 0,
    String? currentDate,
  }) {
    return MaterialApp(
      home: Scaffold(
        body: AttendanceStatusPopup(
          statusList: statusList,
          totalCount: totalCount,
          registeredCount: registeredCount,
          currentDate: currentDate,
        ),
      ),
    );
  }

  const completedEvent = AttendanceStatus(
    scheduleId: 1,
    accountName: 'G마트A',
    workCategory: '행사',
    status: 'REGISTERED',
    secondWorkType: '상온',
  );

  const completedDisplay = AttendanceStatus(
    scheduleId: 2,
    accountName: 'MH 커머스',
    workCategory: '진열',
    status: 'REGISTERED',
  );

  const pending = AttendanceStatus(
    scheduleId: 3,
    accountName: 'PC캠프',
    workCategory: '진열',
    status: 'PENDING',
  );

  group('AttendanceStatusPopup', () {
    testWidgets('기준일이 있으면 "yyyy년 MM월 dd일 (요일)" 제목을 표시한다', (tester) async {
      await tester.pumpWidget(buildTestWidget(
        statusList: const [pending],
        totalCount: 1,
        currentDate: '2020-08-20',
      ));

      expect(find.text('2020년 08월 20일 (목)'), findsOneWidget);
    });

    testWidgets('기준일이 없으면 "출근등록 현황" 제목을 표시한다', (tester) async {
      await tester.pumpWidget(buildTestWidget(
        statusList: const [pending],
        totalCount: 1,
      ));

      expect(find.text('출근등록 현황'), findsOneWidget);
    });

    testWidgets('완료/대기 근태와 등록 수 배지를 표시한다', (tester) async {
      await tester.pumpWidget(buildTestWidget(
        statusList: const [completedEvent, completedDisplay, pending],
        totalCount: 3,
        registeredCount: 2,
      ));

      expect(find.text('1 / 3'), findsNothing);
      expect(find.text('2 / 3'), findsOneWidget);
      expect(find.text('G마트A'), findsOneWidget);
      expect(find.text('완료'), findsNWidgets(2));
      expect(find.text('대기'), findsOneWidget);
    });

    testWidgets('근무유형4가 있는 완료 행에만 "(상온)" 을 덧붙인다', (tester) async {
      await tester.pumpWidget(buildTestWidget(
        statusList: const [completedEvent, completedDisplay],
        totalCount: 2,
        registeredCount: 2,
      ));

      // 행사(근무유형4 = 상온)만 괄호 줄 노출, 진열(null)은 없음
      expect(find.text('(상온)'), findsOneWidget);
    });

    testWidgets('목록이 비어도 안내 문구를 노출한다', (tester) async {
      await tester.pumpWidget(buildTestWidget(statusList: const []));

      expect(find.text('오늘 등록된 근무지가 없습니다.'), findsOneWidget);
    });
  });
}
