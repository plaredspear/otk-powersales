import '../../domain/entities/claim_list_item.dart';

/// 클레임 목록 상태
class ClaimListState {
  /// 조회 기간 최대 일수 (레거시 daterangepicker maxSpan.days=7 동등).
  static const int maxRangeDays = 7;

  final bool isLoading;
  final String? errorMessage;
  final List<ClaimListItem> items;
  final bool hasSearched;
  final DateTime startDate;
  final DateTime endDate;
  final int? selectedAccountId;
  final String? selectedAccountName;

  const ClaimListState({
    this.isLoading = false,
    this.errorMessage,
    this.items = const [],
    this.hasSearched = false,
    required this.startDate,
    required this.endDate,
    this.selectedAccountId,
    this.selectedAccountName,
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
    );
  }
}
