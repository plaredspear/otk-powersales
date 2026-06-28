import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/my_account.dart';
import 'package:mobile/domain/entities/my_account_meta.dart';
import 'package:mobile/domain/repositories/my_account_repository.dart';
import 'package:mobile/domain/usecases/get_my_accounts.dart';
import 'package:mobile/presentation/providers/my_accounts_provider.dart';
import 'package:mobile/presentation/widgets/account/account_selector_sheet.dart';

class _FakeMyAccountRepository implements MyAccountRepository {
  _FakeMyAccountRepository({this.meta});

  /// 서버 표시 기준 메타 (null 이면 미제공 — 클라이언트 폴백 경로).
  final MyAccountMeta? meta;

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
    return MyAccountListResult(
      accounts: filtered,
      totalCount: filtered.length,
      meta: meta,
    );
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

    testWidgets('includeAllOption=false 면 "거래처 전체" 항목이 없다', (tester) async {
      await openSheet(tester);

      expect(find.text('거래처 전체'), findsNothing);
    });

    testWidgets('includeAllOption=true 면 "거래처 전체" 선택 시 allOption 을 반환한다',
        (tester) async {
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
                    result = await AccountSelectorSheet.show(
                      context,
                      includeAllOption: true,
                    );
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

      // 최상단 "거래처 전체" + 거래처 목록이 함께 표시
      expect(find.text('거래처 전체'), findsOneWidget);
      expect(find.text('이마트 부산점'), findsOneWidget);

      await tester.tap(find.text('거래처 전체'));
      await tester.pumpAndSettle();

      expect(AccountSelectorSheet.isAllOption(result), isTrue);
    });

    testWidgets('표시 기준(ⓘ)은 서버 meta 문구를 그대로 보여준다', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            getMyAccountsUseCaseProvider.overrideWithValue(
              GetMyAccounts(
                _FakeMyAccountRepository(
                  meta: const MyAccountMeta(
                    criteriaLines: ['서버가 내려준 표시 기준'],
                    searchHint: '서버 검색 안내',
                  ),
                ),
              ),
            ),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: Builder(
                builder: (context) => ElevatedButton(
                  onPressed: () => AccountSelectorSheet.show(context),
                  child: const Text('open'),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.tap(find.text('open'));
      await tester.pumpAndSettle();

      // 표시 기준 안내(ⓘ) 탭
      await tester.tap(find.byIcon(Icons.info_outline));
      await tester.pumpAndSettle();

      expect(find.text('• 서버가 내려준 표시 기준'), findsOneWidget);
      expect(find.text('서버 검색 안내'), findsOneWidget);
    });
  });
}
