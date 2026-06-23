import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/utils/error_utils.dart';
import '../../domain/entities/suggestion_detail.dart';
import '../../domain/entities/suggestion_list_item.dart';
import 'suggestion_register_provider.dart';

// ============================================
// 1. State
// ============================================
// Repository/DataSource Provider 는 suggestion_register_provider.dart 의
// suggestionRepositoryProvider 를 재사용한다.

/// 내 제안/물류클레임 목록 상태
class SuggestionListState {
  final List<SuggestionListItem> items;
  final int totalElements;
  final int currentPage;
  final bool isLastPage;
  final bool isLoading;
  final bool isLoadingMore;
  final bool hasLoaded;
  final String? errorMessage;

  /// 분류 필터(예: 'LOGISTICS_CLAIM' = 물류클레임 전용). null 이면 전체.
  final String? category;

  /// 거래처 필터(레거시 SAPAccountCode). null 이면 거래처 전체.
  final int? selectedAccountId;
  final String? selectedAccountName;

  /// 등록일 범위 필터(레거시 ClaimDateStart/End → CreatedDate). null 이면 미적용(전체).
  final DateTime? startDate;
  final DateTime? endDate;

  const SuggestionListState({
    this.items = const [],
    this.totalElements = 0,
    this.currentPage = 0,
    this.isLastPage = false,
    this.isLoading = false,
    this.isLoadingMore = false,
    this.hasLoaded = false,
    this.errorMessage,
    this.category,
    this.selectedAccountId,
    this.selectedAccountName,
    this.startDate,
    this.endDate,
  });

  bool get isEmpty => hasLoaded && items.isEmpty && !isLoading;

  SuggestionListState copyWith({
    List<SuggestionListItem>? items,
    int? totalElements,
    int? currentPage,
    bool? isLastPage,
    bool? isLoading,
    bool? isLoadingMore,
    bool? hasLoaded,
    String? errorMessage,
    String? category,
    int? selectedAccountId,
    String? selectedAccountName,
    DateTime? startDate,
    DateTime? endDate,
    bool clearError = false,
    bool clearAccount = false,
    bool clearDates = false,
  }) {
    return SuggestionListState(
      items: items ?? this.items,
      totalElements: totalElements ?? this.totalElements,
      currentPage: currentPage ?? this.currentPage,
      isLastPage: isLastPage ?? this.isLastPage,
      isLoading: isLoading ?? this.isLoading,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
      hasLoaded: hasLoaded ?? this.hasLoaded,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      category: category ?? this.category,
      selectedAccountId:
          clearAccount ? null : (selectedAccountId ?? this.selectedAccountId),
      selectedAccountName: clearAccount
          ? null
          : (selectedAccountName ?? this.selectedAccountName),
      startDate: clearDates ? null : (startDate ?? this.startDate),
      endDate: clearDates ? null : (endDate ?? this.endDate),
    );
  }
}

// ============================================
// 2. StateNotifier Implementation
// ============================================

/// 제안/물류클레임 목록 Notifier (페이징 + 무한 스크롤)
class SuggestionListNotifier extends StateNotifier<SuggestionListState> {
  SuggestionListNotifier(this._ref) : super(const SuggestionListState());

  final Ref _ref;
  static const int _pageSize = 20;

  /// 레거시 logisticsclaimlist 기본 등록일 범위 = 최근 30일(오늘-30 ~ 오늘).
  static const int _defaultRangeDays = 30;

  String? _fmtDate(DateTime? date) {
    if (date == null) return null;
    return '${date.year.toString().padLeft(4, '0')}-'
        '${date.month.toString().padLeft(2, '0')}-'
        '${date.day.toString().padLeft(2, '0')}';
  }

