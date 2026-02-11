import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/shelf_life_item.dart';
import 'package:mobile/domain/repositories/shelf_life_repository.dart';
import 'package:mobile/domain/usecases/delete_shelf_life_batch_usecase.dart';
import 'package:mobile/presentation/providers/shelf_life_delete_provider.dart';

void main() {
  group('ShelfLifeDeleteNotifier', () {
    late ShelfLifeDeleteNotifier notifier;
    late FakeShelfLifeRepository fakeRepository;
    late DeleteShelfLifeBatch useCase;

    setUp(() {
      fakeRepository = FakeShelfLifeRepository();
      useCase = DeleteShelfLifeBatch(fakeRepository);
      notifier = ShelfLifeDeleteNotifier(deleteBatch: useCase);
    });

    test('초기 상태가 올바르게 설정되어야 한다', () {
      expect(notifier.state.isLoading, false);
      expect(notifier.state.items, isEmpty);
      expect(notifier.state.selectedIds, isEmpty);
      expect(notifier.state.isDeleted, false);
    });

    group('setItems', () {
      test('항목 목록을 설정하고 선택을 초기화해야 한다', () {
        notifier.setItems(_allItems);

        expect(notifier.state.items.length, 4);
        expect(notifier.state.selectedIds, isEmpty);
      });
    });

    group('toggleItem', () {
      test('선택되지 않은 항목을 선택할 수 있어야 한다', () {
        notifier.setItems(_allItems);

        notifier.toggleItem(1);

        expect(notifier.state.selectedIds, contains(1));
        expect(notifier.state.selectedCount, 1);
      });

      test('선택된 항목을 해제할 수 있어야 한다', () {
        notifier.setItems(_allItems);
        notifier.toggleItem(1);
        notifier.toggleItem(1);

        expect(notifier.state.selectedIds, isNot(contains(1)));
        expect(notifier.state.selectedCount, 0);
      });

      test('여러 항목을 개별 선택할 수 있어야 한다', () {
        notifier.setItems(_allItems);

        notifier.toggleItem(1);
        notifier.toggleItem(3);

        expect(notifier.state.selectedIds, {1, 3});
        expect(notifier.state.selectedCount, 2);
      });
    });

    group('toggleAll', () {
      test('전체 선택이 안 됐을 때 전체 선택해야 한다', () {
        notifier.setItems(_allItems);

        notifier.toggleAll();

        expect(notifier.state.isAllSelected, true);
        expect(notifier.state.selectedCount, 4);
      });

      test('전체 선택됐을 때 전체 해제해야 한다', () {
        notifier.setItems(_allItems);
        notifier.toggleAll(); // 전체 선택
        notifier.toggleAll(); // 전체 해제

        expect(notifier.state.isAllSelected, false);
        expect(notifier.state.selectedCount, 0);
      });

      test('일부만 선택됐을 때 전체 선택해야 한다', () {
        notifier.setItems(_allItems);
        notifier.toggleItem(1);

        notifier.toggleAll();

        expect(notifier.state.isAllSelected, true);
        expect(notifier.state.selectedCount, 4);
      });
    });

    group('toggleGroup', () {
      test('만료 그룹 전체를 선택할 수 있어야 한다', () {
        notifier.setItems(_allItems);

        notifier.toggleGroup(expired: true);

        expect(notifier.state.isExpiredGroupSelected, true);
        expect(notifier.state.selectedIds, containsAll([1, 2]));
        expect(notifier.state.selectedIds.length, 2);
      });

      test('만료 그룹 전체를 해제할 수 있어야 한다', () {
        notifier.setItems(_allItems);
        notifier.toggleGroup(expired: true);

        notifier.toggleGroup(expired: true);

        expect(notifier.state.isExpiredGroupSelected, false);
        expect(notifier.state.selectedIds, isEmpty);
      });

      test('만료 전 그룹 전체를 선택할 수 있어야 한다', () {
        notifier.setItems(_allItems);

        notifier.toggleGroup(expired: false);

        expect(notifier.state.isActiveGroupSelected, true);
        expect(notifier.state.selectedIds, containsAll([3, 4]));
      });

      test('만료 전 그룹 전체를 해제할 수 있어야 한다', () {
        notifier.setItems(_allItems);
        notifier.toggleGroup(expired: false);

        notifier.toggleGroup(expired: false);

        expect(notifier.state.isActiveGroupSelected, false);
        expect(notifier.state.selectedIds, isEmpty);
      });

      test('다른 그룹 선택에 영향을 주지 않아야 한다', () {
        notifier.setItems(_allItems);
        notifier.toggleGroup(expired: true); // 만료 그룹 선택

        notifier.toggleGroup(expired: false); // 만료 전 그룹도 선택

        expect(notifier.state.isAllSelected, true);
        expect(notifier.state.selectedCount, 4);
      });
    });

    group('deleteSelected', () {
      test('선택된 항목 삭제 성공 시 isDeleted가 true여야 한다', () async {
        fakeRepository.batchDeleteCount = 2;
        notifier.setItems(_allItems);
        notifier.toggleItem(1);
        notifier.toggleItem(2);

        await notifier.deleteSelected();

        expect(notifier.state.isDeleted, true);
        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, isNull);
      });

      test('선택된 항목이 없으면 삭제하지 않아야 한다', () async {
        notifier.setItems(_allItems);

        await notifier.deleteSelected();

        expect(notifier.state.isDeleted, false);
        expect(fakeRepository.deleteBatchCalls, 0);
      });

      test('삭제 실패 시 에러 메시지를 설정해야 한다', () async {
        fakeRepository.exceptionToThrow = Exception('삭제 실패');
        notifier.setItems(_allItems);
        notifier.toggleItem(1);

        await notifier.deleteSelected();

        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, '삭제 실패');
        expect(notifier.state.isDeleted, false);
      });

      test('삭제 전달된 ID가 올바라야 한다', () async {
        fakeRepository.batchDeleteCount = 2;
        notifier.setItems(_allItems);
        notifier.toggleItem(1);
        notifier.toggleItem(3);

        await notifier.deleteSelected();

        expect(
          fakeRepository.lastBatchDeleteIds?.toSet(),
          {1, 3},
        );
      });
    });

    group('clearError', () {
      test('에러 메시지를 초기화해야 한다', () async {
        fakeRepository.exceptionToThrow = Exception('에러');
        notifier.setItems(_allItems);
        notifier.toggleItem(1);
        await notifier.deleteSelected();

        expect(notifier.state.errorMessage, isNotNull);

        notifier.clearError();

        expect(notifier.state.errorMessage, isNull);
      });
    });
  });
}

