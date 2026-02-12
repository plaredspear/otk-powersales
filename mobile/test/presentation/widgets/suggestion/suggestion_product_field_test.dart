import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/suggestion/suggestion_product_field.dart';

void main() {
  group('SuggestionProductField Widget', () {
    testWidgets('활성 상태로 렌더링된다', (tester) async {
      // When
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SuggestionProductField(
              enabled: true,
              onBarcodePressed: () {},
              onSelectPressed: () {},
            ),
          ),
        ),
      );

      // Then
      expect(find.text('제품'), findsOneWidget);
      expect(find.text('바코드'), findsOneWidget);
      expect(find.text('선택'), findsOneWidget);
      expect(find.text('제품 선택'), findsOneWidget);
    });

    testWidgets('비활성 상태로 렌더링된다', (tester) async {
      // When
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SuggestionProductField(
              enabled: false,
            ),
          ),
        ),
      );

      // Then
      expect(find.text('신제품 제안 시 선택 불필요'), findsOneWidget);

      // 버튼이 비활성 상태인지 확인
      final barcodeButton = tester.widget<OutlinedButton>(
        find.widgetWithText(OutlinedButton, '바코드'),
      );
      expect(barcodeButton.onPressed, null);
    });

    testWidgets('제품 정보가 표시된다', (tester) async {
      // When
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SuggestionProductField(
              enabled: true,
              productName: '진라면',
              productCode: '12345678',
            ),
          ),
        ),
      );

      // Then
      expect(find.text('진라면'), findsOneWidget);
      expect(find.text('12345678'), findsOneWidget);
    });

    testWidgets('바코드 버튼 클릭 시 콜백이 호출된다', (tester) async {
      // Given
      bool barcodePressed = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SuggestionProductField(
              enabled: true,
              onBarcodePressed: () {
                barcodePressed = true;
              },
            ),
          ),
        ),
      );

      // When
      await tester.tap(find.text('바코드'));
      await tester.pumpAndSettle();

      // Then
      expect(barcodePressed, true);
    });

    testWidgets('선택 버튼 클릭 시 콜백이 호출된다', (tester) async {
      // Given
      bool selectPressed = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SuggestionProductField(
              enabled: true,
              onSelectPressed: () {
                selectPressed = true;
              },
            ),
          ),
        ),
      );

      // When
      await tester.tap(find.text('선택'));
      await tester.pumpAndSettle();

      // Then
      expect(selectPressed, true);
    });
  });
}
