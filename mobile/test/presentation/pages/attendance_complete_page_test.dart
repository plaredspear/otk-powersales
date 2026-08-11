import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_result.dart';
import 'package:mobile/presentation/pages/attendance_complete_page.dart';

/// 출근등록 완료 화면 — 근무유형4 미표시
///
/// 레거시(home.jsp `#popPlace3`)는 근무유형4(상온/라면/만두/냉동/냉장)를 출근현황 팝업의
/// 근태 셀에만 "완료(냉동)" 형태로 노출했고, 등록 완료 화면에는 표시하지 않았다.
/// 완료 화면에 값이 다시 새어 나오지 않도록 가드한다.
void main() {
  AttendanceResult buildResult({String? secondWorkType}) => AttendanceResult(
    scheduleId: 1,
    accountName: '(주)이마트 만촌점',
    workType: '근무',
    secondWorkType: secondWorkType,
    distanceKm: 0.0,
    totalCount: 1,
    registeredCount: 1,
  );

  Future<void> pumpPage(WidgetTester tester, AttendanceResult result) async {
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(home: AttendanceCompletePage(result: result)),
      ),
    );
    await tester.pump();
  }

  group('AttendanceCompletePage — 근무유형4 미표시', () {
    testWidgets('서버가 근무유형4 를 내려줘도 화면에 표시하지 않는다', (tester) async {
      await pumpPage(tester, buildResult(secondWorkType: '라면'));

      expect(find.text('라면'), findsNothing);
      // 나머지 완료 정보는 정상 노출.
      expect(find.text('출근등록 완료'), findsOneWidget);
      expect(find.text('(주)이마트 만촌점'), findsOneWidget);
    });

    testWidgets('제품 파생 유형(만두/냉동)도 표시하지 않는다', (tester) async {
      await pumpPage(tester, buildResult(secondWorkType: '만두'));

      expect(find.text('만두'), findsNothing);
      expect(find.text('냉동'), findsNothing);
      expect(find.text('출근등록 완료'), findsOneWidget);
    });

    testWidgets('근무유형4 가 null 이어도 완료 정보는 정상 노출한다', (tester) async {
      await pumpPage(tester, buildResult());

      expect(find.text('출근등록 완료'), findsOneWidget);
      expect(find.text('(주)이마트 만촌점'), findsOneWidget);
      expect(find.text('1 / 1 거래처 등록 완료'), findsOneWidget);
    });
  });
}
