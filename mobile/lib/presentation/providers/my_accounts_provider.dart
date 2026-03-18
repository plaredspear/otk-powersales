import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/mock/my_account_mock_repository.dart';
import '../../domain/repositories/my_account_repository.dart';
import '../../domain/usecases/get_my_accounts.dart';
import 'my_accounts_state.dart';

// --- Dependency Providers ---

/// MyAccount Repository Provider
final myAccountRepositoryProvider = Provider<MyAccountRepository>((ref) {
  return MyAccountMockRepository();
});

/// GetMyAccounts UseCase Provider
final getMyAccountsUseCaseProvider = Provider<GetMyAccounts>((ref) {
  final repository = ref.watch(myAccountRepositoryProvider);
  return GetMyAccounts(repository);
});

// --- MyAccountsNotifier ---

/// 내 거래처 상태 관리 Notifier
///
/// 거래처 목록 로딩, 클라이언트 사이드 검색, 검색 초기화를 관리합니다.
class MyAccountsNotifier extends StateNotifier<MyAccountsState> {
  final GetMyAccounts _getMyAccounts;

  MyAccountsNotifier({
    required GetMyAccounts getMyAccounts,
  })  : _getMyAccounts = getMyAccounts,
        super(MyAccountsState.initial());

  /// 내 거래처 목록 로딩
  Future<void> loadAccounts() async {
    state = state.toLoading();

    try {
      final result = await _getMyAccounts.call();

      state = state.copyWith(
        isLoading: false,
        allAccounts: result.accounts,
        filteredAccounts: result.accounts,
        totalCount: result.totalCount,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 거래처 검색 (클라이언트 사이드)
  ///
  /// 거래처명 또는 거래처 코드로 필터링합니다.
  void searchAccounts(String keyword) {
    if (keyword.isEmpty) {
      state = state.copyWith(
        searchKeyword: '',
        filteredAccounts: state.allAccounts,
      );
      return;
    }

    final lowerKeyword = keyword.toLowerCase();
    final filtered = state.allAccounts.where((store) {
      return store.accountName.toLowerCase().contains(lowerKeyword) ||
          store.accountCode.toLowerCase().contains(lowerKeyword);
    }).toList();

    state = state.copyWith(
      searchKeyword: keyword,
      filteredAccounts: filtered,
    );
  }

  /// 검색 초기화
  void clearSearch() {
    state = state.copyWith(
      searchKeyword: '',
      filteredAccounts: state.allAccounts,
    );
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

/// MyAccounts StateNotifier Provider
final myAccountsProvider =
    StateNotifierProvider<MyAccountsNotifier, MyAccountsState>((ref) {
  return MyAccountsNotifier(
    getMyAccounts: ref.watch(getMyAccountsUseCaseProvider),
  );
});
