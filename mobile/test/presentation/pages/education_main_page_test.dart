import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/presentation/pages/education_main_page.dart';

void main() {
  group('EducationMainPage', () {
    Widget buildTestWidget() {
      return const MaterialApp(
        home: EducationMainPage(),
      );
    }

    testWidgets('페이지가 렌더링된다', (WidgetTester tester) async {
      // When
      await tester.pumpWidget(buildTestWidget());

      // Then
      expect(find.byType(EducationMainPage), findsOneWidget);
      expect(find.text('교육 자료'), findsOneWidget);
    });

    testWidgets('안내 문구가 표시된다', (WidgetTester tester) async {
      // When
      await tester.pumpWidget(buildTestWidget());

      // Then
      expect(find.textContaining('카테고리를 선택하여'), findsOneWidget);
    });

    testWidgets('4개의 카테고리 카드가 표시된다', (WidgetTester tester) async {
      // When
      await tester.pumpWidget(buildTestWidget());

      // Then
      expect(find.text('시식 매뉴얼'), findsOneWidget);
      expect(find.text('CS/안전'), findsOneWidget);
      expect(find.text('교육 평가'), findsOneWidget);
      expect(find.text('신제품 소개'), findsOneWidget);
    });

    testWidgets('하단 안내 문구가 표시된다', (WidgetTester tester) async {
      // When
      await tester.pumpWidget(buildTestWidget());

      // Then
      expect(find.textContaining('교육 자료는 정기적으로 업데이트됩니다'), findsOneWidget);
    });

    testWidgets('GridView가 2x2 레이아웃이다', (WidgetTester tester) async {
      // When
      await tester.pumpWidget(buildTestWidget());

      // Then
      final gridView = tester.widget<GridView>(find.byType(GridView));
      expect(gridView.semanticChildCount, EducationCategory.values.length);
    });
  });
}
