import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/product_expiration_remote_datasource.dart';
import 'package:mobile/data/models/product_expiration_item_model.dart';
import 'package:mobile/data/models/product_expiration_register_request.dart';
import 'package:mobile/data/models/product_expiration_update_request.dart';
import 'package:mobile/data/repositories/product_expiration_repository_impl.dart';
import 'package:mobile/domain/entities/product_expiration_form.dart';
import 'package:mobile/domain/entities/product_expiration_item.dart';

void main() {
  late ProductExpirationRepositoryImpl repository;
  late FakeProductExpirationRemoteDataSource fakeDataSource;

  setUp(() {
    fakeDataSource = FakeProductExpirationRemoteDataSource();
    repository = ProductExpirationRepositoryImpl(remoteDataSource: fakeDataSource);
  });

  group('ProductExpirationRepositoryImpl', () {
    group('getProductExpirationList', () {
      test('datasource를 호출하고 엔티티 목록을 반환해야 한다', () async {
        fakeDataSource.productExpirationListToReturn = [
          _sampleModel1,
          _sampleModel2,
        ];

        final filter = ProductExpirationFilter(
          accountCode: 'ACC100',
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        final result = await repository.getProductExpirationList(filter);

        expect(fakeDataSource.getProductExpirationListCalls, 1);
        expect(result, isList);
        expect(result.length, 2);
        expect(result[0], isA<ProductExpirationItem>());
        expect(result[0].seq, 1);
        expect(result[0].productName, '진라면');
        expect(result[1].seq, 2);
        expect(result[1].productName, '케첩');
      });

      test('필터의 accountCode를 datasource에 전달해야 한다', () async {
        fakeDataSource.productExpirationListToReturn = [];

        final filter = ProductExpirationFilter(
          accountCode: 'ACC200',
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        await repository.getProductExpirationList(filter);

        expect(fakeDataSource.lastGetListAccountCode, 'ACC200');
      });

      test('필터의 날짜를 YYYY-MM-DD 형식으로 변환하여 전달해야 한다', () async {
        fakeDataSource.productExpirationListToReturn = [];

        final filter = ProductExpirationFilter(
          accountCode: 'ACC100',
          fromDate: DateTime(2026, 1, 5),
          toDate: DateTime(2026, 3, 10),
        );

        await repository.getProductExpirationList(filter);

        expect(fakeDataSource.lastGetListFromDate, '2026-01-05');
        expect(fakeDataSource.lastGetListToDate, '2026-03-10');
      });

      test('accountCode가 null인 필터도 처리해야 한다', () async {
        fakeDataSource.productExpirationListToReturn = [];

        final filter = ProductExpirationFilter(
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        await repository.getProductExpirationList(filter);

        expect(fakeDataSource.lastGetListAccountCode, isNull);
      });

      test('빈 목록을 올바르게 반환해야 한다', () async {
        fakeDataSource.productExpirationListToReturn = [];

        final filter = ProductExpirationFilter(
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        final result = await repository.getProductExpirationList(filter);

        expect(result, isEmpty);
      });

      test('모델의 날짜 문자열을 DateTime으로 변환해야 한다', () async {
        fakeDataSource.productExpirationListToReturn = [_sampleModel1];

        final filter = ProductExpirationFilter(
          fromDate: DateTime(2026, 2, 1),
          toDate: DateTime(2026, 2, 28),
        );

        final result = await repository.getProductExpirationList(filter);

        expect(result[0].expiryDate, DateTime(2026, 3, 15));
        expect(result[0].alertDate, DateTime(2026, 3, 8));
      });
    });

    group('registerProductExpiration', () {
      test('datasource를 호출하고 등록된 엔티티를 반환해야 한다', () async {
        fakeDataSource.registerResultToReturn = _sampleModel1;

        final form = ProductExpirationRegisterForm(
          accountCode: 'ACC100',
          accountName: '이마트 강남점',
          productCode: 'P001',
          productName: '진라면',
          expiryDate: DateTime(2026, 3, 15),
          alertDate: DateTime(2026, 3, 8),
          description: '3층 선반',
        );

        final result = await repository.registerProductExpiration(form);

        expect(fakeDataSource.registerProductExpirationCalls, 1);
        expect(result, isA<ProductExpirationItem>());
        expect(result.seq, 1);
        expect(result.productCode, 'P001');
        expect(result.productName, '진라면');
      });

      test('Form을 Request 모델로 변환하여 전달해야 한다', () async {
        fakeDataSource.registerResultToReturn = _sampleModel1;

        final form = ProductExpirationRegisterForm(
          accountCode: 'ACC100',
          accountName: '이마트 강남점',
          productCode: 'P001',
          productName: '진라면',
          expiryDate: DateTime(2026, 3, 15),
          alertDate: DateTime(2026, 3, 8),
          description: '3층 선반',
        );

        await repository.registerProductExpiration(form);

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

    group('updateProductExpiration', () {
      test('datasource를 호출하고 수정된 엔티티를 반환해야 한다', () async {
        fakeDataSource.updateResultToReturn = const ProductExpirationItemModel(
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

        final form = ProductExpirationUpdateForm(
          expiryDate: DateTime(2026, 4, 1),
          alertDate: DateTime(2026, 3, 25),
          description: '수정된 메모',
        );

        final result = await repository.updateProductExpiration(1, form);

        expect(fakeDataSource.updateProductExpirationCalls, 1);
        expect(result, isA<ProductExpirationItem>());
        expect(result.seq, 1);
        expect(result.expiryDate, DateTime(2026, 4, 1));
        expect(result.description, '수정된 메모');
      });

      test('seq와 Request 모델을 datasource에 전달해야 한다', () async {
        fakeDataSource.updateResultToReturn = _sampleModel1;

        final form = ProductExpirationUpdateForm(
          expiryDate: DateTime(2026, 4, 1),
          alertDate: DateTime(2026, 3, 25),
          description: '수정됨',
        );

        await repository.updateProductExpiration(42, form);

        expect(fakeDataSource.lastUpdateSeq, 42);
        final capturedRequest = fakeDataSource.lastUpdateRequest!;
        expect(capturedRequest.expirationDate, '2026-04-01');
        expect(capturedRequest.alarmDate, '2026-03-25');
        expect(capturedRequest.description, '수정됨');
      });
    });

    group('deleteProductExpiration', () {
      test('datasource를 호출해야 한다', () async {
        await repository.deleteProductExpiration(1);

        expect(fakeDataSource.deleteProductExpirationCalls, 1);
        expect(fakeDataSource.lastDeleteSeq, 1);
      });

      test('올바른 seq를 전달해야 한다', () async {
        await repository.deleteProductExpiration(42);

        expect(fakeDataSource.lastDeleteSeq, 42);
      });
    });

    group('deleteProductExpirationBatch', () {
      test('datasource를 호출하고 삭제 건수를 반환해야 한다', () async {
        fakeDataSource.batchDeleteResultToReturn =
            const ProductExpirationBatchDeleteResponse(deletedCount: 3);

        final result = await repository.deleteProductExpirationBatch([1, 2, 3]);

        expect(fakeDataSource.deleteProductExpirationBatchCalls, 1);
        expect(result, 3);
      });

      test('seq 목록을 datasource에 전달해야 한다', () async {
        fakeDataSource.batchDeleteResultToReturn =
            const ProductExpirationBatchDeleteResponse(deletedCount: 2);

        await repository.deleteProductExpirationBatch([10, 20]);

        expect(fakeDataSource.lastBatchDeleteSeqs, [10, 20]);
      });

      test('빈 목록도 처리해야 한다', () async {
        fakeDataSource.batchDeleteResultToReturn =
            const ProductExpirationBatchDeleteResponse(deletedCount: 0);

        final result = await repository.deleteProductExpirationBatch([]);

        expect(result, 0);
        expect(fakeDataSource.lastBatchDeleteSeqs, isEmpty);
      });
    });
  });
}

// ──────────────────────────────────────────────────────────────────
// Fake DataSource
// ──────────────────────────────────────────────────────────────────

class FakeProductExpirationRemoteDataSource implements ProductExpirationRemoteDataSource {
  // ─── Call counters ─────────────────────────────────────────────
  int getProductExpirationListCalls = 0;
  int registerProductExpirationCalls = 0;
  int updateProductExpirationCalls = 0;
  int deleteProductExpirationCalls = 0;
  int deleteProductExpirationBatchCalls = 0;

  // ─── Return values ─────────────────────────────────────────────
  List<ProductExpirationItemModel> productExpirationListToReturn = [];
  ProductExpirationItemModel? registerResultToReturn;
  ProductExpirationItemModel? updateResultToReturn;
  ProductExpirationBatchDeleteResponse batchDeleteResultToReturn =
      const ProductExpirationBatchDeleteResponse(deletedCount: 0);

  // ─── Captured parameters ───────────────────────────────────────
  String? lastGetListAccountCode;
  String? lastGetListFromDate;
  String? lastGetListToDate;
  ProductExpirationRegisterRequest? lastRegisterRequest;
  int? lastUpdateSeq;
  ProductExpirationUpdateRequest? lastUpdateRequest;
  int? lastDeleteSeq;
  List<int>? lastBatchDeleteSeqs;

  @override
  Future<List<ProductExpirationItemModel>> getProductExpirationList({
    String? accountCode,
    required String fromDate,
    required String toDate,
  }) async {
    getProductExpirationListCalls++;
    lastGetListAccountCode = accountCode;
    lastGetListFromDate = fromDate;
    lastGetListToDate = toDate;
    return productExpirationListToReturn;
  }

  @override
  Future<ProductExpirationItemModel> registerProductExpiration(
    ProductExpirationRegisterRequest request,
  ) async {
    registerProductExpirationCalls++;
    lastRegisterRequest = request;
    return registerResultToReturn!;
  }

  @override
  Future<ProductExpirationItemModel> updateProductExpiration(
    int seq,
    ProductExpirationUpdateRequest request,
  ) async {
    updateProductExpirationCalls++;
    lastUpdateSeq = seq;
    lastUpdateRequest = request;
    return updateResultToReturn!;
  }

  @override
  Future<void> deleteProductExpiration(int seq) async {
    deleteProductExpirationCalls++;
    lastDeleteSeq = seq;
  }

  @override
  Future<ProductExpirationBatchDeleteResponse> deleteProductExpirationBatch(
    List<int> seqs,
  ) async {
    deleteProductExpirationBatchCalls++;
    lastBatchDeleteSeqs = seqs;
    return batchDeleteResultToReturn;
  }
}

// ──────────────────────────────────────────────────────────────────
// Sample test data
// ──────────────────────────────────────────────────────────────────

const _sampleModel1 = ProductExpirationItemModel(
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

const _sampleModel2 = ProductExpirationItemModel(
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
