import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/inspection/inspection_competitor_form.dart';

void main() {
  group('InspectionCompetitorForm Widget', () {
    Widget buildTestWidget({
      String? competitorName,
      String? competitorActivity,
      bool? competitorTasting,
      String? competitorProductName,
      int? competitorProductPrice,
      int? competitorSalesQuantity,
      ValueChanged<String>? onCompetitorNameChanged,
      ValueChanged<String>? onCompetitorActivityChanged,
      ValueChanged<bool>? onCompetitorTastingChanged,
      ValueChanged<String>? onCompetitorProductNameChanged,
      ValueChanged<String>? onCompetitorProductPriceChanged,
      ValueChanged<String>? onCompetitorSalesQuantityChanged,
    }) {
      return MaterialApp(
        home: Scaffold(
          body: InspectionCompetitorForm(
            competitorName: competitorName,
            competitorActivity: competitorActivity,
            competitorTasting: competitorTasting,
            competitorProductName: competitorProductName,
            competitorProductPrice: competitorProductPrice,
            competitorSalesQuantity: competitorSalesQuantity,
            onCompetitorNameChanged: onCompetitorNameChanged ?? (_) {},
            onCompetitorActivityChanged: onCompetitorActivityChanged ?? (_) {},
            onCompetitorTastingChanged: onCompetitorTastingChanged ?? (_) {},
            onCompetitorProductNameChanged:
                onCompetitorProductNameChanged ?? (_) {},
            onCompetitorProductPriceChanged:
                onCompetitorProductPriceChanged ?? (_) {},
            onCompetitorSalesQuantityChanged:
                onCompetitorSalesQuantityChanged ?? (_) {},
          ),
        ),
      );
    }

    testWidgets('경쟁사 활동 정보 섹션 헤더가 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());

      // Then
      expect(find.text('경쟁사 활동 정보'), findsOneWidget);
    });

    testWidgets('경쟁사명 필수 필드가 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());

      // Then
      expect(
        find.widgetWithText(TextField, '경쟁사명 *'),
        findsOneWidget,
      );
    });

    testWidgets('경쟁사 활동 내용 필수 필드가 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());

      // Then
      expect(
        find.widgetWithText(TextField, '경쟁사 활동 내용 *'),
        findsOneWidget,
      );
    });

    testWidgets('시식 여부 토글이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());

      // Then
      expect(find.byType(ToggleButtons), findsOneWidget);
      expect(find.text('예'), findsOneWidget);
      expect(find.text('아니요'), findsOneWidget);
    });

    testWidgets('경쟁사명 초기값이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(competitorName: '농심'),
      );

      // Then
      final textField = tester.widget<TextField>(
        find.widgetWithText(TextField, '경쟁사명 *'),
      );
      expect(textField.controller!.text, '농심');
    });

    testWidgets('경쟁사 활동 내용 초기값이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(competitorActivity: '시식 행사'),
      );

      // Then
      final textField = tester.widget<TextField>(
        find.widgetWithText(TextField, '경쟁사 활동 내용 *'),
      );
      expect(textField.controller!.text, '시식 행사');
    });

    testWidgets('경쟁사명 입력 시 콜백이 호출된다', (tester) async {
      // Given
      String? changedName;
      await tester.pumpWidget(
        buildTestWidget(
          onCompetitorNameChanged: (name) => changedName = name,
        ),
      );

      // When
      await tester.enterText(
        find.widgetWithText(TextField, '경쟁사명 *'),
        '삼양식품',
      );
      await tester.pumpAndSettle();

      // Then
      expect(changedName, '삼양식품');
    });

    testWidgets('경쟁사 활동 내용 입력 시 콜백이 호출된다', (tester) async {
      // Given
      String? changedActivity;
      await tester.pumpWidget(
        buildTestWidget(
          onCompetitorActivityChanged: (activity) =>
              changedActivity = activity,
        ),
      );

      // When
      await tester.enterText(
        find.widgetWithText(TextField, '경쟁사 활동 내용 *'),
        '할인 행사',
      );
      await tester.pumpAndSettle();

      // Then
      expect(changedActivity, '할인 행사');
    });

    testWidgets('시식 여부 토글 시 콜백이 호출된다', (tester) async {
      // Given
      bool? changedTasting;
      await tester.pumpWidget(
        buildTestWidget(
          competitorTasting: false,
          onCompetitorTastingChanged: (tasting) => changedTasting = tasting,
        ),
      );

      // When: 예 선택
      await tester.tap(find.text('예'));
      await tester.pumpAndSettle();

      // Then
      expect(changedTasting, true);
    });

    testWidgets('시식=아니요 시 조건부 필드가 표시되지 않는다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(competitorTasting: false),
      );

      // Then: 시식 관련 필드 없음
      expect(find.text('경쟁사 상품명 *'), findsNothing);
      expect(find.text('제품 가격 *'), findsNothing);
      expect(find.text('판매 수량 *'), findsNothing);
    });

    testWidgets('시식=예 시 조건부 필드가 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(competitorTasting: true),
      );

      // Then: 시식 관련 필드 표시됨
      expect(find.text('경쟁사 상품명 *'), findsOneWidget);
      expect(find.text('제품 가격 *'), findsOneWidget);
      expect(find.text('판매 수량 *'), findsOneWidget);
    });

    testWidgets('시식=예 시 경쟁사 상품명 초기값이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(
          competitorTasting: true,
          competitorProductName: '신라면 블랙',
        ),
      );

      // Then
      final textField = tester.widget<TextField>(
        find.widgetWithText(TextField, '경쟁사 상품명 *'),
      );
      expect(textField.controller!.text, '신라면 블랙');
    });

    testWidgets('시식=예 시 제품 가격 초기값이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(
          competitorTasting: true,
          competitorProductPrice: 5000,
        ),
      );

      // Then
      final textField = tester.widget<TextField>(
        find.widgetWithText(TextField, '제품 가격 *'),
      );
      expect(textField.controller!.text, '5000');
    });

    testWidgets('시식=예 시 판매 수량 초기값이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(
          competitorTasting: true,
          competitorSalesQuantity: 50,
        ),
      );

      // Then
      final textField = tester.widget<TextField>(
        find.widgetWithText(TextField, '판매 수량 *'),
      );
      expect(textField.controller!.text, '50');
    });

    testWidgets('시식=예 시 경쟁사 상품명 입력 시 콜백이 호출된다', (tester) async {
      // Given
      String? changedProductName;
      await tester.pumpWidget(
        buildTestWidget(
          competitorTasting: true,
          onCompetitorProductNameChanged: (name) =>
              changedProductName = name,
        ),
      );

      // When
      await tester.enterText(
        find.widgetWithText(TextField, '경쟁사 상품명 *'),
        '너구리',
      );
      await tester.pumpAndSettle();

      // Then
      expect(changedProductName, '너구리');
    });

    testWidgets('시식=예 시 제품 가격 입력 시 콜백이 호출된다', (tester) async {
      // Given
      String? changedPrice;
      await tester.pumpWidget(
        buildTestWidget(
          competitorTasting: true,
          onCompetitorProductPriceChanged: (price) => changedPrice = price,
        ),
      );

      // When
      await tester.enterText(
        find.widgetWithText(TextField, '제품 가격 *'),
        '3000',
      );
      await tester.pumpAndSettle();

      // Then
      expect(changedPrice, '3000');
    });

    testWidgets('시식=예 시 판매 수량 입력 시 콜백이 호출된다', (tester) async {
      // Given
      String? changedQuantity;
      await tester.pumpWidget(
        buildTestWidget(
          competitorTasting: true,
          onCompetitorSalesQuantityChanged: (quantity) =>
              changedQuantity = quantity,
        ),
      );

      // When
      await tester.enterText(
        find.widgetWithText(TextField, '판매 수량 *'),
        '100',
      );
      await tester.pumpAndSettle();

      // Then
      expect(changedQuantity, '100');
    });
  });
}
