import '../../domain/entities/my_account.dart';

/// 내 거래처 화면 상태
class MyAccountsState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 거래처 목록 (API 결과)
  final List<MyAccount> accounts;

  /// 현재 검색어
  final String searchKeyword;

  /// 총 거래처 수
  final int totalCount;

  const MyAccountsState({
    this.isLoading = false,
    this.errorMessage,
    this.accounts = const [],
    this.searchKeyword = '',
    this.totalCount = 0,
  });

  /// 초기 상태
  factory MyAccountsState.initial() {
    return const MyAccountsState();
  }

  /// 로딩 상태로 전환
  MyAccountsState toLoading() {
    return copyWith(isLoading: true, errorMessage: null);
  }

  /// 에러 상태로 전환
  MyAccountsState toError(String message) {
    return copyWith(
      isLoading: false,
      errorMessage: message,
    );
  }

  /// 표시할 거래처 수
  int get displayCount => accounts.length;

  /// 검색 결과가 없는지 (검색 후)
  bool get isSearchEmpty =>
      searchKeyword.isNotEmpty && accounts.isEmpty && !isLoading;

  /// 거래처 목록이 비어있는지 (API 결과 자체가 빈 경우)
  bool get isAccountsEmpty =>
      !isLoading && accounts.isEmpty && errorMessage == null && searchKeyword.isEmpty;

  MyAccountsState copyWith({
    bool? isLoading,
    String? errorMessage,
    List<MyAccount>? accounts,
    String? searchKeyword,
    int? totalCount,
  }) {
    return MyAccountsState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      accounts: accounts ?? this.accounts,
      searchKeyword: searchKeyword ?? this.searchKeyword,
      totalCount: totalCount ?? this.totalCount,
    );
  }
}
