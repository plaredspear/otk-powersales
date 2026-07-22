import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/auth/password_policy_checklist.dart';

void main() {
  group('PasswordPolicyChecklist (Spec #584)', () {
    Widget buildHarness(String password) {
      return MaterialApp(
        home: Scaffold(
          body: PasswordPolicyChecklist(password: password),
        ),
      );
    }

    // 위젯은 규칙 2개를 순서대로 렌더한다: [0]=8자 이상, [1]=3종 이상 조합.
    Future<void> verifyIcons(WidgetTester tester, {
      required String password,
      required IconData lengthIcon,
      required IconData typesIcon,
    }) async {
      await tester.pumpWidget(buildHarness(password));
      final icons = tester.widgetList<Icon>(find.byType(Icon)).toList();
      expect(icons.length, 2);
      expect(icons[0].icon, lengthIcon);
      expect(icons[1].icon, typesIcon);
    }

    testWidgets('빈 입력 -> 모든 규칙 회색 (대기 상태)', (tester) async {
      await verifyIcons(
        tester,
        password: '',
        lengthIcon: Icons.circle_outlined,
        typesIcon: Icons.circle_outlined,
      );
    });

    testWidgets('"Abcd1234" -> 모든 규칙 충족 (8자+3종)', (tester) async {
      // 길이 8, 종류 3종(대문자 A / 소문자 bcd / 숫자 1234).
      await verifyIcons(
        tester,
        password: 'Abcd1234',
        lengthIcon: Icons.check_circle,
        typesIcon: Icons.check_circle,
      );
    });

    testWidgets('"Ab1!" -> 길이 위반 (조합 충족)', (tester) async {
      // 길이 4(<8) 위반, 종류 4종(대/소/숫자/특수) 충족.
      await verifyIcons(
        tester,
        password: 'Ab1!',
        lengthIcon: Icons.cancel,
        typesIcon: Icons.check_circle,
      );
    });

    testWidgets('"abcdefgh" -> 조합 위반 (길이 충족)', (tester) async {
      // 길이 8 충족, 종류 1종(소문자만) → 3종 미달 위반.
      await verifyIcons(
        tester,
        password: 'abcdefgh',
        lengthIcon: Icons.check_circle,
        typesIcon: Icons.cancel,
      );
    });

    testWidgets('"가가가가" -> 길이·조합 모두 위반 (한글은 문자종류 0)', (tester) async {
      // 길이 4(<8) 위반, 한글은 대/소/숫자/특수 어디에도 안 들어가 0종 → 조합 위반.
      await verifyIcons(
        tester,
        password: '가가가가',
        lengthIcon: Icons.cancel,
        typesIcon: Icons.cancel,
      );
    });
  });
}
