import '../../domain/entities/promotion.dart';

/// 행사 목록 화면 상태
class PromotionListState {
  final bool isLoading;
  final bool isLoadingMore;
  final String? errorMessage;
  final List<PromotionItem> items;
  final int totalElements;
  final int totalPages;
  final int currentPage;
  final bool isLastPage;
  final bool hasSearched;

  /// 기간 필터
  final String startDate;
  final String endDate;

  /// 검색어
  final String keyword;

  const PromotionListState({
    this.isLoading = false,
    this.isLoadingMore = false,
    this.errorMessage,
    this.items = const [],
    this.totalElements = 0,
    this.totalPages = 0,
    this.currentPage = 0,
    this.isLastPage = false,
    this.hasSearched = false,
    required this.startDate,
    required this.endDate,
    this.keyword = '',
  });

  /// 초기 상태 (당월 1일 ~ 말일)
  factory PromotionListState.initial() {
    final now = DateTime.now();
    final firstDay = DateTime(now.year, now.month, 1);
    final lastDay = DateTime(now.year, now.month + 1, 0);

    return PromotionListState(
      startDate: _formatDate(firstDay),
      endDate: _formatDate(lastDay),
    );
  }

  static String _formatDate(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }

  PromotionListState toLoading() => copyWith(
        isLoading: true,
        isLoadingMore: false,
        errorMessage: null,
      );

  PromotionListState toLoadingMore() => copyWith(
        isLoadingMore: true,
        errorMessage: null,
      );

  PromotionListState toError(String message) => copyWith(
        isLoading: false,
        isLoadingMore: false,
        errorMessage: message,
      );

  bool get isEmpty => hasSearched && items.isEmpty;
  bool get hasNextPage => !isLastPage;

  PromotionListState copyWith({
    bool? isLoading,
    bool? isLoadingMore,
    String? errorMessage,
    List<PromotionItem>? items,
    int? totalElements,
    int? totalPages,
    int? currentPage,
    bool? isLastPage,
    bool? hasSearched,
    String? startDate,
    String? endDate,
    String? keyword,
  }) {
    return PromotionListState(
      isLoading: isLoading ?? this.isLoading,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
      errorMessage: errorMessage,
      items: items ?? this.items,
      totalElements: totalElements ?? this.totalElements,
      totalPages: totalPages ?? this.totalPages,
      currentPage: currentPage ?? this.currentPage,
      isLastPage: isLastPage ?? this.isLastPage,
      hasSearched: hasSearched ?? this.hasSearched,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      keyword: keyword ?? this.keyword,
    );
  }
}
