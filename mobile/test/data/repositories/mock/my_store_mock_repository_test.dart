import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/my_store_mock_repository.dart';
import 'package:mobile/domain/repositories/my_store_repository.dart';

void main() {
  group('MyStoreMockRepository', () {
    late MyStoreMockRepository repository;

    setUp(() {
      repository = MyStoreMockRepository();
    });

    group('getMyStores', () {
      test('거래처 목록을 반환한다', () async {
        final result = await repository.getMyStores();

        expect(result, isA<MyStoreListResult>());
        expect(result.stores, isNotEmpty);
        expect(result.totalCount, result.stores.length);
      });

      test('각 거래처에 필수 필드가 있다', () async {
        final result = await repository.getMyStores();

        for (final store in result.stores) {
          expect(store.storeId, isPositive);
          expect(store.storeName, isNotEmpty);
          expect(store.storeCode, isNotEmpty);
          expect(store.address, isNotEmpty);
          expect(store.representativeName, isNotEmpty);
        }
      });

      test('전화번호가 없는 거래처가 존재한다', () async {
        final result = await repository.getMyStores();

        final storesWithoutPhone =
            result.stores.where((s) => s.phoneNumber == null);

        expect(storesWithoutPhone, isNotEmpty,
            reason: '전화 아이콘 비활성화 UI 테스트를 위해 전화번호 없는 거래처가 필요');
      });

      test('totalCount가 stores 목록 길이와 일치한다', () async {
        final result = await repository.getMyStores();

        expect(result.totalCount, result.stores.length);
      });

      test('MyStoreRepository 인터페이스를 구현한다', () {
        expect(repository, isA<MyStoreRepository>());
      });

      test('반복 호출 시 동일한 데이터를 반환한다', () async {
        final result1 = await repository.getMyStores();
        final result2 = await repository.getMyStores();

        expect(result1.totalCount, result2.totalCount);
        expect(result1.stores.length, result2.stores.length);
      });
    });
  });
}
