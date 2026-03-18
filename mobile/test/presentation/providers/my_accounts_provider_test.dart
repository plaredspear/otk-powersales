import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/my_account.dart';
import 'package:mobile/domain/repositories/my_account_repository.dart';
import 'package:mobile/domain/usecases/get_my_accounts.dart';
import 'package:mobile/presentation/providers/my_accounts_provider.dart';
import 'package:mobile/presentation/providers/my_accounts_state.dart';

/// 테스트용 Mock Repository
class _MockMyAccountRepository implements MyAccountRepository {
  final MyAccountListResult? _result;
  final Exception? _error;

  _MockMyAccountRepository({MyAccountListResult? result, Exception? error})
      : _result = result,
        _error = error;

  @override
  Future<MyAccountListResult> getMyAccounts() async {
    // 비동기 딜레이를 추가하여 로딩 상태를 테스트에서 캡처할 수 있게 함
    await Future<void>.delayed(Duration.zero);
    if (_error != null) throw _error;
    return _result!;
  }
}

void main() {
  group('MyAccountsNotifier', () {
    const mockAccounts = [
      MyAccount(
        accountId: 1,
        accountName: '(유)경산식품',
        accountCode: '1025172',
        address: '전라남도 목포시',
        representativeName: '김정자',
        phoneNumber: '061-123-4567',
      ),
      MyAccount(
        accountId: 2,
        accountName: '대성마트',
        accountCode: '1030456',
        address: '광주광역시 서구',
        representativeName: '이대성',
        phoneNumber: '062-345-6789',
      ),
      MyAccount(
        accountId: 3,
        accountName: '경남식품',
        accountCode: '1035403',
        address: '경남 창원시',
        representativeName: '윤경남',
      ),
    ];

    final mockResult = MyAccountListResult(
      accounts: mockAccounts,
      totalCount: mockAccounts.length,
    );

    late MyAccountsNotifier notifier;

    setUp(() {
      final repository = _MockMyAccountRepository(result: mockResult);
      final useCase = GetMyAccounts(repository);
      notifier = MyAccountsNotifier(getMyAccounts: useCase);
    });

    group('loadAccounts', () {
      test('거래처 목록을 성공적으로 로딩한다', () async {
        await notifier.loadAccounts();

        expect(notifier.state.isLoading, false);
        expect(notifier.state.allAccounts.length, 3);
        expect(notifier.state.filteredAccounts.length, 3);
        expect(notifier.state.totalCount, 3);
        expect(notifier.state.errorMessage, isNull);
      });

      test('로딩 중 상태를 거친다', () async {
        // loadAccounts 호출 후 await 전에 로딩 상태를 확인
        final future = notifier.loadAccounts();

        // loadAccounts 내부에서 state = state.toLoading()이 동기적으로 실행됨
        expect(notifier.state.isLoading, true);

        await future;

        // 완료 후 로딩 해제
        expect(notifier.state.isLoading, false);
        expect(notifier.state.allAccounts.length, 3);
      });

      test('에러 발생 시 에러 상태로 전환한다', () async {
        final errorRepo = _MockMyAccountRepository(
          error: Exception('네트워크 오류'),
        );
        final errorNotifier = MyAccountsNotifier(
          getMyAccounts: GetMyAccounts(errorRepo),
        );

        await errorNotifier.loadAccounts();

        expect(errorNotifier.state.isLoading, false);
        expect(errorNotifier.state.errorMessage, isNotNull);
        expect(errorNotifier.state.errorMessage, contains('네트워크 오류'));
      });
    });

    group('searchAccounts', () {
      test('거래처명으로 검색한다', () async {
        await notifier.loadAccounts();
        notifier.searchAccounts('경산');

        expect(notifier.state.filteredAccounts.length, 1);
        expect(
            notifier.state.filteredAccounts[0].accountName, '(유)경산식품');
        expect(notifier.state.searchKeyword, '경산');
      });

      test('거래처 코드로 검색한다', () async {
        await notifier.loadAccounts();
        notifier.searchAccounts('1030456');

        expect(notifier.state.filteredAccounts.length, 1);
        expect(notifier.state.filteredAccounts[0].accountName, '대성마트');
      });

      test('대소문자 구분 없이 검색한다', () async {
        await notifier.loadAccounts();
        notifier.searchAccounts('경산');

        expect(notifier.state.filteredAccounts.length, 1);
      });

      test('검색 결과가 없으면 빈 목록을 반환한다', () async {
        await notifier.loadAccounts();
        notifier.searchAccounts('존재하지않는거래처');

        expect(notifier.state.filteredAccounts, isEmpty);
        expect(notifier.state.isSearchEmpty, true);
      });

      test('빈 검색어는 전체 목록을 복원한다', () async {
        await notifier.loadAccounts();
        notifier.searchAccounts('경산');
        notifier.searchAccounts('');

        expect(notifier.state.filteredAccounts.length, 3);
        expect(notifier.state.searchKeyword, '');
      });

      test('경 키워드로 여러 거래처가 검색된다', () async {
        await notifier.loadAccounts();
        notifier.searchAccounts('경');

        // '경산식품', '경남식품' 모두 매칭
        expect(notifier.state.filteredAccounts.length, 2);
      });
    });

    group('clearSearch', () {
      test('검색을 초기화하면 전체 목록이 복원된다', () async {
        await notifier.loadAccounts();
        notifier.searchAccounts('경산');
        expect(notifier.state.filteredAccounts.length, 1);

        notifier.clearSearch();

        expect(notifier.state.filteredAccounts.length, 3);
        expect(notifier.state.searchKeyword, '');
      });
    });

    group('clearError', () {
      test('에러를 초기화한다', () async {
        final errorRepo = _MockMyAccountRepository(
          error: Exception('오류'),
        );
        final errorNotifier = MyAccountsNotifier(
          getMyAccounts: GetMyAccounts(errorRepo),
        );

        await errorNotifier.loadAccounts();
        expect(errorNotifier.state.errorMessage, isNotNull);

        errorNotifier.clearError();
        expect(errorNotifier.state.errorMessage, isNull);
      });
    });
  });
}
