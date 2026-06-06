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

    Future<void> verifyIcons(WidgetTester tester, {
      required String password,
      required IconData lengthIcon,
      required IconData repeatIcon,
    }) async {
      await tester.pumpWidget(buildHarness(password));
      final icons = tester.widgetList<Icon>(find.byType(Icon)).toList();
      expect(icons.length, 2);
      expect(icons[0].icon, lengthIcon);
      expect(icons[1].icon, repeatIcon);
    }

    testWidgets('빈 입력 -> 모든 규칙 회색 (대기 상태)', (tester) async {
      await verifyIcons(
        tester,
        password: '',
        lengthIcon: Icons.circle_outlined,
        repeatIcon: Icons.circle_outlined,
      );
    });

    testWidgets('"abcd" -> 모든 규칙 충족', (tester) async {
      await verifyIcons(
        tester,
        password: 'abcd',
        lengthIcon: Icons.check_circle,
        repeatIcon: Icons.check_circle,
      );
    });

    testWidgets('"abc" -> 길이 위반', (tester) async {
      await verifyIcons(
        tester,
        password: 'abc',
        lengthIcon: Icons.cancel,
        repeatIcon: Icons.check_circle,
      );
    });

    testWidgets('"aaaa" -> 반복 위반', (tester) async {
      await verifyIcons(
        tester,
        password: 'aaaa',
        lengthIcon: Icons.check_circle,
        repeatIcon: Icons.cancel,
      );
    });

    testWidgets('"가가가가" -> 한글 4연속 위반', (tester) async {
      await verifyIcons(
        tester,
        password: '가가가가',
        lengthIcon: Icons.check_circle,
        repeatIcon: Icons.cancel,
      );
    });
  });
}
