import '../../domain/entities/shelf_life_item.dart';

/// 유통기한 관리 메인 화면 상태
class ShelfLifeListState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 유통기한 목록
  final List<ShelfLifeItem> items;

  /// 검색 완료 여부
  final bool hasSearched;

  /// 거래처 필터 (null이면 전체)
  final String? selectedAccountCode;

  /// 선택된 거래처명
  final String? selectedAccountName;

  /// 검색 시작일
  final DateTime fromDate;

  /// 검색 종료일
  final DateTime toDate;

  /// 내 거래처 목록 (드롭다운용) - {accountCode: accountName}
  final Map<String, String> stores;

  /// 거래처 목록 로딩 중 여부
  final bool isStoresLoading;

  const ShelfLifeListState({
    this.isLoading = false,
    this.errorMessage,
    this.items = const [],
    this.hasSearched = false,
    this.selectedAccountCode,
    this.selectedAccountName,
    required this.fromDate,
    required this.toDate,
    this.stores = const {},
    this.isStoresLoading = false,
  });

  /// 초기 상태 (시작: 오늘 - 7일, 종료: 오늘 + 3개월)
  factory ShelfLifeListState.initial() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    return ShelfLifeListState(
      fromDate: today.subtract(const Duration(days: 7)),
      toDate: DateTime(today.year, today.month + 3, today.day),
    );
  }

  /// 로딩 상태로 전환
  ShelfLifeListState toLoading() {
    return copyWith(isLoading: true, errorMessage: null);
  }

  /// 에러 상태로 전환
  ShelfLifeListState toError(String message) {
    return copyWith(isLoading: false, errorMessage: message);
  }

  /// 만료된 항목 (D-DAY <= 0)
  List<ShelfLifeItem> get expiredItems =>
      items.where((item) => item.isExpired).toList()
        ..sort((a, b) => a.dDay.compareTo(b.dDay));

  /// 만료 전 항목 (D-DAY > 0)
  List<ShelfLifeItem> get activeItems =>
      items.where((item) => !item.isExpired).toList()
        ..sort((a, b) => a.dDay.compareTo(b.dDay));

  /// 전체 항목 수
  int get totalCount => items.length;

  /// 검색 결과가 있는지
  bool get hasResults => items.isNotEmpty;

  /// 검색 결과가 비어있는지 (검색 후)
  bool get isEmpty => hasSearched && items.isEmpty;

  ShelfLifeListState copyWith({
    bool? isLoading,
    String? errorMessage,
    List<ShelfLifeItem>? items,
    bool? hasSearched,
    String? selectedAccountCode,
    String? selectedAccountName,
    DateTime? fromDate,
    DateTime? toDate,
    Map<String, String>? stores,
    bool? isStoresLoading,
    bool clearStoreFilter = false,
  }) {
    return ShelfLifeListState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      items: items ?? this.items,
      hasSearched: hasSearched ?? this.hasSearched,
      selectedAccountCode:
          clearStoreFilter ? null : (selectedAccountCode ?? this.selectedAccountCode),
      selectedAccountName:
          clearStoreFilter ? null : (selectedAccountName ?? this.selectedAccountName),
      fromDate: fromDate ?? this.fromDate,
      toDate: toDate ?? this.toDate,
      stores: stores ?? this.stores,
      isStoresLoading: isStoresLoading ?? this.isStoresLoading,
    );
  }
}
