import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/presentation/widgets/common/loading_indicator.dart';

void main() {
  Widget wrap(Widget child) =>
      MaterialApp(theme: AppTheme.light, home: Scaffold(body: child));

  Color spinnerColor(WidgetTester tester) {
    final indicator = tester.widget<CircularProgressIndicator>(
      find.byType(CircularProgressIndicator),
    );
    return indicator.valueColor!.value!;
  }

  group('LoadingIndicator', () {
    testWidgets('흰 배경에서 보이지 않는 노랑 대신 네이비로 그린다', (tester) async {
      await tester.pumpWidget(wrap(const LoadingIndicator()));

      expect(spinnerColor(tester), AppColors.secondary);
      expect(spinnerColor(tester), isNot(AppColors.primary));
    });

    testWidgets('문구를 지정하지 않아도 기본 안내 문구를 함께 표시한다', (tester) async {
      await tester.pumpWidget(wrap(const LoadingIndicator()));

      expect(find.text(LoadingIndicator.defaultMessage), findsOneWidget);
    });

    testWidgets('지정한 문구가 있으면 그대로 표시한다', (tester) async {
      await tester.pumpWidget(
        wrap(const LoadingIndicator(message: '주문 목록을 불러오는 중...')),
      );

      expect(find.text('주문 목록을 불러오는 중...'), findsOneWidget);
      expect(find.text(LoadingIndicator.defaultMessage), findsNothing);
    });

    testWidgets('기본 형태는 배경 카드를 그린다', (tester) async {
      await tester.pumpWidget(wrap(const LoadingIndicator()));

      final decoration = tester
          .widgetList<Container>(find.byType(Container))
          .map((c) => c.decoration)
          .whereType<BoxDecoration>()
          .toList();
      expect(decoration, isNotEmpty);
      expect(decoration.first.color, AppColors.surface);
      expect(decoration.first.boxShadow, isNotEmpty);
    });

    testWidgets('inline 은 카드 없이 스피너만 그린다', (tester) async {
      await tester.pumpWidget(wrap(const LoadingIndicator.inline()));

      final decoration = tester
          .widgetList<Container>(find.byType(Container))
          .map((c) => c.decoration)
          .whereType<BoxDecoration>();
      expect(decoration, isEmpty);
      expect(find.text(LoadingIndicator.defaultMessage), findsNothing);
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });
  });

  group('OverlayLoadingIndicator', () {
    testWidgets('반투명 배경 위에 카드형 인디케이터를 표시한다', (tester) async {
      await tester.pumpWidget(
        wrap(const OverlayLoadingIndicator(message: '제출 중...')),
      );

      expect(find.text('제출 중...'), findsOneWidget);
      expect(spinnerColor(tester), AppColors.secondary);
    });
  });
}
