import '../../domain/entities/inspection_list_item.dart';

/// 현장점검 목록 화면 상태
class InspectionListState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 현장점검 목록
  final List<InspectionListItem> items;

  /// 검색 완료 여부
  final bool hasSearched;

  /// 거래처 필터 (null이면 전체)
  final int? selectedStoreId;

  /// 선택된 거래처명
  final String? selectedStoreName;

  /// 분류 필터 (null이면 전체)
  final InspectionCategory? selectedCategory;

  /// 검색 시작일
  final DateTime fromDate;

  /// 검색 종료일
  final DateTime toDate;

  /// 내 거래처 목록 (드롭다운용) - {storeId: storeName}
  final Map<int, String> stores;

  const InspectionListState({
    this.isLoading = false,
    this.errorMessage,
    this.items = const [],
    this.hasSearched = false,
    this.selectedStoreId,
    this.selectedStoreName,
    this.selectedCategory,
    required this.fromDate,
    required this.toDate,
    this.stores = const {},
  });

  /// 초기 상태 (기본 필터: 오늘 기준 앞 7일 ~ 오늘)
  factory InspectionListState.initial() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    return InspectionListState(
      fromDate: today.subtract(const Duration(days: 7)),
      toDate: today,
    );
  }

  /// 로딩 상태로 전환
  InspectionListState toLoading() {
    return copyWith(isLoading: true, errorMessage: null);
  }

  /// 에러 상태로 전환
  InspectionListState toError(String message) {
    return copyWith(isLoading: false, errorMessage: message);
  }

  /// 자사 점검 목록
  List<InspectionListItem> get ownItems =>
      items.where((item) => item.category == InspectionCategory.OWN).toList();

  /// 경쟁사 점검 목록
  List<InspectionListItem> get competitorItems =>
      items.where((item) => item.category == InspectionCategory.COMPETITOR).toList();

  /// 전체 항목 수
  int get totalCount => items.length;

  /// 검색 결과가 있는지
  bool get hasResults => items.isNotEmpty;

  /// 검색 결과가 비어있는지 (검색 후)
  bool get isEmpty => hasSearched && items.isEmpty;

  InspectionListState copyWith({
    bool? isLoading,
    String? errorMessage,
    List<InspectionListItem>? items,
    bool? hasSearched,
    int? selectedStoreId,
    String? selectedStoreName,
    InspectionCategory? selectedCategory,
    DateTime? fromDate,
    DateTime? toDate,
    Map<int, String>? stores,
    bool clearStoreFilter = false,
    bool clearCategoryFilter = false,
  }) {
    return InspectionListState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      items: items ?? this.items,
      hasSearched: hasSearched ?? this.hasSearched,
      selectedStoreId:
          clearStoreFilter ? null : (selectedStoreId ?? this.selectedStoreId),
      selectedStoreName:
          clearStoreFilter ? null : (selectedStoreName ?? this.selectedStoreName),
      selectedCategory:
          clearCategoryFilter ? null : (selectedCategory ?? this.selectedCategory),
      fromDate: fromDate ?? this.fromDate,
      toDate: toDate ?? this.toDate,
      stores: stores ?? this.stores,
    );
  }
}
