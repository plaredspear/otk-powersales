import '../../domain/entities/claim_list_item.dart';

/// 클레임 목록 상태
class ClaimListState {
  /// 조회 기간 최대 일수 (레거시 daterangepicker maxSpan.days=7 동등).
  static const int maxRangeDays = 7;

  /// 페이지당 표시 건수 (레거시 list.jsp claimListAppend 의 20건/페이지 동등).
  static const int pageSize = 20;

  final bool isLoading;
  final String? errorMessage;
  final List<ClaimListItem> items;
  final bool hasSearched;
  final DateTime startDate;
  final DateTime endDate;
  final int? selectedAccountId;
  final String? selectedAccountName;

  /// 현재 화면에 노출 중인 건수 (클라이언트 페이징 — 전체 items 중 앞에서부터 이만큼만 표시).
  final int visibleCount;

  const ClaimListState({
    this.isLoading = false,
    this.errorMessage,
    this.items = const [],
    this.hasSearched = false,
    required this.startDate,
    required this.endDate,
    this.selectedAccountId,
    this.selectedAccountName,
    this.visibleCount = pageSize,
  });

  factory ClaimListState.initial() {
    final today = DateTime(
        DateTime.now().year, DateTime.now().month, DateTime.now().day);
    return ClaimListState(
      startDate: today.subtract(const Duration(days: 7)),
      endDate: today,
    );
  }

  ClaimListState toLoading() => copyWith(isLoading: true, clearErrorMessage: true);

  ClaimListState toError(String msg) =>
      copyWith(isLoading: false, errorMessage: msg);

  bool get isEmpty => hasSearched && items.isEmpty;

  /// 현재 페이지까지 노출할 아이템 (전체 items 의 앞부분 slice).
  List<ClaimListItem> get visibleItems =>
      items.length <= visibleCount ? items : items.sublist(0, visibleCount);

  /// 더 보여줄 다음 페이지가 남아있는지.
  bool get hasMore => items.length > visibleCount;

  ClaimListState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearErrorMessage = false,
    List<ClaimListItem>? items,
    bool? hasSearched,
    DateTime? startDate,
    DateTime? endDate,
    int? selectedAccountId,
    String? selectedAccountName,
    bool clearAccount = false,
    int? visibleCount,
  }) {
    return ClaimListState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearErrorMessage ? null : (errorMessage ?? this.errorMessage),
      items: items ?? this.items,
      hasSearched: hasSearched ?? this.hasSearched,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      selectedAccountId:
          clearAccount ? null : (selectedAccountId ?? this.selectedAccountId),
      selectedAccountName: clearAccount
          ? null
          : (selectedAccountName ?? this.selectedAccountName),
      visibleCount: visibleCount ?? this.visibleCount,
    );
  }
}
