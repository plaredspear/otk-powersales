import '../../domain/entities/my_account.dart';

/// 내 거래처 화면 상태
class MyAccountsState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 전체 거래처 목록 (필터 전)
  final List<MyAccount> allAccounts;

  /// 검색 결과 거래처 목록
  final List<MyAccount> filteredAccounts;

  /// 현재 검색어
  final String searchKeyword;

  /// 총 거래처 수 (= allAccounts.length)
  final int totalCount;

  const MyAccountsState({
    this.isLoading = false,
    this.errorMessage,
    this.allAccounts = const [],
    this.filteredAccounts = const [],
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

  /// 표시할 거래처 수 (검색 전: 전체, 검색 후: 필터 결과)
  int get displayCount => filteredAccounts.length;

  /// 검색 결과가 없는지 (검색 후)
  bool get isSearchEmpty =>
      searchKeyword.isNotEmpty && filteredAccounts.isEmpty;

  /// 거래처 목록이 비어있는지 (API 결과 자체가 빈 경우)
  bool get isAccountsEmpty =>
      !isLoading && allAccounts.isEmpty && errorMessage == null;

  MyAccountsState copyWith({
    bool? isLoading,
    String? errorMessage,
    List<MyAccount>? allAccounts,
    List<MyAccount>? filteredAccounts,
    String? searchKeyword,
    int? totalCount,
  }) {
    return MyAccountsState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      allAccounts: allAccounts ?? this.allAccounts,
      filteredAccounts: filteredAccounts ?? this.filteredAccounts,
      searchKeyword: searchKeyword ?? this.searchKeyword,
      totalCount: totalCount ?? this.totalCount,
    );
  }
}
