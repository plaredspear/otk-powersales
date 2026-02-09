import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/common/loading_indicator.dart';

void main() {
  group('LoadingIndicator', () {
    testWidgets('기본 로딩 인디케이터가 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: LoadingIndicator(),
          ),
        ),
      );

      // CircularProgressIndicator가 표시되는지 확인
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('메시지가 표시된다', (WidgetTester tester) async {
      const testMessage = '데이터를 불러오는 중...';

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: LoadingIndicator(message: testMessage),
          ),
        ),
      );

      // 메시지가 표시되는지 확인
      expect(find.text(testMessage), findsOneWidget);
    });

    testWidgets('메시지가 없으면 표시되지 않는다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: LoadingIndicator(),
          ),
        ),
      );

      // Text 위젯이 하나도 없어야 함 (메시지가 없으므로)
      expect(find.byType(Text), findsNothing);
    });

    testWidgets('커스텀 크기가 적용된다', (WidgetTester tester) async {
      const customSize = 60.0;

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: LoadingIndicator(size: customSize),
          ),
        ),
      );

      final sizedBox = tester.widget<SizedBox>(
        find.ancestor(
          of: find.byType(CircularProgressIndicator),
          matching: find.byType(SizedBox),
        ),
      );

      expect(sizedBox.width, customSize);
      expect(sizedBox.height, customSize);
    });

    testWidgets('커스텀 색상이 적용된다', (WidgetTester tester) async {
      const customColor = Colors.red;

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: LoadingIndicator(color: customColor),
          ),
        ),
      );

      final indicator = tester.widget<CircularProgressIndicator>(
        find.byType(CircularProgressIndicator),
      );

      expect(
        (indicator.valueColor as AlwaysStoppedAnimation<Color>).value,
        customColor,
      );
    });

    testWidgets('전체 화면 모드가 동작한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: LoadingIndicator(isFullScreen: true),
          ),
        ),
      );

      // 전체 화면 컨테이너가 존재하는지 확인
      expect(
        find.byWidgetPredicate(
          (widget) => widget is Container && widget.color != null,
        ),
        findsOneWidget,
      );
    });

    testWidgets('fullScreen 생성자가 동작한다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: LoadingIndicator.fullScreen(
              message: '처리 중...',
            ),
          ),
        ),
      );

      // 전체 화면 컨테이너가 존재하는지 확인
      expect(
        find.byWidgetPredicate(
          (widget) => widget is Container && widget.color != null,
        ),
        findsOneWidget,
      );

      // 메시지가 표시되는지 확인
      expect(find.text('처리 중...'), findsOneWidget);
    });

    testWidgets('전체 화면 모드에서 배경 색상이 적용된다', (WidgetTester tester) async {
      const backgroundColor = Colors.black54;

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: LoadingIndicator.fullScreen(
              backgroundColor: backgroundColor,
            ),
          ),
        ),
      );

      final container = tester.widget<Container>(
        find.byWidgetPredicate(
          (widget) => widget is Container && widget.color != null,
        ),
      );

      expect(container.color, backgroundColor);
    });
  });

  group('OverlayLoadingIndicator', () {
    testWidgets('오버레이 로딩 인디케이터가 렌더링된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: OverlayLoadingIndicator(),
          ),
        ),
      );

      // OverlayLoadingIndicator가 있는지 확인
      expect(find.byType(OverlayLoadingIndicator), findsOneWidget);

      // CircularProgressIndicator가 있는지 확인
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('메시지가 표시된다', (WidgetTester tester) async {
      const testMessage = '업로드 중...';

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: OverlayLoadingIndicator(message: testMessage),
          ),
        ),
      );

      expect(find.text(testMessage), findsOneWidget);
    });

    testWidgets('커스텀 배경 색상이 적용된다', (WidgetTester tester) async {
      const backgroundColor = Color(0x99000000);

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: OverlayLoadingIndicator(
              backgroundColor: backgroundColor,
            ),
          ),
        ),
      );

      // Stack의 첫 번째 자식(배경 Container) 확인
      final container = tester.widget<Container>(
        find.descendant(
          of: find.byType(Stack),
          matching: find.byType(Container),
        ).first,
      );

      expect(container.color, backgroundColor);
    });

    testWidgets('인디케이터 색상이 적용된다', (WidgetTester tester) async {
      const indicatorColor = Colors.blue;

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: OverlayLoadingIndicator(
              indicatorColor: indicatorColor,
            ),
          ),
        ),
      );

      final indicator = tester.widget<CircularProgressIndicator>(
        find.byType(CircularProgressIndicator),
      );

      expect(
        (indicator.valueColor as AlwaysStoppedAnimation<Color>).value,
        indicatorColor,
      );
    });

    testWidgets('흰색 박스 안에 인디케이터가 표시된다', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: OverlayLoadingIndicator(),
          ),
        ),
      );

      // 흰색 박스 Container 찾기
      final whiteBoxContainer = tester.widgetList<Container>(
        find.byType(Container),
      ).firstWhere(
        (container) =>
            container.decoration is BoxDecoration &&
            (container.decoration as BoxDecoration).color == Colors.white,
        orElse: () => throw Exception('White box container not found'),
      );

      final decoration = whiteBoxContainer.decoration as BoxDecoration;
      expect(decoration.color, Colors.white);
      expect(decoration.borderRadius, BorderRadius.circular(12));
    });
  });
}
