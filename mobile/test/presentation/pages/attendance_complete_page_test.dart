import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_result.dart';
import 'package:mobile/presentation/pages/attendance_complete_page.dart';

/// 출근등록 완료 화면 — 근무유형4 뱃지
///
/// 레거시(SF)는 온도 구분을 행사마스터 제품유형 / 진열마스터 근무형태4에서 자동 파생해
/// `TeamMemberSchedule.SecondWorkType__c` 에 담는다. 값 도메인은 상온/라면/만두/냉동/냉장 이며
/// 앱이 판정하지 않고 서버 문자열을 그대로 출력해야 한다.
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

  group('AttendanceCompletePage — 근무유형4 뱃지', () {
    testWidgets('서버가 준 근무유형4 문자열을 그대로 표시한다', (tester) async {
      await pumpPage(tester, buildResult(secondWorkType: '라면'));

      expect(find.text('라면'), findsOneWidget);
      // 앱에서 '냉장/냉동' 을 임의로 만들어내지 않는다.
      expect(find.text('냉장/냉동'), findsNothing);
    });

    testWidgets('근무유형4 가 null 이면 뱃지를 표시하지 않는다', (tester) async {
      await pumpPage(tester, buildResult());

      expect(find.text('냉장/냉동'), findsNothing);
      expect(find.text('상온'), findsNothing);
      // 나머지 완료 정보는 정상 노출.
      expect(find.text('출근등록 완료'), findsOneWidget);
      expect(find.text('(주)이마트 만촌점'), findsOneWidget);
    });

    testWidgets('근무유형4 가 빈 문자열이면 뱃지를 표시하지 않는다', (tester) async {
      await pumpPage(tester, buildResult(secondWorkType: ''));

      expect(find.text(''), findsNothing);
      expect(find.text('출근등록 완료'), findsOneWidget);
    });
  });
}
