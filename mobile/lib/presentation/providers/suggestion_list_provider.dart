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

  const SuggestionListState({
    this.items = const [],
    this.totalElements = 0,
    this.currentPage = 0,
    this.isLastPage = false,
    this.isLoading = false,
    this.isLoadingMore = false,
    this.hasLoaded = false,
    this.errorMessage,
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
    bool clearError = false,
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

  /// 첫 페이지 로드(새로고침)
  Future<void> load() async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final result = await _ref
          .read(suggestionRepositoryProvider)
          .getSuggestions(page: 0, size: _pageSize);
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
      final result = await _ref
          .read(suggestionRepositoryProvider)
          .getSuggestions(page: nextPage, size: _pageSize);
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
