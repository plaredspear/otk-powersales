import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/shelf_life_item.dart';
import 'package:mobile/domain/repositories/shelf_life_repository.dart';
import 'package:mobile/domain/usecases/get_shelf_life_list_usecase.dart';
import 'package:mobile/presentation/providers/shelf_life_list_provider.dart';
import 'package:mobile/presentation/providers/shelf_life_list_state.dart';

void main() {
  group('ShelfLifeListNotifier', () {
    late ShelfLifeListNotifier notifier;
    late FakeShelfLifeRepository fakeRepository;
    late GetShelfLifeList useCase;

    setUp(() {
      fakeRepository = FakeShelfLifeRepository();
      useCase = GetShelfLifeList(fakeRepository);
      notifier = ShelfLifeListNotifier(getShelfLifeList: useCase);
    });

    test('초기 상태가 올바르게 설정되어야 한다', () {
      expect(notifier.state.isLoading, false);
      expect(notifier.state.items, isEmpty);
      expect(notifier.state.hasSearched, false);
    });

    group('selectStore', () {
      test('거래처를 선택하면 state에 반영되어야 한다', () {
        notifier.selectStore(100, '이마트');

        expect(notifier.state.selectedStoreId, 100);
        expect(notifier.state.selectedStoreName, '이마트');
      });

      test('null을 전달하면 거래처 필터가 초기화되어야 한다', () {
        notifier.selectStore(100, '이마트');
        notifier.selectStore(null, null);

        expect(notifier.state.selectedStoreId, isNull);
        expect(notifier.state.selectedStoreName, isNull);
      });
    });

    group('updateFromDate / updateToDate', () {
      test('시작일을 변경하면 state에 반영되어야 한다', () {
        final newDate = DateTime(2026, 3, 1);
        notifier.updateFromDate(newDate);

        expect(notifier.state.fromDate, newDate);
      });

      test('종료일을 변경하면 state에 반영되어야 한다', () {
        final newDate = DateTime(2026, 4, 1);
        notifier.updateToDate(newDate);

        expect(notifier.state.toDate, newDate);
      });
    });

    group('searchShelfLife', () {
      test('검색 성공 시 items를 업데이트해야 한다', () async {
        fakeRepository.itemsToReturn = [_sampleItem1, _sampleItem2];

        await notifier.searchShelfLife();

        expect(notifier.state.isLoading, false);
        expect(notifier.state.items.length, 2);
        expect(notifier.state.hasSearched, true);
        expect(notifier.state.errorMessage, isNull);
      });

      test('검색 결과가 없으면 빈 목록을 반환해야 한다', () async {
        fakeRepository.itemsToReturn = [];

        await notifier.searchShelfLife();

        expect(notifier.state.items, isEmpty);
        expect(notifier.state.hasSearched, true);
      });

      test('검색 실패 시 에러 메시지를 설정해야 한다', () async {
        fakeRepository.exceptionToThrow =
            Exception('유통기한 검색 기간은 최대 6개월입니다');

        await notifier.searchShelfLife();

        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, '유통기한 검색 기간은 최대 6개월입니다');
      });

      test('검색 중에는 로딩 상태여야 한다', () async {
        fakeRepository.itemsToReturn = [];

        // 검색 시작 전 상태 확인
        expect(notifier.state.isLoading, false);

        final future = notifier.searchShelfLife();

        // 검색 시작 후 로딩 상태 확인
        // (Dart의 Future 특성상 await 전에 상태가 변경됨)
        expect(notifier.state.isLoading, true);

        await future;

        // 검색 완료 후 로딩 해제
        expect(notifier.state.isLoading, false);
      });
    });

    group('clearError', () {
      test('에러 메시지를 초기화해야 한다', () async {
        fakeRepository.exceptionToThrow = Exception('에러');
        await notifier.searchShelfLife();

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
  List<ShelfLifeItem> itemsToReturn = [];
  ShelfLifeItem? registerResult;
  ShelfLifeItem? updateResult;
  int batchDeleteCount = 0;
  Exception? exceptionToThrow;

  @override
  Future<List<ShelfLifeItem>> getShelfLifeList(ShelfLifeFilter filter) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return itemsToReturn;
  }

  @override
  Future<ShelfLifeItem> registerShelfLife(dynamic form) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return registerResult!;
  }

  @override
  Future<ShelfLifeItem> updateShelfLife(int id, dynamic form) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return updateResult!;
  }

  @override
  Future<void> deleteShelfLife(int id) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
  }

  @override
  Future<int> deleteShelfLifeBatch(List<int> ids) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return batchDeleteCount;
  }
}

// ──────────────────────────────────────────────────────────────────
// Sample test data
// ──────────────────────────────────────────────────────────────────

final _sampleItem1 = ShelfLifeItem(
  id: 1,
  productCode: 'P001',
  productName: '진라면',
  storeName: '이마트',
  storeId: 100,
  expiryDate: DateTime(2026, 3, 15),
  alertDate: DateTime(2026, 3, 14),
  dDay: 5,
  description: '',
  isExpired: false,
);

final _sampleItem2 = ShelfLifeItem(
  id: 2,
  productCode: 'P002',
  productName: '케첩',
  storeName: '이마트',
  storeId: 100,
  expiryDate: DateTime(2026, 2, 1),
  alertDate: DateTime(2026, 1, 31),
  dDay: -10,
  description: '',
  isExpired: true,
);
