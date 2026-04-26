import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/my_account_api_datasource.dart';
import '../../data/datasources/my_account_remote_datasource.dart';
import '../../data/repositories/my_account_repository_impl.dart';
import '../../domain/repositories/my_account_repository.dart';
import '../../domain/usecases/get_my_accounts.dart';
import 'my_accounts_state.dart';

// --- Dependency Providers ---

/// MyAccount Remote DataSource Provider
final myAccountRemoteDataSourceProvider =
    Provider<MyAccountRemoteDataSource>((ref) {
  final dio = ref.watch(dioProvider);
  return MyAccountApiDataSource(dio);
});

/// MyAccount Repository Provider
final myAccountRepositoryProvider = Provider<MyAccountRepository>((ref) {
  return MyAccountRepositoryImpl(
    remoteDataSource: ref.watch(myAccountRemoteDataSourceProvider),
  );
});

/// GetMyAccounts UseCase Provider
final getMyAccountsUseCaseProvider = Provider<GetMyAccounts>((ref) {
  final repository = ref.watch(myAccountRepositoryProvider);
  return GetMyAccounts(repository);
});

// --- MyAccountsNotifier ---

/// 내 거래처 상태 관리 Notifier
///
/// 거래처 목록 로딩, 서버 사이드 검색, 검색 초기화를 관리합니다.
class MyAccountsNotifier extends StateNotifier<MyAccountsState> {
  final GetMyAccounts _getMyAccounts;

  MyAccountsNotifier({
    required GetMyAccounts getMyAccounts,
  })  : _getMyAccounts = getMyAccounts,
        super(MyAccountsState.initial());

  /// 내 거래처 목록 로딩 (서버 사이드 검색 지원)
  Future<void> loadAccounts({String? keyword}) async {
    state = state.toLoading();

    try {
      final result = await _getMyAccounts.call(keyword: keyword);

      state = state.copyWith(
        isLoading: false,
        accounts: result.accounts,
        totalCount: result.totalCount,
        searchKeyword: keyword ?? '',
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 검색 초기화 (keyword 없이 API 재호출)
  Future<void> clearSearch() async {
    await loadAccounts();
  }

}

/// MyAccounts StateNotifier Provider
final myAccountsProvider =
    StateNotifierProvider<MyAccountsNotifier, MyAccountsState>((ref) {
  return MyAccountsNotifier(
    getMyAccounts: ref.watch(getMyAccountsUseCaseProvider),
  );
});
