import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/my_store.dart';
import 'package:mobile/domain/repositories/my_store_repository.dart';
import 'package:mobile/domain/usecases/get_my_stores.dart';

/// 테스트용 Mock Repository
class _MockMyStoreRepository implements MyStoreRepository {
  final MyStoreListResult? _result;
  final Exception? _error;

  _MockMyStoreRepository({MyStoreListResult? result, Exception? error})
      : _result = result,
        _error = error;

  @override
  Future<MyStoreListResult> getMyStores() async {
    if (_error != null) {
      throw _error;
    }
    return _result!;
  }
}

void main() {
  group('GetMyStores UseCase', () {
    test('거래처 목록을 성공적으로 조회한다', () async {
      const stores = [
        MyStore(
          storeId: 1,
          storeName: '테스트마트',
          storeCode: '0000001',
          address: '서울시 강남구',
          representativeName: '홍길동',
          phoneNumber: '010-1234-5678',
        ),
        MyStore(
          storeId: 2,
          storeName: '샘플유통',
          storeCode: '0000002',
          address: '서울시 서초구',
          representativeName: '김철수',
        ),
      ];

      final expectedResult = MyStoreListResult(
        stores: stores,
        totalCount: stores.length,
      );

      final repository = _MockMyStoreRepository(result: expectedResult);
      final useCase = GetMyStores(repository);

      final result = await useCase.call();

      expect(result.stores.length, 2);
      expect(result.totalCount, 2);
      expect(result.stores[0].storeName, '테스트마트');
      expect(result.stores[1].storeName, '샘플유통');
    });

    test('빈 거래처 목록을 반환한다', () async {
      const expectedResult = MyStoreListResult(
        stores: [],
        totalCount: 0,
      );

      final repository = _MockMyStoreRepository(result: expectedResult);
      final useCase = GetMyStores(repository);

      final result = await useCase.call();

      expect(result.stores, isEmpty);
      expect(result.totalCount, 0);
    });

    test('Repository 에러를 전파한다', () async {
      final repository = _MockMyStoreRepository(
        error: Exception('네트워크 오류'),
      );
      final useCase = GetMyStores(repository);

      expect(
        () => useCase.call(),
        throwsA(isA<Exception>()),
      );
    });
  });
}
