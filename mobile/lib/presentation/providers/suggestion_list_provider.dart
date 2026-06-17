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
      category: category ?? this.category,
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
  ///
  /// [category] 지정 시 해당 분류만 조회하며, 이후 새로고침/페이징에도 유지된다.
  /// 미지정 시 기존에 설정된 필터(state.category)를 그대로 사용한다.
  Future<void> load({String? category}) async {
    final activeCategory = category ?? state.category;
    state = state.copyWith(
      isLoading: true,
      category: activeCategory,
      clearError: true,
    );
    try {
      final result = await _ref
          .read(suggestionRepositoryProvider)
          .getSuggestions(page: 0, size: _pageSize, category: activeCategory);
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
          .getSuggestions(
            page: nextPage,
            size: _pageSize,
            category: state.category,
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
