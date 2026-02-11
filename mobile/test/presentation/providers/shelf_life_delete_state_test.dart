import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/shelf_life_item.dart';
import 'package:mobile/presentation/providers/shelf_life_delete_state.dart';

void main() {
  group('ShelfLifeDeleteState', () {
    final expiredItem1 = ShelfLifeItem(
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

    final expiredItem2 = ShelfLifeItem(
      id: 2,
      productCode: 'P002',
      productName: '케첩',
      storeName: '이마트',
      storeId: 100,
      expiryDate: DateTime(2026, 2, 5),
      alertDate: DateTime(2026, 2, 4),
      dDay: -5,
      description: '',
      isExpired: true,
    );

    final activeItem1 = ShelfLifeItem(
      id: 3,
      productCode: 'P003',
      productName: '카레',
      storeName: '이마트',
      storeId: 100,
      expiryDate: DateTime(2026, 3, 15),
      alertDate: DateTime(2026, 3, 14),
      dDay: 5,
      description: '',
      isExpired: false,
    );

    final activeItem2 = ShelfLifeItem(
      id: 4,
      productCode: 'P004',
      productName: '마요네즈',
      storeName: '이마트',
      storeId: 100,
      expiryDate: DateTime(2026, 3, 20),
      alertDate: DateTime(2026, 3, 19),
      dDay: 10,
      description: '',
      isExpired: false,
    );

    final allItems = [expiredItem1, expiredItem2, activeItem1, activeItem2];

    group('initial', () {
      test('기본값이 올바르게 설정되어야 한다', () {
        final state = ShelfLifeDeleteState.initial();

        expect(state.isLoading, false);
        expect(state.errorMessage, isNull);
        expect(state.items, isEmpty);
        expect(state.selectedIds, isEmpty);
        expect(state.isDeleted, false);
      });
    });

    group('toLoading', () {
      test('로딩 상태로 전환하고 에러를 초기화해야 한다', () {
        final state = ShelfLifeDeleteState.initial()
            .copyWith(errorMessage: '이전 에러');

        final result = state.toLoading();

        expect(result.isLoading, true);
        expect(result.errorMessage, isNull);
      });
    });

    group('toError', () {
      test('에러 상태로 전환하고 로딩을 해제해야 한다', () {
        final state = ShelfLifeDeleteState.initial().toLoading();

        final result = state.toError('삭제 실패');

        expect(result.isLoading, false);
        expect(result.errorMessage, '삭제 실패');
      });
    });

    group('computed getters', () {
      test('expiredItems는 만료된 항목만 반환해야 한다', () {
        final state = ShelfLifeDeleteState.initial().copyWith(
          items: allItems,
        );

        expect(state.expiredItems.length, 2);
        expect(state.expiredItems.map((e) => e.id), containsAll([1, 2]));
      });

      test('activeItems는 만료 전 항목만 반환해야 한다', () {
        final state = ShelfLifeDeleteState.initial().copyWith(
          items: allItems,
        );

        expect(state.activeItems.length, 2);
        expect(state.activeItems.map((e) => e.id), containsAll([3, 4]));
      });

      test('selectedCount는 선택된 항목 수를 반환해야 한다', () {
        final state = ShelfLifeDeleteState.initial().copyWith(
          items: allItems,
          selectedIds: {1, 3},
        );

        expect(state.selectedCount, 2);
      });

      test('canDelete는 선택 항목이 있을 때 true여야 한다', () {
        final state = ShelfLifeDeleteState.initial().copyWith(
          items: allItems,
          selectedIds: {1},
        );

        expect(state.canDelete, true);
      });

      test('canDelete는 선택 항목이 없을 때 false여야 한다', () {
        final state = ShelfLifeDeleteState.initial().copyWith(
          items: allItems,
          selectedIds: {},
        );

        expect(state.canDelete, false);
      });

      test('isAllSelected는 모든 항목이 선택됐을 때 true여야 한다', () {
        final state = ShelfLifeDeleteState.initial().copyWith(
          items: allItems,
          selectedIds: {1, 2, 3, 4},
        );

        expect(state.isAllSelected, true);
      });

      test('isAllSelected는 일부만 선택됐을 때 false여야 한다', () {
        final state = ShelfLifeDeleteState.initial().copyWith(
          items: allItems,
          selectedIds: {1, 2},
        );

        expect(state.isAllSelected, false);
      });

      test('isExpiredGroupSelected는 만료 그룹 전체 선택 시 true여야 한다', () {
        final state = ShelfLifeDeleteState.initial().copyWith(
          items: allItems,
          selectedIds: {1, 2},
        );

        expect(state.isExpiredGroupSelected, true);
      });

      test('isExpiredGroupSelected는 만료 그룹 일부 선택 시 false여야 한다', () {
        final state = ShelfLifeDeleteState.initial().copyWith(
          items: allItems,
          selectedIds: {1},
        );

        expect(state.isExpiredGroupSelected, false);
      });

      test('isActiveGroupSelected는 만료 전 그룹 전체 선택 시 true여야 한다', () {
        final state = ShelfLifeDeleteState.initial().copyWith(
          items: allItems,
          selectedIds: {3, 4},
        );

        expect(state.isActiveGroupSelected, true);
      });

      test('isActiveGroupSelected는 만료 전 그룹 일부 선택 시 false여야 한다', () {
        final state = ShelfLifeDeleteState.initial().copyWith(
          items: allItems,
          selectedIds: {3},
        );

        expect(state.isActiveGroupSelected, false);
      });
    });

    group('copyWith', () {
      test('selectedIds를 올바르게 업데이트해야 한다', () {
        final state = ShelfLifeDeleteState.initial().copyWith(
          items: allItems,
        );

        final result = state.copyWith(selectedIds: {1, 2, 3});

        expect(result.selectedIds, {1, 2, 3});
        expect(result.items, allItems);
      });

      test('isDeleted를 올바르게 업데이트해야 한다', () {
        final state = ShelfLifeDeleteState.initial();

        final result = state.copyWith(isDeleted: true);

        expect(result.isDeleted, true);
      });
    });
  });
}
