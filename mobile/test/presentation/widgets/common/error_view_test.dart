import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/common/error_view.dart';

void main() {
  group('ErrorView', () {
    testWidgets('기본 에러 화면이 렌더링된다', (WidgetTester tester) async {
      const testMessage = '데이터를 불러올 수 없습니다';

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ErrorView(message: testMessage),
          ),
        ),
      );

      // 에러 메시지가 표시되는지 확인
      expect(find.text(testMessage), findsOneWidget);

      // 에러 아이콘이 표시되는지 확인
      expect(find.byIcon(Icons.error_outline), findsOneWidget);
    });

    testWidgets('재시도 버튼이 표시된다', (WidgetTester tester) async {
      bool retryPressed = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ErrorView(
              message: '오류 발생',
              onRetry: () {
                retryPressed = true;
              },
            ),
          ),
        ),
      );

      // 재시도 버튼이 표시되는지 확인
      expect(find.text('다시 시도'), findsOneWidget);
      expect(find.byIcon(Icons.refresh), findsOneWidget);

      // 재시도 버튼 클릭
      await tester.tap(find.text('다시 시도'));
      await tester.pump();

      // 콜백이 호출되었는지 확인
      expect(retryPressed, true);
    });

    testWidgets('재시도 콜백이 없으면 버튼이 표시되지 않는다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ErrorView(message: '오류 발생'),
          ),
        ),
      );

      // 재시도 버튼이 표시되지 않아야 함
      expect(find.text('다시 시도'), findsNothing);
      expect(find.byIcon(Icons.refresh), findsNothing);
    });

    testWidgets('커스텀 아이콘이 적용된다', (WidgetTester tester) async {
      const customIcon = Icons.warning;

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ErrorView(
              message: '경고',
              icon: customIcon,
            ),
          ),
        ),
      );

      expect(find.byIcon(customIcon), findsOneWidget);
    });

    testWidgets('커스텀 재시도 버튼 텍스트가 적용된다', (WidgetTester tester) async {
      const customText = '새로고침';

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ErrorView(
              message: '오류',
              onRetry: () {},
              retryButtonText: customText,
            ),
          ),
        ),
      );

      expect(find.text(customText), findsOneWidget);
    });

    testWidgets('설명 텍스트가 표시된다', (WidgetTester tester) async {
      const description = '네트워크 연결을 확인하고 다시 시도해주세요';

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ErrorView(
              message: '오류',
              description: description,
            ),
          ),
        ),
      );

      expect(find.text(description), findsOneWidget);
    });

    testWidgets('설명이 없으면 표시되지 않는다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ErrorView(message: '오류'),
          ),
        ),
      );

      // Text 위젯이 1개만 있어야 함 (메시지만)
      expect(find.byType(Text), findsOneWidget);
    });

    testWidgets('전체 화면 모드가 기본값이다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ErrorView(message: '오류'),
          ),
        ),
      );

      // ErrorView가 렌더링되는지 확인
      expect(find.byType(ErrorView), findsOneWidget);
      // 에러 메시지가 표시되는지 확인
      expect(find.text('오류'), findsOneWidget);
    });

    testWidgets('isFullScreen false일 때도 정상 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ErrorView(
              message: '오류',
              isFullScreen: false,
            ),
          ),
        ),
      );

      // ErrorView가 렌더링되는지 확인
      expect(find.byType(ErrorView), findsOneWidget);
      // 에러 메시지가 표시되는지 확인
      expect(find.text('오류'), findsOneWidget);
    });
  });

  group('ErrorView - 편의 생성자', () {
    testWidgets('network 생성자가 동작한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ErrorView.network(onRetry: () {}),
          ),
        ),
      );

      // 네트워크 아이콘이 표시되는지 확인
      expect(find.byIcon(Icons.wifi_off), findsOneWidget);

      // 기본 메시지가 표시되는지 확인
      expect(find.text('네트워크 연결을 확인해주세요'), findsOneWidget);
    });

    testWidgets('noData 생성자가 동작한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ErrorView.noData(onRetry: () {}),
          ),
        ),
      );

      // 빈 박스 아이콘이 표시되는지 확인
      expect(find.byIcon(Icons.inbox_outlined), findsOneWidget);

      // 기본 메시지가 표시되는지 확인
      expect(find.text('데이터가 없습니다'), findsOneWidget);
    });

    testWidgets('unauthorized 생성자가 동작한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ErrorView.unauthorized(onRetry: () {}),
          ),
        ),
      );

      // 잠금 아이콘이 표시되는지 확인
      expect(find.byIcon(Icons.lock_outline), findsOneWidget);

      // 기본 메시지가 표시되는지 확인
      expect(find.text('접근 권한이 없습니다'), findsOneWidget);

      // 기본 설명이 표시되는지 확인
      expect(find.text('로그인이 필요하거나 권한이 부족합니다'), findsOneWidget);
    });

    testWidgets('network 생성자에서 커스텀 메시지를 사용할 수 있다', (WidgetTester tester) async {
      const customMessage = '인터넷 연결이 끊어졌습니다';

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ErrorView.network(
              message: customMessage,
              onRetry: () {},
            ),
          ),
        ),
      );

      expect(find.text(customMessage), findsOneWidget);
    });
  });

  group('InlineErrorMessage', () {
    testWidgets('인라인 에러 메시지가 렌더링된다', (WidgetTester tester) async {
      const testMessage = '입력값이 올바르지 않습니다';

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: InlineErrorMessage(message: testMessage),
          ),
        ),
      );

      // 에러 메시지가 표시되는지 확인
      expect(find.text(testMessage), findsOneWidget);

      // 에러 아이콘이 표시되는지 확인
      expect(find.byIcon(Icons.error_outline), findsOneWidget);
    });

    testWidgets('재시도 아이콘이 표시된다', (WidgetTester tester) async {
      bool retryPressed = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: InlineErrorMessage(
              message: '오류',
              onRetry: () {
                retryPressed = true;
              },
            ),
          ),
        ),
      );

      // 새로고침 아이콘이 표시되는지 확인
      expect(find.byIcon(Icons.refresh), findsOneWidget);

      // 재시도 아이콘 클릭
      await tester.tap(find.byIcon(Icons.refresh));
      await tester.pump();

      // 콜백이 호출되었는지 확인
      expect(retryPressed, true);
    });

    testWidgets('재시도 콜백이 없으면 아이콘이 표시되지 않는다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: InlineErrorMessage(message: '오류'),
          ),
        ),
      );

      // 새로고침 아이콘이 표시되지 않아야 함
      expect(find.byIcon(Icons.refresh), findsNothing);
    });

    testWidgets('커스텀 배경 색상이 적용된다', (WidgetTester tester) async {
      const backgroundColor = Colors.yellow;

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: InlineErrorMessage(
              message: '경고',
              backgroundColor: backgroundColor,
            ),
          ),
        ),
      );

      final container = tester.widget<Container>(find.byType(Container));
      final decoration = container.decoration as BoxDecoration;

      expect(decoration.color, backgroundColor);
    });

    testWidgets('커스텀 텍스트 색상이 적용된다', (WidgetTester tester) async {
      const textColor = Colors.blue;

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: InlineErrorMessage(
              message: '정보',
              textColor: textColor,
            ),
          ),
        ),
      );

      // 텍스트 색상 확인
      final textWidget = tester.widget<Text>(find.text('정보'));
      expect(textWidget.style?.color, textColor);

      // 아이콘 색상 확인
      final iconWidget = tester.widget<Icon>(find.byIcon(Icons.error_outline));
      expect(iconWidget.color, textColor);
    });

    testWidgets('Container에 border와 borderRadius가 있다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: InlineErrorMessage(message: '오류'),
          ),
        ),
      );

      final container = tester.widget<Container>(find.byType(Container));
      final decoration = container.decoration as BoxDecoration;

      expect(decoration.borderRadius, BorderRadius.circular(8));
      expect(decoration.border, isNotNull);
    });
  });
}
