import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/claim/claim_product_field.dart';

void main() {
  group('ClaimProductField', () {
    testWidgets('제품 미선택 시 플레이스홀더를 표시한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimProductField(
              productName: null,
              productCode: null,
              onBarcodePressed: () {},
              onProductSelectPressed: () {},
            ),
          ),
        ),
      );

      // Then: 라벨 표시
      expect(find.text('제품 *'), findsOneWidget);

      // Then: 플레이스홀더 표시
      expect(find.text('[제품 선택]'), findsOneWidget);

      // Then: 버튼들 표시
      expect(find.text('바코드'), findsOneWidget);
      expect(find.text('선택'), findsOneWidget);
    });

    testWidgets('제품 선택 시 제품 정보를 표시한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimProductField(
              productName: '맛있는부대찌개라양념140G',
              productCode: '12345678',
              onBarcodePressed: () {},
              onProductSelectPressed: () {},
            ),
          ),
        ),
      );

      // Then: 제품명 표시
      expect(find.text('맛있는부대찌개라양념140G'), findsOneWidget);

      // Then: 제품 코드 표시
      expect(find.text('12345678'), findsOneWidget);

      // Then: 플레이스홀더 미표시
      expect(find.text('[제품 선택]'), findsNothing);
    });

    testWidgets('바코드 버튼 탭 시 콜백이 호출된다', (tester) async {
      bool barcodeCalled = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimProductField(
              productName: null,
              productCode: null,
              onBarcodePressed: () {
                barcodeCalled = true;
              },
              onProductSelectPressed: () {},
            ),
          ),
        ),
      );

      // When: 바코드 버튼 탭
      await tester.tap(find.text('바코드'));
      await tester.pump();

      // Then
      expect(barcodeCalled, true);
    });

    testWidgets('제품 선택 버튼 탭 시 콜백이 호출된다', (tester) async {
      bool selectCalled = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimProductField(
              productName: null,
              productCode: null,
              onBarcodePressed: () {},
              onProductSelectPressed: () {
                selectCalled = true;
              },
            ),
          ),
        ),
      );

      // When: 선택 버튼 탭
      await tester.tap(find.text('선택'));
      await tester.pump();

      // Then
      expect(selectCalled, true);
    });
  });
}
