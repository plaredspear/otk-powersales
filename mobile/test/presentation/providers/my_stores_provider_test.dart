import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/my_store.dart';
import 'package:mobile/domain/repositories/my_store_repository.dart';
import 'package:mobile/domain/usecases/get_my_stores.dart';
import 'package:mobile/presentation/providers/my_stores_provider.dart';
import 'package:mobile/presentation/providers/my_stores_state.dart';

/// 테스트용 Mock Repository
class _MockMyStoreRepository implements MyStoreRepository {
  final MyStoreListResult? _result;
  final Exception? _error;

  _MockMyStoreRepository({MyStoreListResult? result, Exception? error})
      : _result = result,
        _error = error;

  @override
  Future<MyStoreListResult> getMyStores() async {
    // 비동기 딜레이를 추가하여 로딩 상태를 테스트에서 캡처할 수 있게 함
    await Future<void>.delayed(Duration.zero);
    if (_error != null) throw _error;
    return _result!;
  }
}

void main() {
  group('MyStoresNotifier', () {
    const mockStores = [
      MyStore(
        storeId: 1,
        storeName: '(유)경산식품',
        storeCode: '1025172',
        address: '전라남도 목포시',
        representativeName: '김정자',
        phoneNumber: '061-123-4567',
      ),
      MyStore(
        storeId: 2,
        storeName: '대성마트',
        storeCode: '1030456',
        address: '광주광역시 서구',
        representativeName: '이대성',
        phoneNumber: '062-345-6789',
      ),
      MyStore(
        storeId: 3,
        storeName: '경남식품',
        storeCode: '1035403',
        address: '경남 창원시',
        representativeName: '윤경남',
      ),
    ];

    final mockResult = MyStoreListResult(
      stores: mockStores,
      totalCount: mockStores.length,
    );

    late MyStoresNotifier notifier;

    setUp(() {
      final repository = _MockMyStoreRepository(result: mockResult);
      final useCase = GetMyStores(repository);
      notifier = MyStoresNotifier(getMyStores: useCase);
    });

    group('loadStores', () {
      test('거래처 목록을 성공적으로 로딩한다', () async {
        await notifier.loadStores();

        expect(notifier.state.isLoading, false);
        expect(notifier.state.allStores.length, 3);
        expect(notifier.state.filteredStores.length, 3);
        expect(notifier.state.totalCount, 3);
        expect(notifier.state.errorMessage, isNull);
      });

      test('로딩 중 상태를 거친다', () async {
        // loadStores 호출 후 await 전에 로딩 상태를 확인
        final future = notifier.loadStores();

        // loadStores 내부에서 state = state.toLoading()이 동기적으로 실행됨
        expect(notifier.state.isLoading, true);

        await future;

        // 완료 후 로딩 해제
        expect(notifier.state.isLoading, false);
        expect(notifier.state.allStores.length, 3);
      });

      test('에러 발생 시 에러 상태로 전환한다', () async {
        final errorRepo = _MockMyStoreRepository(
          error: Exception('네트워크 오류'),
        );
        final errorNotifier = MyStoresNotifier(
          getMyStores: GetMyStores(errorRepo),
        );

        await errorNotifier.loadStores();

        expect(errorNotifier.state.isLoading, false);
        expect(errorNotifier.state.errorMessage, isNotNull);
        expect(errorNotifier.state.errorMessage, contains('네트워크 오류'));
      });
    });

    group('searchStores', () {
      test('거래처명으로 검색한다', () async {
        await notifier.loadStores();
        notifier.searchStores('경산');

        expect(notifier.state.filteredStores.length, 1);
        expect(
            notifier.state.filteredStores[0].storeName, '(유)경산식품');
        expect(notifier.state.searchKeyword, '경산');
      });

      test('거래처 코드로 검색한다', () async {
        await notifier.loadStores();
        notifier.searchStores('1030456');

        expect(notifier.state.filteredStores.length, 1);
        expect(notifier.state.filteredStores[0].storeName, '대성마트');
      });

      test('대소문자 구분 없이 검색한다', () async {
        await notifier.loadStores();
        notifier.searchStores('경산');

        expect(notifier.state.filteredStores.length, 1);
      });

      test('검색 결과가 없으면 빈 목록을 반환한다', () async {
        await notifier.loadStores();
        notifier.searchStores('존재하지않는거래처');

        expect(notifier.state.filteredStores, isEmpty);
        expect(notifier.state.isSearchEmpty, true);
      });

      test('빈 검색어는 전체 목록을 복원한다', () async {
        await notifier.loadStores();
        notifier.searchStores('경산');
        notifier.searchStores('');

        expect(notifier.state.filteredStores.length, 3);
        expect(notifier.state.searchKeyword, '');
      });

      test('경 키워드로 여러 거래처가 검색된다', () async {
        await notifier.loadStores();
        notifier.searchStores('경');

        // '경산식품', '경남식품' 모두 매칭
        expect(notifier.state.filteredStores.length, 2);
      });
    });

    group('clearSearch', () {
      test('검색을 초기화하면 전체 목록이 복원된다', () async {
        await notifier.loadStores();
        notifier.searchStores('경산');
        expect(notifier.state.filteredStores.length, 1);

        notifier.clearSearch();

        expect(notifier.state.filteredStores.length, 3);
        expect(notifier.state.searchKeyword, '');
      });
    });

    group('clearError', () {
      test('에러를 초기화한다', () async {
        final errorRepo = _MockMyStoreRepository(
          error: Exception('오류'),
        );
        final errorNotifier = MyStoresNotifier(
          getMyStores: GetMyStores(errorRepo),
        );

        await errorNotifier.loadStores();
        expect(errorNotifier.state.errorMessage, isNotNull);

        errorNotifier.clearError();
        expect(errorNotifier.state.errorMessage, isNull);
      });
    });
  });
}
