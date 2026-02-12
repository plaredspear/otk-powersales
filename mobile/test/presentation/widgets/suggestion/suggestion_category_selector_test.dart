import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/suggestion_form.dart';
import 'package:mobile/presentation/widgets/suggestion/suggestion_category_selector.dart';

void main() {
  group('SuggestionCategorySelector Widget', () {
    testWidgets('렌더링 테스트', (tester) async {
      // Given
      SuggestionCategory? selectedCategory = SuggestionCategory.newProduct;

      // When
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SuggestionCategorySelector(
              selectedCategory: selectedCategory,
              onCategoryChanged: (category) {
                selectedCategory = category;
              },
            ),
          ),
        ),
      );

      // Then
      expect(find.text('분류 *'), findsOneWidget);
      expect(find.text('신제품 제안'), findsOneWidget);
      expect(find.text('기존제품 상품가치향상'), findsOneWidget);
    });

    testWidgets('신제품 제안이 선택되어 있다', (tester) async {
      // When
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SuggestionCategorySelector(
              selectedCategory: SuggestionCategory.newProduct,
              onCategoryChanged: (_) {},
            ),
          ),
        ),
      );

      // Then
      final radioTiles = tester.widgetList<RadioListTile<SuggestionCategory>>(
        find.byType(RadioListTile<SuggestionCategory>),
      );
      expect(radioTiles.first.groupValue, SuggestionCategory.newProduct);
    });

    testWidgets('분류 변경 시 콜백이 호출된다', (tester) async {
      // Given
      SuggestionCategory selectedCategory = SuggestionCategory.newProduct;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StatefulBuilder(
              builder: (context, setState) {
                return SuggestionCategorySelector(
                  selectedCategory: selectedCategory,
                  onCategoryChanged: (category) {
                    setState(() {
                      selectedCategory = category;
                    });
                  },
                );
              },
            ),
          ),
        ),
      );

      // When - 기존제품 라디오 버튼 탭
      await tester.tap(find.text('기존제품 상품가치향상'));
      await tester.pumpAndSettle();

      // Then
      expect(selectedCategory, SuggestionCategory.existingProduct);
    });
  });
}
