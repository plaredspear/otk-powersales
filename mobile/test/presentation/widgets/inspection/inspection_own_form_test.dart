import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/inspection/inspection_own_form.dart';

void main() {
  group('InspectionOwnForm Widget', () {
    Widget buildTestWidget({
      String? selectedProductName,
      String? description,
      ValueChanged<String>? onDescriptionChanged,
      VoidCallback? onBarcodeScan,
      VoidCallback? onProductSelect,
    }) {
      return MaterialApp(
        home: Scaffold(
          body: InspectionOwnForm(
            selectedProductName: selectedProductName,
            description: description,
            onDescriptionChanged: onDescriptionChanged ?? (_) {},
            onBarcodeScan: onBarcodeScan ?? () {},
            onProductSelect: onProductSelect ?? () {},
          ),
        ),
      );
    }

    testWidgets('자사 활동 정보 섹션 헤더가 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());

      // Then
      expect(find.text('자사 활동 정보'), findsOneWidget);
    });

    testWidgets('설명 입력 필드가 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());

      // Then
      expect(find.byType(TextField), findsOneWidget);
      expect(
        find.widgetWithText(TextField, '설명'),
        findsOneWidget,
      );
    });

    testWidgets('설명 초기값이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(description: '냉장고 앞 본매대'),
      );

      // Then
      final textField = tester.widget<TextField>(find.byType(TextField));
      expect(textField.controller!.text, '냉장고 앞 본매대');
    });

    testWidgets('설명 입력 시 콜백이 호출된다', (tester) async {
      // Given
      String? changedDescription;
      await tester.pumpWidget(
        buildTestWidget(
          onDescriptionChanged: (desc) => changedDescription = desc,
        ),
      );

      // When
      await tester.enterText(find.byType(TextField), '새로운 설명');
      await tester.pumpAndSettle();

      // Then
      expect(changedDescription, '새로운 설명');
    });

    testWidgets('제품 필수 필드 레이블이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());

      // Then: RichText로 "제품 *" 렌더링됨
      expect(find.byType(RichText), findsWidgets);
    });

    testWidgets('제품 선택 전 안내 문구가 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget(selectedProductName: null));

      // Then
      expect(find.text('제품을 선택하세요'), findsOneWidget);
    });

    testWidgets('선택된 제품명이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(selectedProductName: '진라면'),
      );

      // Then
      expect(find.text('진라면'), findsOneWidget);
      expect(find.text('제품을 선택하세요'), findsNothing);
    });

    testWidgets('바코드 버튼이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());

      // Then
      expect(find.text('바코드'), findsOneWidget);
      expect(find.byIcon(Icons.qr_code_scanner), findsOneWidget);
    });

    testWidgets('제품 선택 버튼이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());

      // Then
      expect(find.text('선택'), findsOneWidget);
      expect(find.byIcon(Icons.add), findsOneWidget);
    });

    testWidgets('바코드 버튼 탭 시 콜백이 호출된다', (tester) async {
      // Given
      var tapped = false;
      await tester.pumpWidget(
        buildTestWidget(onBarcodeScan: () => tapped = true),
      );

      // When
      await tester.tap(find.text('바코드'));
      await tester.pumpAndSettle();

      // Then
      expect(tapped, true);
    });

    testWidgets('제품 선택 버튼 탭 시 콜백이 호출된다', (tester) async {
      // Given
      var tapped = false;
      await tester.pumpWidget(
        buildTestWidget(onProductSelect: () => tapped = true),
      );

      // When
      await tester.tap(find.text('선택'));
      await tester.pumpAndSettle();

      // Then
      expect(tapped, true);
    });
  });
}
