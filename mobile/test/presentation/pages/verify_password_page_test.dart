import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/pages/verify_password_page.dart';

void main() {
  group('VerifyPasswordPage', () {
    testWidgets('페이지가 렌더링된다', (tester) async {
      // When
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: VerifyPasswordPage(),
          ),
        ),
      );

      // Then
      expect(find.text('현재 비밀번호 확인'), findsOneWidget);
      expect(find.text('아이디'), findsOneWidget);
      expect(find.text('비밀번호*'), findsOneWidget);
      expect(find.text('확인'), findsOneWidget);
    });

    testWidgets('비밀번호 가시성 토글 버튼이 있다', (tester) async {
      // Given
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: VerifyPasswordPage(),
          ),
        ),
      );

      // Then: 가시성 토글 버튼이 존재
      expect(find.byIcon(Icons.visibility_off), findsOneWidget);

      // When: 가시성 토글 버튼 탭
      await tester.tap(find.byIcon(Icons.visibility_off));
      await tester.pumpAndSettle();

      // Then: 아이콘이 변경됨
      expect(find.byIcon(Icons.visibility), findsOneWidget);
    });

    testWidgets('비밀번호가 비어있으면 확인 버튼이 비활성화된다', (tester) async {
      // Given
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: VerifyPasswordPage(),
          ),
        ),
      );

      // When: 확인 버튼 찾기
      final confirmButton = find.widgetWithText(ElevatedButton, '확인');
      final button = tester.widget<ElevatedButton>(confirmButton);

      // Then: 버튼이 비활성화 상태
      expect(button.onPressed, null);
    });

    testWidgets('비밀번호 입력 시 확인 버튼이 활성화된다', (tester) async {
      // Given
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: VerifyPasswordPage(),
          ),
        ),
      );

      // When: 비밀번호 입력
      final passwordField = find.byType(TextFormField).last;
      await tester.enterText(passwordField, 'test1234');
      await tester.pumpAndSettle();

      // Then: 확인 버튼이 활성화됨
      final confirmButton = find.widgetWithText(ElevatedButton, '확인');
      final button = tester.widget<ElevatedButton>(confirmButton);
      expect(button.onPressed, isNotNull);
    });

    testWidgets('안내 문구가 표시된다', (tester) async {
      // When
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: VerifyPasswordPage(),
          ),
        ),
      );

      // Then
      expect(
        find.text('마이페이지에서 비밀번호 변경 시, 현재 비밀번호를 입력하여 확인합니다.'),
        findsOneWidget,
      );
      expect(
        find.text('1. 비밀번호 입력'),
        findsOneWidget,
      );
      expect(
        find.text('2. 확인 버튼'),
        findsOneWidget,
      );
    });
  });
}