// ──────────────────────────────────────────────────────────────────
// Fake Repository
// ──────────────────────────────────────────────────────────────────

class FakeShelfLifeRepository implements ShelfLifeRepository {
  int batchDeleteCount = 0;
  int deleteBatchCalls = 0;
  List<int>? lastBatchDeleteIds;
  Exception? exceptionToThrow;

  @override
  Future<List<ShelfLifeItem>> getShelfLifeList(ShelfLifeFilter filter) async {
    return [];
  }

  @override
  Future<ShelfLifeItem> registerShelfLife(dynamic form) async {
    throw UnimplementedError();
  }

  @override
  Future<ShelfLifeItem> updateShelfLife(int id, dynamic form) async {
    throw UnimplementedError();
  }

  @override
  Future<void> deleteShelfLife(int id) async {
    throw UnimplementedError();
  }

  @override
  Future<int> deleteShelfLifeBatch(List<int> ids) async {
    deleteBatchCalls++;
    lastBatchDeleteIds = ids;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return batchDeleteCount;
  }
}

// ──────────────────────────────────────────────────────────────────
// Sample test data
// ──────────────────────────────────────────────────────────────────

final _expiredItem1 = ShelfLifeItem(
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

final _expiredItem2 = ShelfLifeItem(
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

final _activeItem1 = ShelfLifeItem(
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

final _activeItem2 = ShelfLifeItem(
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

final _allItems = [_expiredItem1, _expiredItem2, _activeItem1, _activeItem2];
