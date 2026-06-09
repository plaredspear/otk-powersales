import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/my_account.dart';
import 'package:mobile/domain/repositories/my_account_repository.dart';
import 'package:mobile/domain/usecases/get_my_accounts.dart';
import 'package:mobile/presentation/providers/my_accounts_provider.dart';
import 'package:mobile/presentation/providers/my_accounts_state.dart';

/// 테스트용 Fake Repository
class _FakeMyAccountRepository implements MyAccountRepository {
  MyAccountListResult? resultToReturn;
  Exception? exceptionToThrow;
  String? lastKeyword;

  _FakeMyAccountRepository({this.resultToReturn, this.exceptionToThrow});

  @override
  Future<MyAccountListResult> getMyAccounts({
    String? keyword,
    MyAccountScope scope = MyAccountScope.field,
  }) async {
    await Future<void>.delayed(Duration.zero);
    lastKeyword = keyword;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return resultToReturn!;
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

    late _FakeMyAccountRepository fakeRepository;
    late MyAccountsNotifier notifier;

    setUp(() {
      fakeRepository = _FakeMyAccountRepository(resultToReturn: mockResult);
      final useCase = GetMyAccounts(fakeRepository);
      notifier = MyAccountsNotifier(getMyAccounts: useCase);
    });

    group('초기 상태', () {
      test('초기 상태가 올바르게 설정되어야 한다', () {
        expect(notifier.state.isLoading, false);
        expect(notifier.state.accounts, isEmpty);
        expect(notifier.state.searchKeyword, '');
        expect(notifier.state.totalCount, 0);
        expect(notifier.state.errorMessage, isNull);
      });
    });

    group('loadAccounts', () {
      test('거래처 목록을 성공적으로 로딩한다', () async {
        await notifier.loadAccounts();

        expect(notifier.state.isLoading, false);
        expect(notifier.state.accounts.length, 3);
        expect(notifier.state.totalCount, 3);
        expect(notifier.state.errorMessage, isNull);
      });

      test('로딩 중 상태를 거친다', () async {
        final future = notifier.loadAccounts();

        expect(notifier.state.isLoading, true);

        await future;

        expect(notifier.state.isLoading, false);
        expect(notifier.state.accounts.length, 3);
      });

      test('에러 발생 시 에러 상태로 전환한다', () async {
        final errorRepo = _FakeMyAccountRepository(
          exceptionToThrow: Exception('네트워크 오류'),
        );
        final errorNotifier = MyAccountsNotifier(
          getMyAccounts: GetMyAccounts(errorRepo),
        );

        await errorNotifier.loadAccounts();

        expect(errorNotifier.state.isLoading, false);
        expect(errorNotifier.state.errorMessage, isNotNull);
        expect(errorNotifier.state.errorMessage, contains('네트워크 오류'));
      });

      test('keyword를 전달하면 Repository에 keyword가 전달된다', () async {
        await notifier.loadAccounts(keyword: '경산');

        expect(fakeRepository.lastKeyword, '경산');
        expect(notifier.state.searchKeyword, '경산');
      });

      test('keyword 없이 호출하면 searchKeyword가 빈 문자열이 된다', () async {
        await notifier.loadAccounts(keyword: '경산');
        await notifier.loadAccounts();

        expect(fakeRepository.lastKeyword, isNull);
        expect(notifier.state.searchKeyword, '');
      });
    });

    group('clearSearch', () {
      test('검색을 초기화하면 keyword 없이 API를 재호출한다', () async {
        await notifier.loadAccounts(keyword: '경산');
        expect(notifier.state.searchKeyword, '경산');

        await notifier.clearSearch();

        expect(fakeRepository.lastKeyword, isNull);
        expect(notifier.state.searchKeyword, '');
        expect(notifier.state.accounts.length, 3);
      });
    });

    group('clearError', () {
      test('에러를 초기화한다', () async {
        final errorRepo = _FakeMyAccountRepository(
          exceptionToThrow: Exception('오류'),
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

    group('State computed getters', () {
      test('displayCount는 accounts 길이를 반환한다', () async {
        await notifier.loadAccounts();
        expect(notifier.state.displayCount, 3);
      });

      test('isSearchEmpty는 검색어가 있고 결과가 비었을 때 true', () {
        final emptyState = MyAccountsState(
          searchKeyword: '없는검색어',
          accounts: const [],
        );
        expect(emptyState.isSearchEmpty, true);
      });

      test('isAccountsEmpty는 로딩 완료 후 결과가 비었을 때 true', () {
        final emptyState = MyAccountsState(
          accounts: const [],
        );
        expect(emptyState.isAccountsEmpty, true);
      });

      test('isAccountsEmpty는 검색어가 있으면 false', () {
        final emptyState = MyAccountsState(
          searchKeyword: '검색어',
          accounts: const [],
        );
        expect(emptyState.isAccountsEmpty, false);
      });
    });
  });
}
