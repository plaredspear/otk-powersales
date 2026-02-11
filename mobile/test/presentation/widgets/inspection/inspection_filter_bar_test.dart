import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/presentation/widgets/inspection/inspection_filter_bar.dart';

void main() {
  group('InspectionFilterBar', () {
    final testStores = {
      100: '이마트 죽전점',
      200: '홈플러스 강남점',
      300: '롯데마트 사당점',
    };

    testWidgets('기본 렌더링이 올바르게 동작한다', (tester) async {
      // Given
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: InspectionFilterBar(
              stores: testStores,
              selectedStoreId: null,
              selectedCategory: null,
              fromDate: DateTime(2020, 8, 12),
              toDate: DateTime(2020, 8, 19),
              onStoreChanged: (_, __) {},
              onCategoryChanged: (_) {},
              onFromDateChanged: (_) {},
              onToDateChanged: (_) {},
              onSearch: () {},
            ),
          ),
        ),
      );

      // Then
      expect(find.text('거래처 전체'), findsOneWidget);
      expect(find.text('분류 전체'), findsOneWidget);
      expect(find.text('점검일'), findsOneWidget);
      expect(find.text('2020-08-12'), findsOneWidget);
      expect(find.text('2020-08-19'), findsOneWidget);
      expect(find.text('검색'), findsOneWidget);
    });

    testWidgets('선택된 거래처가 표시된다', (tester) async {
      // Given
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: InspectionFilterBar(
              stores: testStores,
              selectedStoreId: 100,
              selectedCategory: null,
              fromDate: DateTime(2020, 8, 12),
              toDate: DateTime(2020, 8, 19),
              onStoreChanged: (_, __) {},
              onCategoryChanged: (_) {},
              onFromDateChanged: (_) {},
              onToDateChanged: (_) {},
              onSearch: () {},
            ),
          ),
        ),
      );

      // Then
      expect(find.text('이마트 죽전점'), findsOneWidget);
    });

    testWidgets('선택된 분류가 표시된다', (tester) async {
      // Given
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: InspectionFilterBar(
              stores: testStores,
              selectedStoreId: null,
              selectedCategory: InspectionCategory.OWN,
              fromDate: DateTime(2020, 8, 12),
              toDate: DateTime(2020, 8, 19),
              onStoreChanged: (_, __) {},
              onCategoryChanged: (_) {},
              onFromDateChanged: (_) {},
              onToDateChanged: (_) {},
              onSearch: () {},
            ),
          ),
        ),
      );

      // Then
      expect(find.text('자사'), findsOneWidget);
    });

    testWidgets('검색 버튼 탭 시 콜백이 호출된다', (tester) async {
      // Given
      var searchCalled = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: InspectionFilterBar(
              stores: testStores,
              selectedStoreId: null,
              selectedCategory: null,
              fromDate: DateTime(2020, 8, 12),
              toDate: DateTime(2020, 8, 19),
              onStoreChanged: (_, __) {},
              onCategoryChanged: (_) {},
              onFromDateChanged: (_) {},
              onToDateChanged: (_) {},
              onSearch: () {
                searchCalled = true;
              },
            ),
          ),
        ),
      );

      // When
      await tester.tap(find.text('검색'));
      await tester.pump();

      // Then
      expect(searchCalled, true);
    });

    testWidgets('거래처 드롭다운 변경 시 콜백이 호출된다', (tester) async {
      // Given
      int? changedStoreId;
      String? changedStoreName;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: InspectionFilterBar(
              stores: testStores,
              selectedStoreId: null,
              selectedCategory: null,
              fromDate: DateTime(2020, 8, 12),
              toDate: DateTime(2020, 8, 19),
              onStoreChanged: (storeId, storeName) {
                changedStoreId = storeId;
                changedStoreName = storeName;
              },
              onCategoryChanged: (_) {},
              onFromDateChanged: (_) {},
              onToDateChanged: (_) {},
              onSearch: () {},
            ),
          ),
        ),
      );

      // When
      await tester.tap(find.text('거래처 전체').first);
      await tester.pumpAndSettle();
      await tester.tap(find.text('이마트 죽전점').last);
      await tester.pumpAndSettle();

      // Then
      expect(changedStoreId, 100);
      expect(changedStoreName, '이마트 죽전점');
    });

    testWidgets('분류 드롭다운 변경 시 콜백이 호출된다', (tester) async {
      // Given
      InspectionCategory? changedCategory;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: InspectionFilterBar(
              stores: testStores,
              selectedStoreId: null,
              selectedCategory: null,
              fromDate: DateTime(2020, 8, 12),
              toDate: DateTime(2020, 8, 19),
              onStoreChanged: (_, __) {},
              onCategoryChanged: (category) {
                changedCategory = category;
              },
              onFromDateChanged: (_) {},
              onToDateChanged: (_) {},
              onSearch: () {},
            ),
          ),
        ),
      );

      // When
      await tester.tap(find.text('분류 전체').first);
      await tester.pumpAndSettle();
      await tester.tap(find.text('자사').last);
      await tester.pumpAndSettle();

      // Then
      expect(changedCategory, InspectionCategory.OWN);
    });
  });
}
