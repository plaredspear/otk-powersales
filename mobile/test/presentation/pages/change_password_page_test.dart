import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/pages/change_password_page.dart';

void main() {
  group('ChangePasswordPage', () {
    const currentPassword = 'oldpass123';

    testWidgets('페이지가 렌더링된다', (tester) async {
      // When
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ChangePasswordPage(currentPassword: currentPassword),
          ),
        ),
      );

      // Then
      expect(find.text('비밀번호 변경'), findsOneWidget);
      expect(find.text('변경할 비밀번호*'), findsOneWidget);
      expect(find.text('변경할 비밀번호 다시 입력*'), findsOneWidget);
      expect(find.text('변경'), findsOneWidget);
    });

    testWidgets('비밀번호가 비어있으면 변경 버튼이 비활성화된다', (tester) async {
      // Given
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ChangePasswordPage(currentPassword: currentPassword),
          ),
        ),
      );

      // When: 변경 버튼 찾기
      final changeButton = find.widgetWithText(ElevatedButton, '변경');
      final button = tester.widget<ElevatedButton>(changeButton);

      // Then: 버튼이 비활성화 상태
      expect(button.onPressed, null);
    });

    testWidgets('유효한 비밀번호 입력 시 유효성 피드백이 표시된다', (tester) async {
      // Given
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ChangePasswordPage(currentPassword: currentPassword),
          ),
        ),
      );

      // When: 유효한 비밀번호 입력
      final newPasswordField = find.byType(TextFormField).first;
      await tester.enterText(newPasswordField, 'newpass123');
      await tester.pumpAndSettle();

      // Then: 유효성 피드백 표시
      expect(find.text('4글자 이상'), findsOneWidget);
      expect(find.text('동일 문자 반복 불가 (예: 1111, aaaa)'), findsOneWidget);
      expect(find.byIcon(Icons.check_circle), findsNWidgets(2));
    });

    testWidgets('동일 문자 반복 비밀번호는 유효하지 않음', (tester) async {
      // Given
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ChangePasswordPage(currentPassword: currentPassword),
          ),
        ),
      );

      // When: 동일 문자 반복 비밀번호 입력
      final newPasswordField = find.byType(TextFormField).first;
      await tester.enterText(newPasswordField, '1111');
      await tester.pumpAndSettle();

      // Then: 반복 불가 항목이 빨간색으로 표시
      expect(find.byIcon(Icons.check_circle), findsOneWidget); // 길이는 OK
      expect(find.byIcon(Icons.cancel), findsOneWidget); // 반복은 NG
    });

    testWidgets('비밀번호 일치 여부 피드백이 표시된다', (tester) async {
      // Given
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ChangePasswordPage(currentPassword: currentPassword),
          ),
        ),
      );

      // When: 두 비밀번호 입력
      final newPasswordField = find.byType(TextFormField).first;
      final confirmPasswordField = find.byType(TextFormField).last;

      await tester.enterText(newPasswordField, 'newpass123');
      await tester.pumpAndSettle();

      // 일치하는 비밀번호 입력
      await tester.enterText(confirmPasswordField, 'newpass123');
      await tester.pumpAndSettle();

      // Then: 일치 피드백 표시
      expect(find.text('비밀번호가 일치합니다'), findsOneWidget);
    });

    testWidgets('비밀번호 불일치 시 피드백이 표시된다', (tester) async {
      // Given
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ChangePasswordPage(currentPassword: currentPassword),
          ),
        ),
      );

      // When: 불일치하는 비밀번호 입력
      final newPasswordField = find.byType(TextFormField).first;
      final confirmPasswordField = find.byType(TextFormField).last;

      await tester.enterText(newPasswordField, 'newpass123');
      await tester.pumpAndSettle();

      await tester.enterText(confirmPasswordField, 'different');
      await tester.pumpAndSettle();

      // Then: 불일치 피드백 표시
      expect(find.text('비밀번호가 일치하지 않습니다'), findsOneWidget);
    });

    testWidgets('모든 조건 충족 시 변경 버튼이 활성화된다', (tester) async {
      // Given
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ChangePasswordPage(currentPassword: currentPassword),
          ),
        ),
      );

      // When: 유효한 비밀번호 입력
      final newPasswordField = find.byType(TextFormField).first;
      final confirmPasswordField = find.byType(TextFormField).last;

      await tester.enterText(newPasswordField, 'newpass123');
      await tester.pumpAndSettle();

      await tester.enterText(confirmPasswordField, 'newpass123');
      await tester.pumpAndSettle();

      // Then: 변경 버튼이 활성화됨
      final changeButton = find.widgetWithText(ElevatedButton, '변경');
      final button = tester.widget<ElevatedButton>(changeButton);
      expect(button.onPressed, isNotNull);
    });

    testWidgets('안내 문구가 표시된다', (tester) async {
      // When
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ChangePasswordPage(currentPassword: currentPassword),
          ),
        ),
      );

      // Then
      expect(find.text('변경할 비밀번호를 입력합니다.'), findsOneWidget);
      expect(find.text('1. 비밀번호 변경'), findsOneWidget);
      expect(find.text('2. 변경 버튼'), findsOneWidget);
    });
  });
}
