import '../../domain/entities/my_store.dart';

/// 내 거래처 화면 상태
class MyStoresState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 전체 거래처 목록 (필터 전)
  final List<MyStore> allStores;

  /// 검색 결과 거래처 목록
  final List<MyStore> filteredStores;

  /// 현재 검색어
  final String searchKeyword;

  /// 총 거래처 수 (= allStores.length)
  final int totalCount;

  const MyStoresState({
    this.isLoading = false,
    this.errorMessage,
    this.allStores = const [],
    this.filteredStores = const [],
    this.searchKeyword = '',
    this.totalCount = 0,
  });

  /// 초기 상태
  factory MyStoresState.initial() {
    return const MyStoresState();
  }

  /// 로딩 상태로 전환
  MyStoresState toLoading() {
    return copyWith(isLoading: true, errorMessage: null);
  }

  /// 에러 상태로 전환
  MyStoresState toError(String message) {
    return copyWith(
      isLoading: false,
      errorMessage: message,
    );
  }

  /// 표시할 거래처 수 (검색 전: 전체, 검색 후: 필터 결과)
  int get displayCount => filteredStores.length;

  /// 검색 결과가 없는지 (검색 후)
  bool get isSearchEmpty =>
      searchKeyword.isNotEmpty && filteredStores.isEmpty;

  /// 거래처 목록이 비어있는지 (API 결과 자체가 빈 경우)
  bool get isStoresEmpty =>
      !isLoading && allStores.isEmpty && errorMessage == null;

  MyStoresState copyWith({
    bool? isLoading,
    String? errorMessage,
    List<MyStore>? allStores,
    List<MyStore>? filteredStores,
    String? searchKeyword,
    int? totalCount,
  }) {
    return MyStoresState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      allStores: allStores ?? this.allStores,
      filteredStores: filteredStores ?? this.filteredStores,
      searchKeyword: searchKeyword ?? this.searchKeyword,
      totalCount: totalCount ?? this.totalCount,
    );
  }
}