  /// 화면 진입 시 검색조건 초기화(레거시는 페이지 로드마다 기본값으로 시작).
  ///
  /// 레거시 logisticsclaimlist 정합: 진입 유형(물류클레임 전용 / 제안·물류클레임 통합)과
  /// 무관하게 거래처 전체 + 등록일 범위 기본값(최근 30일)으로 초기화한다.
  /// 전역 provider 를 두 진입이 공유하므로, 매 진입마다 리셋해 이전 진입의 필터 누수를 막고
  /// 필터 UI 가 항상 유효한 날짜 범위를 갖게 한다.
  void initFilters() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    state = state.copyWith(
      clearAccount: true,
      startDate: today.subtract(const Duration(days: _defaultRangeDays)),
      endDate: today,
    );
  }

  /// 거래처 필터 선택
  void selectAccount(int accountId, String accountName) {
    state = state.copyWith(
      selectedAccountId: accountId,
      selectedAccountName: accountName,
    );
  }

  /// 거래처 필터 해제(거래처 전체)
  void clearAccount() {
    state = state.copyWith(clearAccount: true);
  }

  /// 등록일 범위 변경(검색 버튼 누르기 전까지는 조회하지 않음 — 레거시 동등)
  void updateDateRange(DateTime start, DateTime end) {
    state = state.copyWith(startDate: start, endDate: end);
  }

  /// 첫 페이지 로드(새로고침)
  ///
  /// [category] 지정 시 해당 분류만 조회하며, 이후 새로고침/페이징에도 유지된다.
  /// 미지정 시 기존에 설정된 필터(state.category)를 그대로 사용한다.
  /// 거래처/등록일 범위 필터는 state 에 설정된 값을 함께 전송한다.
  Future<void> load({String? category}) async {
    final activeCategory = category ?? state.category;
    state = state.copyWith(
      isLoading: true,
      category: activeCategory,
      clearError: true,
    );
    try {
      final result = await _ref.read(suggestionRepositoryProvider).getSuggestions(
            page: 0,
            size: _pageSize,
            category: activeCategory,
            accountId: state.selectedAccountId,
            startDate: _fmtDate(state.startDate),
            endDate: _fmtDate(state.endDate),
          );
      state = state.copyWith(
        items: result.items,
        totalElements: result.totalElements,
        currentPage: result.currentPage,
        isLastPage: result.isLast,
        isLoading: false,
        hasLoaded: true,
        clearError: true,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        hasLoaded: true,
        errorMessage: extractErrorMessage(e),
      );
    }
  }

  /// 다음 페이지 로드(무한 스크롤)
  Future<void> loadNextPage() async {
    if (state.isLoading || state.isLoadingMore || state.isLastPage) return;

    state = state.copyWith(isLoadingMore: true);
    try {
      final nextPage = state.currentPage + 1;
      final result = await _ref.read(suggestionRepositoryProvider).getSuggestions(
            page: nextPage,
            size: _pageSize,
            category: state.category,
            accountId: state.selectedAccountId,
            startDate: _fmtDate(state.startDate),
            endDate: _fmtDate(state.endDate),
          );
      state = state.copyWith(
        items: [...state.items, ...result.items],
        totalElements: result.totalElements,
        currentPage: result.currentPage,
        isLastPage: result.isLast,
        isLoadingMore: false,
        clearError: true,
      );
    } catch (e) {
      state = state.copyWith(
        isLoadingMore: false,
        errorMessage: extractErrorMessage(e),
      );
    }
  }
}

// ============================================
// 3. Providers
// ============================================

/// 제안/물류클레임 목록 Provider
final suggestionListProvider =
    StateNotifierProvider<SuggestionListNotifier, SuggestionListState>((ref) {
  return SuggestionListNotifier(ref);
});

/// 제안/물류클레임 상세 Provider (Family)
final suggestionDetailProvider =
    FutureProvider.family<SuggestionDetail, int>((ref, suggestionId) async {
  return ref
      .read(suggestionRepositoryProvider)
      .getSuggestionDetail(suggestionId);
});
