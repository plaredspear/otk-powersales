import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/shelf_life_item.dart';
import 'package:mobile/presentation/providers/shelf_life_list_state.dart';

void main() {
  group('ShelfLifeListState', () {
    group('initial', () {
      test('기본값이 올바르게 설정되어야 한다', () {
        final state = ShelfLifeListState.initial();

        expect(state.isLoading, false);
        expect(state.errorMessage, isNull);
        expect(state.items, isEmpty);
        expect(state.hasSearched, false);
        expect(state.selectedStoreId, isNull);
        expect(state.selectedStoreName, isNull);
        expect(state.stores, isEmpty);
      });

      test('기본 날짜 범위는 오늘 기준 앞/뒤 7일이어야 한다', () {
        final state = ShelfLifeListState.initial();
        final now = DateTime.now();
        final today = DateTime(now.year, now.month, now.day);

        expect(
          state.fromDate,
          today.subtract(const Duration(days: 7)),
        );
        expect(
          state.toDate,
          today.add(const Duration(days: 7)),
        );
      });
    });

    group('toLoading', () {
      test('로딩 상태로 전환하고 에러를 초기화해야 한다', () {
        final state = ShelfLifeListState.initial()
            .copyWith(errorMessage: '이전 에러');

        final result = state.toLoading();

        expect(result.isLoading, true);
        expect(result.errorMessage, isNull);
      });
    });

    group('toError', () {
      test('에러 상태로 전환하고 로딩을 해제해야 한다', () {
        final state = ShelfLifeListState.initial().toLoading();

        final result = state.toError('에러 발생');

        expect(result.isLoading, false);
        expect(result.errorMessage, '에러 발생');
      });
    });

    group('computed getters', () {
      final expiredItem = ShelfLifeItem(
        id: 1,
        productCode: 'P001',
        productName: '진라면',
        storeName: '이마트',
        storeId: 100,
        expiryDate: DateTime(2026, 2, 1),
        alertDate: DateTime(2026, 1, 31),
        dDay: -10,
        description: '',
        isExpired: true,
      );

      final activeItem1 = ShelfLifeItem(
        id: 2,
        productCode: 'P002',
        productName: '케첩',
        storeName: '이마트',
        storeId: 100,
        expiryDate: DateTime(2026, 3, 15),
        alertDate: DateTime(2026, 3, 14),
        dDay: 5,
        description: '',
        isExpired: false,
      );

      final activeItem2 = ShelfLifeItem(
        id: 3,
        productCode: 'P003',
        productName: '카레',
        storeName: '이마트',
        storeId: 100,
        expiryDate: DateTime(2026, 3, 12),
        alertDate: DateTime(2026, 3, 11),
        dDay: 2,
        description: '',
        isExpired: false,
      );

      test('expiredItems는 만료된 항목만 반환해야 한다', () {
        final state = ShelfLifeListState.initial().copyWith(
          items: [expiredItem, activeItem1, activeItem2],
        );

        expect(state.expiredItems.length, 1);
        expect(state.expiredItems.first.id, 1);
      });

      test('activeItems는 만료 전 항목만 반환해야 한다', () {
        final state = ShelfLifeListState.initial().copyWith(
          items: [expiredItem, activeItem1, activeItem2],
        );

        expect(state.activeItems.length, 2);
      });

      test('activeItems는 D-DAY 오름차순으로 정렬되어야 한다', () {
        final state = ShelfLifeListState.initial().copyWith(
          items: [expiredItem, activeItem1, activeItem2],
        );

        expect(state.activeItems[0].dDay, 2);
        expect(state.activeItems[1].dDay, 5);
      });

      test('totalCount는 전체 항목 수를 반환해야 한다', () {
        final state = ShelfLifeListState.initial().copyWith(
          items: [expiredItem, activeItem1],
        );

        expect(state.totalCount, 2);
      });

      test('hasResults는 항목이 있을 때 true여야 한다', () {
        final state = ShelfLifeListState.initial().copyWith(
          items: [activeItem1],
        );

        expect(state.hasResults, true);
      });

      test('isEmpty는 검색 후 결과가 없을 때 true여야 한다', () {
        final state = ShelfLifeListState.initial().copyWith(
          hasSearched: true,
          items: [],
        );

        expect(state.isEmpty, true);
      });

      test('isEmpty는 검색 전에는 false여야 한다', () {
        final state = ShelfLifeListState.initial();

        expect(state.isEmpty, false);
      });
    });

    group('copyWith', () {
      test('선택적 필드를 올바르게 업데이트해야 한다', () {
        final state = ShelfLifeListState.initial();

        final result = state.copyWith(
          isLoading: true,
          selectedStoreId: 100,
          selectedStoreName: '이마트',
        );

        expect(result.isLoading, true);
        expect(result.selectedStoreId, 100);
        expect(result.selectedStoreName, '이마트');
        // 나머지는 유지
        expect(result.hasSearched, false);
        expect(result.items, isEmpty);
      });

      test('clearStoreFilter가 true이면 거래처 필터를 초기화해야 한다', () {
        final state = ShelfLifeListState.initial().copyWith(
          selectedStoreId: 100,
          selectedStoreName: '이마트',
        );

        final result = state.copyWith(clearStoreFilter: true);

        expect(result.selectedStoreId, isNull);
        expect(result.selectedStoreName, isNull);
      });

      test('errorMessage를 null로 초기화할 수 있어야 한다', () {
        final state = ShelfLifeListState.initial()
            .copyWith(errorMessage: '에러');

        // copyWith에서 errorMessage 파라미터를 제공하지 않으면
        // null이 됨 (의도적으로 errorMessage 초기화)
        final result = state.copyWith(isLoading: false);

        expect(result.errorMessage, isNull);
      });
    });
  });
}
