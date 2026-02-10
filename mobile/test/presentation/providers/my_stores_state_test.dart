import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/my_store.dart';
import 'package:mobile/presentation/providers/my_stores_state.dart';

void main() {
  group('MyStoresState', () {
    group('초기 상태', () {
      test('initial()은 기본값으로 생성된다', () {
        final state = MyStoresState.initial();

        expect(state.isLoading, false);
        expect(state.errorMessage, isNull);
        expect(state.allStores, isEmpty);
        expect(state.filteredStores, isEmpty);
        expect(state.searchKeyword, '');
        expect(state.totalCount, 0);
      });
    });

    group('상태 전환', () {
      test('toLoading()은 로딩 상태로 전환한다', () {
        final state = MyStoresState.initial().toLoading();

        expect(state.isLoading, true);
        expect(state.errorMessage, isNull);
      });

      test('toError()는 에러 상태로 전환한다', () {
        final state = MyStoresState.initial()
            .toLoading()
            .toError('네트워크 오류');

        expect(state.isLoading, false);
        expect(state.errorMessage, '네트워크 오류');
      });

      test('toLoading()은 이전 에러를 초기화한다', () {
        final state = MyStoresState.initial()
            .toError('이전 에러')
            .toLoading();

        expect(state.errorMessage, isNull);
      });
    });

    group('computed 속성', () {
      const stores = [
        MyStore(
          storeId: 1,
          storeName: '테스트마트',
          storeCode: '0000001',
          address: '서울시 강남구',
          representativeName: '홍길동',
        ),
        MyStore(
          storeId: 2,
          storeName: '샘플유통',
          storeCode: '0000002',
          address: '서울시 서초구',
          representativeName: '김철수',
        ),
      ];

      test('displayCount는 filteredStores 길이를 반환한다', () {
        final state = MyStoresState(
          allStores: stores,
          filteredStores: stores,
        );

        expect(state.displayCount, 2);
      });

      test('isSearchEmpty는 검색어 있고 결과 없을 때 true', () {
        final state = MyStoresState(
          allStores: stores,
          filteredStores: const [],
          searchKeyword: '없는거래처',
        );

        expect(state.isSearchEmpty, true);
      });

      test('isSearchEmpty는 검색어 없으면 false', () {
        final state = MyStoresState(
          allStores: stores,
          filteredStores: const [],
          searchKeyword: '',
        );

        expect(state.isSearchEmpty, false);
      });

      test('isStoresEmpty는 로딩 완료 후 거래처 없을 때 true', () {
        final state = MyStoresState.initial();

        expect(state.isStoresEmpty, true);
      });

      test('isStoresEmpty는 로딩 중에는 false', () {
        final state = MyStoresState.initial().toLoading();

        expect(state.isStoresEmpty, false);
      });

      test('isStoresEmpty는 에러 상태에서 false', () {
        final state = MyStoresState.initial().toError('에러');

        expect(state.isStoresEmpty, false);
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경할 수 있다', () {
        final state = MyStoresState.initial();
        final updated = state.copyWith(isLoading: true);

        expect(updated.isLoading, true);
        expect(updated.searchKeyword, '');
        expect(updated.allStores, isEmpty);
      });

      test('errorMessage를 null로 초기화할 수 있다', () {
        final state = MyStoresState.initial().toError('에러');
        final updated = state.copyWith(errorMessage: null);

        expect(updated.errorMessage, isNull);
      });
    });
  });
}
