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
          accountCode: 'ACC100',
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        final result = await repository.getShelfLifeList(filter);

        expect(fakeDataSource.getShelfLifeListCalls, 1);
        expect(result, isList);
        expect(result.length, 2);
        expect(result[0], isA<ShelfLifeItem>());
        expect(result[0].seq, 1);
        expect(result[0].productName, '진라면');
        expect(result[1].seq, 2);
        expect(result[1].productName, '케첩');
      });

      test('필터의 accountCode를 datasource에 전달해야 한다', () async {
        fakeDataSource.shelfLifeListToReturn = [];

        final filter = ShelfLifeFilter(
          accountCode: 'ACC200',
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        await repository.getShelfLifeList(filter);

        expect(fakeDataSource.lastGetListAccountCode, 'ACC200');
      });

      test('필터의 날짜를 YYYY-MM-DD 형식으로 변환하여 전달해야 한다', () async {
        fakeDataSource.shelfLifeListToReturn = [];

        final filter = ShelfLifeFilter(
          accountCode: 'ACC100',
          fromDate: DateTime(2026, 1, 5),
          toDate: DateTime(2026, 3, 10),
        );

        await repository.getShelfLifeList(filter);

        expect(fakeDataSource.lastGetListFromDate, '2026-01-05');
        expect(fakeDataSource.lastGetListToDate, '2026-03-10');
      });

      test('accountCode가 null인 필터도 처리해야 한다', () async {
        fakeDataSource.shelfLifeListToReturn = [];

        final filter = ShelfLifeFilter(
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        await repository.getShelfLifeList(filter);

        expect(fakeDataSource.lastGetListAccountCode, isNull);
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
          accountCode: 'ACC100',
          accountName: '이마트 강남점',
          productCode: 'P001',
          productName: '진라면',
          expiryDate: DateTime(2026, 3, 15),
          alertDate: DateTime(2026, 3, 8),
          description: '3층 선반',
        );

        final result = await repository.registerShelfLife(form);

        expect(fakeDataSource.registerShelfLifeCalls, 1);
        expect(result, isA<ShelfLifeItem>());
        expect(result.seq, 1);
        expect(result.productCode, 'P001');
        expect(result.productName, '진라면');
      });

      test('Form을 Request 모델로 변환하여 전달해야 한다', () async {
        fakeDataSource.registerResultToReturn = _sampleModel1;

        final form = ShelfLifeRegisterForm(
          accountCode: 'ACC100',
          accountName: '이마트 강남점',
          productCode: 'P001',
          productName: '진라면',
          expiryDate: DateTime(2026, 3, 15),
          alertDate: DateTime(2026, 3, 8),
          description: '3층 선반',
        );

        await repository.registerShelfLife(form);

        final capturedRequest = fakeDataSource.lastRegisterRequest!;
        expect(capturedRequest.accountCode, 'ACC100');
        expect(capturedRequest.accountName, '이마트 강남점');
        expect(capturedRequest.productCode, 'P001');
        expect(capturedRequest.productName, '진라면');
        expect(capturedRequest.expirationDate, '2026-03-15');
        expect(capturedRequest.alarmDate, '2026-03-08');
        expect(capturedRequest.description, '3층 선반');
      });
    });

    group('updateShelfLife', () {
      test('datasource를 호출하고 수정된 엔티티를 반환해야 한다', () async {
        fakeDataSource.updateResultToReturn = const ShelfLifeItemModel(
          seq: 1,
          productCode: 'P001',
          productName: '진라면',
          accountName: '이마트 강남점',
          accountCode: 'ACC100',
          expirationDate: '2026-04-01',
          alarmDate: '2026-03-25',
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
        expect(result.seq, 1);
        expect(result.expiryDate, DateTime(2026, 4, 1));
        expect(result.description, '수정된 메모');
      });

      test('seq와 Request 모델을 datasource에 전달해야 한다', () async {
        fakeDataSource.updateResultToReturn = _sampleModel1;

        final form = ShelfLifeUpdateForm(
          expiryDate: DateTime(2026, 4, 1),
          alertDate: DateTime(2026, 3, 25),
          description: '수정됨',
        );

        await repository.updateShelfLife(42, form);

        expect(fakeDataSource.lastUpdateSeq, 42);
        final capturedRequest = fakeDataSource.lastUpdateRequest!;
        expect(capturedRequest.expirationDate, '2026-04-01');
        expect(capturedRequest.alarmDate, '2026-03-25');
        expect(capturedRequest.description, '수정됨');
      });
    });

    group('deleteShelfLife', () {
      test('datasource를 호출해야 한다', () async {
        await repository.deleteShelfLife(1);

        expect(fakeDataSource.deleteShelfLifeCalls, 1);
        expect(fakeDataSource.lastDeleteSeq, 1);
      });

      test('올바른 seq를 전달해야 한다', () async {
        await repository.deleteShelfLife(42);

        expect(fakeDataSource.lastDeleteSeq, 42);
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

      test('seq 목록을 datasource에 전달해야 한다', () async {
        fakeDataSource.batchDeleteResultToReturn =
            const ShelfLifeBatchDeleteResponse(deletedCount: 2);

        await repository.deleteShelfLifeBatch([10, 20]);

        expect(fakeDataSource.lastBatchDeleteSeqs, [10, 20]);
      });

      test('빈 목록도 처리해야 한다', () async {
        fakeDataSource.batchDeleteResultToReturn =
            const ShelfLifeBatchDeleteResponse(deletedCount: 0);

        final result = await repository.deleteShelfLifeBatch([]);

        expect(result, 0);
        expect(fakeDataSource.lastBatchDeleteSeqs, isEmpty);
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
  String? lastGetListAccountCode;
  String? lastGetListFromDate;
  String? lastGetListToDate;
  ShelfLifeRegisterRequest? lastRegisterRequest;
  int? lastUpdateSeq;
  ShelfLifeUpdateRequest? lastUpdateRequest;
  int? lastDeleteSeq;
  List<int>? lastBatchDeleteSeqs;

  @override
  Future<List<ShelfLifeItemModel>> getShelfLifeList({
    String? accountCode,
    required String fromDate,
    required String toDate,
  }) async {
    getShelfLifeListCalls++;
    lastGetListAccountCode = accountCode;
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
    int seq,
    ShelfLifeUpdateRequest request,
  ) async {
    updateShelfLifeCalls++;
    lastUpdateSeq = seq;
    lastUpdateRequest = request;
    return updateResultToReturn!;
  }

  @override
  Future<void> deleteShelfLife(int seq) async {
    deleteShelfLifeCalls++;
    lastDeleteSeq = seq;
  }

  @override
  Future<ShelfLifeBatchDeleteResponse> deleteShelfLifeBatch(
    List<int> seqs,
  ) async {
    deleteShelfLifeBatchCalls++;
    lastBatchDeleteSeqs = seqs;
    return batchDeleteResultToReturn;
  }
}

// ──────────────────────────────────────────────────────────────────
// Sample test data
// ──────────────────────────────────────────────────────────────────

const _sampleModel1 = ShelfLifeItemModel(
  seq: 1,
  productCode: 'P001',
  productName: '진라면',
  accountName: '이마트 강남점',
  accountCode: 'ACC100',
  expirationDate: '2026-03-15',
  alarmDate: '2026-03-08',
  dDay: 32,
  description: '3층 선반',
  isExpired: false,
);

const _sampleModel2 = ShelfLifeItemModel(
  seq: 2,
  productCode: 'P002',
  productName: '케첩',
  accountName: '이마트 강남점',
  accountCode: 'ACC100',
  expirationDate: '2026-02-20',
  alarmDate: '2026-02-13',
  dDay: -3,
  description: '',
  isExpired: true,
);
