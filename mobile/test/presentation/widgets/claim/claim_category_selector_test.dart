import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_category.dart';
import 'package:mobile/presentation/widgets/claim/claim_category_selector.dart';

void main() {
  group('ClaimCategorySelector', () {
    final testCategories = [
      ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: [
          const ClaimSubcategory(id: 101, name: '벌레'),
          const ClaimSubcategory(id: 102, name: '금속'),
        ],
      ),
      ClaimCategory(
        id: 2,
        name: '변질/변패',
        subcategories: [
          const ClaimSubcategory(id: 201, name: '맛 변질'),
        ],
      ),
    ];

    testWidgets('초기 상태로 렌더링된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimCategorySelector(
              categories: testCategories,
              selectedCategory: null,
              selectedSubcategory: null,
              onCategorySelected: (_) {},
              onSubcategorySelected: (_) {},
            ),
          ),
        ),
      );

      // Then: 라벨 표시
      expect(find.text('클레임 종류1 *'), findsOneWidget);
      expect(find.text('클레임 종류2 *'), findsOneWidget);

      // Then: 종류1 플레이스홀더
      expect(find.text('종류선택'), findsOneWidget);

      // Then: 종류2 비활성 안내
      expect(find.text('종류1을 먼저 선택하세요'), findsOneWidget);
    });

    testWidgets('종류1 선택 시 종류2가 활성화된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimCategorySelector(
              categories: testCategories,
              selectedCategory: testCategories[0],
              selectedSubcategory: null,
              onCategorySelected: (_) {},
              onSubcategorySelected: (_) {},
            ),
          ),
        ),
      );

      // Then: 선택된 종류1 표시
      expect(find.text('이물'), findsOneWidget);

      // Then: 종류2 플레이스홀더 (활성화됨)
      expect(find.text('종류선택'), findsOneWidget);
      expect(find.text('종류1을 먼저 선택하세요'), findsNothing);
    });

    testWidgets('종류1과 종류2 모두 선택 시 라벨이 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimCategorySelector(
              categories: testCategories,
              selectedCategory: testCategories[0],
              selectedSubcategory: testCategories[0].subcategories[0],
              onCategorySelected: (_) {},
              onSubcategorySelected: (_) {},
            ),
          ),
        ),
      );

      // Then: 선택된 종류들 표시
      expect(find.text('이물'), findsOneWidget);
      expect(find.text('벌레'), findsOneWidget);

      // Then: 라벨 표시
      expect(find.text('이물 > 벌레'), findsOneWidget);
    });

    testWidgets('종류1 ListTile 탭 시 바텀시트가 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimCategorySelector(
              categories: testCategories,
              selectedCategory: null,
              selectedSubcategory: null,
              onCategorySelected: (_) {},
              onSubcategorySelected: (_) {},
            ),
          ),
        ),
      );

      // When: 종류1 필드 탭 (첫 번째 ListTile)
      await tester.tap(find.byType(ListTile).first);
      await tester.pumpAndSettle();

      // Then: 바텀시트 표시
      expect(find.text('클레임 종류1 선택'), findsOneWidget);
      expect(find.text('이물'), findsOneWidget);
      expect(find.text('변질/변패'), findsOneWidget);
    });
  });
}
