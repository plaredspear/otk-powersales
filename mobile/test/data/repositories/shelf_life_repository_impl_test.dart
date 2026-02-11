import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/shelf_life_remote_datasource.dart';
import 'package:mobile/data/models/shelf_life_item_model.dart';
import 'package:mobile/data/models/shelf_life_register_request.dart';
import 'package:mobile/data/models/shelf_life_update_request.dart';
import 'package:mobile/data/repositories/shelf_life_repository_impl.dart';
import 'package:mobile/domain/entities/shelf_life_form.dart';
import 'package:mobile/domain/entities/shelf_life_item.dart';

void main() {
  late ShelfLifeRepositoryImpl repository;
  late FakeShelfLifeRemoteDataSource fakeDataSource;

  setUp(() {
    fakeDataSource = FakeShelfLifeRemoteDataSource();
    repository = ShelfLifeRepositoryImpl(remoteDataSource: fakeDataSource);
  });

  group('ShelfLifeRepositoryImpl', () {
    group('getShelfLifeList', () {
      test('datasource를 호출하고 엔티티 목록을 반환해야 한다', () async {
        fakeDataSource.shelfLifeListToReturn = [
          _sampleModel1,
          _sampleModel2,
        ];

        final filter = ShelfLifeFilter(
          storeId: 100,
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        final result = await repository.getShelfLifeList(filter);

        expect(fakeDataSource.getShelfLifeListCalls, 1);
        expect(result, isList);
        expect(result.length, 2);
        expect(result[0], isA<ShelfLifeItem>());
        expect(result[0].id, 1);
        expect(result[0].productName, '진라면');
        expect(result[1].id, 2);
        expect(result[1].productName, '케첩');
      });

      test('필터의 storeId를 datasource에 전달해야 한다', () async {
        fakeDataSource.shelfLifeListToReturn = [];

        final filter = ShelfLifeFilter(
          storeId: 200,
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        await repository.getShelfLifeList(filter);

        expect(fakeDataSource.lastGetListStoreId, 200);
      });

      test('필터의 날짜를 YYYY-MM-DD 형식으로 변환하여 전달해야 한다', () async {
        fakeDataSource.shelfLifeListToReturn = [];

        final filter = ShelfLifeFilter(
          storeId: 100,
          fromDate: DateTime(2026, 1, 5),
          toDate: DateTime(2026, 3, 10),
        );

        await repository.getShelfLifeList(filter);

        expect(fakeDataSource.lastGetListFromDate, '2026-01-05');
        expect(fakeDataSource.lastGetListToDate, '2026-03-10');
      });

      test('storeId가 null인 필터도 처리해야 한다', () async {
        fakeDataSource.shelfLifeListToReturn = [];

        final filter = ShelfLifeFilter(
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        await repository.getShelfLifeList(filter);

        expect(fakeDataSource.lastGetListStoreId, isNull);
      });

      test('빈 목록을 올바르게 반환해야 한다', () async {
        fakeDataSource.shelfLifeListToReturn = [];

        final filter = ShelfLifeFilter(
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        final result = await repository.getShelfLifeList(filter);

        expect(result, isEmpty);
      });

      test('모델의 날짜 문자열을 DateTime으로 변환해야 한다', () async {
        fakeDataSource.shelfLifeListToReturn = [_sampleModel1];

        final filter = ShelfLifeFilter(
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        final result = await repository.getShelfLifeList(filter);

        expect(result[0].expiryDate, DateTime(2026, 3, 15));
        expect(result[0].alertDate, DateTime(2026, 3, 8));
      });
    });

    group('registerShelfLife', () {
      test('datasource를 호출하고 등록된 엔티티를 반환해야 한다', () async {
        fakeDataSource.registerResultToReturn = _sampleModel1;

        final form = ShelfLifeRegisterForm(
          storeId: 100,
          productCode: 'P001',
          expiryDate: DateTime(2026, 3, 15),
          alertDate: DateTime(2026, 3, 8),
          description: '3층 선반',
        );

        final result = await repository.registerShelfLife(form);

        expect(fakeDataSource.registerShelfLifeCalls, 1);
        expect(result, isA<ShelfLifeItem>());
        expect(result.id, 1);
        expect(result.productCode, 'P001');
        expect(result.productName, '진라면');
      });

      test('Form을 Request 모델로 변환하여 전달해야 한다', () async {
        fakeDataSource.registerResultToReturn = _sampleModel1;

        final form = ShelfLifeRegisterForm(
          storeId: 100,
          productCode: 'P001',
          expiryDate: DateTime(2026, 3, 15),
          alertDate: DateTime(2026, 3, 8),
          description: '3층 선반',
        );

        await repository.registerShelfLife(form);

        final capturedRequest = fakeDataSource.lastRegisterRequest!;
        expect(capturedRequest.storeId, 100);
        expect(capturedRequest.productCode, 'P001');
        expect(capturedRequest.expiryDate, '2026-03-15');
        expect(capturedRequest.alertDate, '2026-03-08');
        expect(capturedRequest.description, '3층 선반');
      });
    });

    group('updateShelfLife', () {
      test('datasource를 호출하고 수정된 엔티티를 반환해야 한다', () async {
        fakeDataSource.updateResultToReturn = const ShelfLifeItemModel(
          id: 1,
          productCode: 'P001',
          productName: '진라면',
          storeName: '이마트 강남점',
          storeId: 100,
          expiryDate: '2026-04-01',
          alertDate: '2026-03-25',
          dDay: 49,
          description: '수정된 메모',
          isExpired: false,
        );

        final form = ShelfLifeUpdateForm(
          expiryDate: DateTime(2026, 4, 1),
          alertDate: DateTime(2026, 3, 25),
          description: '수정된 메모',
        );

        final result = await repository.updateShelfLife(1, form);

        expect(fakeDataSource.updateShelfLifeCalls, 1);
        expect(result, isA<ShelfLifeItem>());
        expect(result.id, 1);
        expect(result.expiryDate, DateTime(2026, 4, 1));
        expect(result.description, '수정된 메모');
      });

      test('id와 Request 모델을 datasource에 전달해야 한다', () async {
        fakeDataSource.updateResultToReturn = _sampleModel1;

        final form = ShelfLifeUpdateForm(
          expiryDate: DateTime(2026, 4, 1),
          alertDate: DateTime(2026, 3, 25),
          description: '수정됨',
        );

        await repository.updateShelfLife(42, form);

        expect(fakeDataSource.lastUpdateId, 42);
        final capturedRequest = fakeDataSource.lastUpdateRequest!;
        expect(capturedRequest.expiryDate, '2026-04-01');
        expect(capturedRequest.alertDate, '2026-03-25');
        expect(capturedRequest.description, '수정됨');
      });
    });

    group('deleteShelfLife', () {
      test('datasource를 호출해야 한다', () async {
        await repository.deleteShelfLife(1);

        expect(fakeDataSource.deleteShelfLifeCalls, 1);
        expect(fakeDataSource.lastDeleteId, 1);
      });

      test('올바른 id를 전달해야 한다', () async {
        await repository.deleteShelfLife(42);

        expect(fakeDataSource.lastDeleteId, 42);
      });
    });

    group('deleteShelfLifeBatch', () {
      test('datasource를 호출하고 삭제 건수를 반환해야 한다', () async {
        fakeDataSource.batchDeleteResultToReturn =
            const ShelfLifeBatchDeleteResponse(deletedCount: 3);

        final result = await repository.deleteShelfLifeBatch([1, 2, 3]);

        expect(fakeDataSource.deleteShelfLifeBatchCalls, 1);
        expect(result, 3);
      });

      test('id 목록을 datasource에 전달해야 한다', () async {
        fakeDataSource.batchDeleteResultToReturn =
            const ShelfLifeBatchDeleteResponse(deletedCount: 2);

        await repository.deleteShelfLifeBatch([10, 20]);

        expect(fakeDataSource.lastBatchDeleteIds, [10, 20]);
      });

      test('빈 목록도 처리해야 한다', () async {
        fakeDataSource.batchDeleteResultToReturn =
            const ShelfLifeBatchDeleteResponse(deletedCount: 0);

        final result = await repository.deleteShelfLifeBatch([]);

        expect(result, 0);
        expect(fakeDataSource.lastBatchDeleteIds, isEmpty);
      });
    });
  });
}

// ──────────────────────────────────────────────────────────────────
// Fake DataSource
// ──────────────────────────────────────────────────────────────────

class FakeShelfLifeRemoteDataSource implements ShelfLifeRemoteDataSource {
  // ─── Call counters ─────────────────────────────────────────────
  int getShelfLifeListCalls = 0;
  int registerShelfLifeCalls = 0;
  int updateShelfLifeCalls = 0;
  int deleteShelfLifeCalls = 0;
  int deleteShelfLifeBatchCalls = 0;

  // ─── Return values ─────────────────────────────────────────────
  List<ShelfLifeItemModel> shelfLifeListToReturn = [];
  ShelfLifeItemModel? registerResultToReturn;
  ShelfLifeItemModel? updateResultToReturn;
  ShelfLifeBatchDeleteResponse batchDeleteResultToReturn =
      const ShelfLifeBatchDeleteResponse(deletedCount: 0);

  // ─── Captured parameters ───────────────────────────────────────
  int? lastGetListStoreId;
  String? lastGetListFromDate;
  String? lastGetListToDate;
  ShelfLifeRegisterRequest? lastRegisterRequest;
  int? lastUpdateId;
  ShelfLifeUpdateRequest? lastUpdateRequest;
  int? lastDeleteId;
  List<int>? lastBatchDeleteIds;

  @override
  Future<List<ShelfLifeItemModel>> getShelfLifeList({
    int? storeId,
    required String fromDate,
    required String toDate,
  }) async {
    getShelfLifeListCalls++;
    lastGetListStoreId = storeId;
    lastGetListFromDate = fromDate;
    lastGetListToDate = toDate;
    return shelfLifeListToReturn;
  }

  @override
  Future<ShelfLifeItemModel> registerShelfLife(
    ShelfLifeRegisterRequest request,
  ) async {
    registerShelfLifeCalls++;
    lastRegisterRequest = request;
    return registerResultToReturn!;
  }

  @override
  Future<ShelfLifeItemModel> updateShelfLife(
    int shelfLifeId,
    ShelfLifeUpdateRequest request,
  ) async {
    updateShelfLifeCalls++;
    lastUpdateId = shelfLifeId;
    lastUpdateRequest = request;
    return updateResultToReturn!;
  }

  @override
  Future<void> deleteShelfLife(int shelfLifeId) async {
    deleteShelfLifeCalls++;
    lastDeleteId = shelfLifeId;
  }

  @override
  Future<ShelfLifeBatchDeleteResponse> deleteShelfLifeBatch(
    List<int> ids,
  ) async {
    deleteShelfLifeBatchCalls++;
    lastBatchDeleteIds = ids;
    return batchDeleteResultToReturn;
  }
}

// ──────────────────────────────────────────────────────────────────
// Sample test data
// ──────────────────────────────────────────────────────────────────

const _sampleModel1 = ShelfLifeItemModel(
  id: 1,
  productCode: 'P001',
  productName: '진라면',
  storeName: '이마트 강남점',
  storeId: 100,
  expiryDate: '2026-03-15',
  alertDate: '2026-03-08',
  dDay: 32,
  description: '3층 선반',
  isExpired: false,
);

const _sampleModel2 = ShelfLifeItemModel(
  id: 2,
  productCode: 'P002',
  productName: '케첩',
  storeName: '이마트 강남점',
  storeId: 100,
  expiryDate: '2026-02-20',
  alertDate: '2026-02-13',
  dDay: -3,
  description: '',
  isExpired: true,
);
