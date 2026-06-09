import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/my_account.dart';
import 'package:mobile/domain/repositories/my_account_repository.dart';
import 'package:mobile/domain/usecases/get_my_accounts.dart';
import 'package:mobile/presentation/providers/my_accounts_provider.dart';
import 'package:mobile/presentation/widgets/account/account_selector_sheet.dart';

class _FakeMyAccountRepository implements MyAccountRepository {
  static const _all = [
    MyAccount(accountId: 1, accountName: '이마트 부산점', accountCode: '00001'),
    MyAccount(accountId: 2, accountName: '홈플러스 해운대점', accountCode: '00002'),
  ];

  @override
  Future<MyAccountListResult> getMyAccounts({
    String? keyword,
    MyAccountScope scope = MyAccountScope.field,
  }) async {
    final filtered = keyword == null
        ? _all
        : _all
            .where((a) =>
                a.accountName.contains(keyword) ||
                a.accountCode.contains(keyword))
            .toList();
    return MyAccountListResult(accounts: filtered, totalCount: filtered.length);
  }
}

void main() {
  Future<MyAccount?> openSheet(WidgetTester tester) async {
    MyAccount? selected;
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          getMyAccountsUseCaseProvider
              .overrideWithValue(GetMyAccounts(_FakeMyAccountRepository())),
        ],
        child: MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () async {
                  selected = await AccountSelectorSheet.show(context);
                },
                child: const Text('open'),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
    return selected;
  }

  group('AccountSelectorSheet', () {
    testWidgets('내 거래처 목록을 표시한다', (tester) async {
      await openSheet(tester);

      expect(find.text('거래처 선택'), findsOneWidget);
      expect(find.text('이마트 부산점'), findsOneWidget);
      expect(find.text('홈플러스 해운대점'), findsOneWidget);
    });

    testWidgets('거래처를 탭하면 선택된 거래처를 반환한다', (tester) async {
      MyAccount? result;
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            getMyAccountsUseCaseProvider
                .overrideWithValue(GetMyAccounts(_FakeMyAccountRepository())),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: Builder(
                builder: (context) => ElevatedButton(
                  onPressed: () async {
                    result = await AccountSelectorSheet.show(context);
                  },
                  child: const Text('open'),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.tap(find.text('open'));
      await tester.pumpAndSettle();

      await tester.tap(find.text('이마트 부산점'));
      await tester.pumpAndSettle();

      expect(result, isNotNull);
      expect(result!.accountId, 1);
      expect(result!.accountName, '이마트 부산점');
    });
  });
}
