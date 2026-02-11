import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/presentation/providers/inspection_list_state.dart';

void main() {
  group('InspectionListState', () {
    test('초기 상태가 올바르게 생성된다', () {
      // When
      final state = InspectionListState.initial();

      // Then
      expect(state.isLoading, false);
      expect(state.errorMessage, null);
      expect(state.items, isEmpty);
      expect(state.hasSearched, false);
      expect(state.selectedStoreId, null);
      expect(state.selectedStoreName, null);
      expect(state.selectedCategory, null);
      expect(state.stores, isEmpty);

      // 기본 날짜 범위: 오늘 기준 앞 7일 ~ 오늘
      final now = DateTime.now();
      final today = DateTime(now.year, now.month, now.day);
      expect(state.fromDate, today.subtract(const Duration(days: 7)));
      expect(state.toDate, today);
    });

    test('toLoading은 로딩 상태로 전환한다', () {
      // Given
      final state = InspectionListState.initial().copyWith(
        errorMessage: '이전 에러',
      );

      // When
      final loadingState = state.toLoading();

      // Then
      expect(loadingState.isLoading, true);
      expect(loadingState.errorMessage, null);
    });

    test('toError는 에러 상태로 전환한다', () {
      // Given
      final state = InspectionListState.initial().copyWith(
        isLoading: true,
      );

      // When
      final errorState = state.toError('네트워크 오류');

      // Then
      expect(errorState.isLoading, false);
      expect(errorState.errorMessage, '네트워크 오류');
    });

    test('ownItems는 자사 점검만 반환한다', () {
      // Given
      final items = [
        InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 100,
          inspectionDate: DateTime(2020, 8, 13),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        ),
        InspectionListItem(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '홈플러스',
          storeId: 200,
          inspectionDate: DateTime(2020, 8, 14),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        ),
        InspectionListItem(
          id: 3,
          category: InspectionCategory.OWN,
          storeName: '롯데마트',
          storeId: 300,
          inspectionDate: DateTime(2020, 8, 15),
          fieldType: '행사매대',
          fieldTypeCode: 'FT03',
        ),
      ];
      final state = InspectionListState.initial().copyWith(items: items);

      // When
      final ownItems = state.ownItems;

      // Then
      expect(ownItems.length, 2);
      expect(ownItems[0].id, 1);
      expect(ownItems[1].id, 3);
      expect(ownItems.every((item) => item.category == InspectionCategory.OWN), true);
    });

    test('competitorItems는 경쟁사 점검만 반환한다', () {
      // Given
      final items = [
        InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 100,
          inspectionDate: DateTime(2020, 8, 13),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        ),
        InspectionListItem(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '홈플러스',
          storeId: 200,
          inspectionDate: DateTime(2020, 8, 14),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        ),
      ];
      final state = InspectionListState.initial().copyWith(items: items);

      // When
      final competitorItems = state.competitorItems;

      // Then
      expect(competitorItems.length, 1);
      expect(competitorItems[0].id, 2);
      expect(competitorItems.every((item) => item.category == InspectionCategory.COMPETITOR), true);
    });

    test('totalCount는 전체 항목 수를 반환한다', () {
      // Given
      final items = [
        InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 100,
          inspectionDate: DateTime(2020, 8, 13),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        ),
        InspectionListItem(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '홈플러스',
          storeId: 200,
          inspectionDate: DateTime(2020, 8, 14),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        ),
      ];
      final state = InspectionListState.initial().copyWith(items: items);

      // Then
      expect(state.totalCount, 2);
    });

    test('hasResults는 항목이 있을 때 true를 반환한다', () {
      // Given
      final items = [
        InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 100,
          inspectionDate: DateTime(2020, 8, 13),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        ),
      ];
      final state = InspectionListState.initial().copyWith(items: items);

      // Then
      expect(state.hasResults, true);
    });

    test('isEmpty는 검색 후 결과가 없을 때 true를 반환한다', () {
      // Given
      final state = InspectionListState.initial().copyWith(
        hasSearched: true,
        items: [],
      );

      // Then
      expect(state.isEmpty, true);
    });

    test('isEmpty는 검색 전에는 false를 반환한다', () {
      // Given
      final state = InspectionListState.initial();

      // Then
      expect(state.isEmpty, false);
    });

    test('copyWith는 선택적으로 필드를 업데이트한다', () {
      // Given
      final state = InspectionListState.initial();

      // When
      final updated = state.copyWith(
        isLoading: true,
        selectedStoreId: 100,
        selectedStoreName: '이마트',
        selectedCategory: InspectionCategory.OWN,
      );

      // Then
      expect(updated.isLoading, true);
      expect(updated.selectedStoreId, 100);
      expect(updated.selectedStoreName, '이마트');
      expect(updated.selectedCategory, InspectionCategory.OWN);
      expect(updated.fromDate, state.fromDate); // 변경되지 않음
    });

    test('copyWith의 clearStoreFilter는 거래처 필터를 초기화한다', () {
      // Given
      final state = InspectionListState.initial().copyWith(
        selectedStoreId: 100,
        selectedStoreName: '이마트',
      );

      // When
      final updated = state.copyWith(clearStoreFilter: true);

      // Then
      expect(updated.selectedStoreId, null);
      expect(updated.selectedStoreName, null);
    });

    test('copyWith의 clearCategoryFilter는 분류 필터를 초기화한다', () {
      // Given
      final state = InspectionListState.initial().copyWith(
        selectedCategory: InspectionCategory.OWN,
      );

      // When
      final updated = state.copyWith(clearCategoryFilter: true);

      // Then
      expect(updated.selectedCategory, null);
    });
  });
}
