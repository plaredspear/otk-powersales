import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/promotion_api_datasource.dart';
import '../../data/repositories/promotion_repository_impl.dart';
import '../../domain/entities/promotion.dart';
import '../../domain/repositories/promotion_repository.dart';
import 'promotion_list_state.dart';

// ============================================
// 1. Dependency Providers
// ============================================

final promotionRepositoryProvider = Provider<PromotionRepository>((ref) {
  final dio = ref.watch(dioProvider);
  final remoteDataSource = PromotionApiDataSource(dio);
  return PromotionRepositoryImpl(remoteDataSource: remoteDataSource);
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

class PromotionListNotifier extends StateNotifier<PromotionListState> {
  final PromotionRepository _repository;

  PromotionListNotifier({required PromotionRepository repository})
      : _repository = repository,
        super(PromotionListState.initial());

  /// 초기 로드
  Future<void> initialize() async {
    await searchPromotions();
  }

  /// 행사 목록 검색 (첫 페이지)
  Future<void> searchPromotions() async {
    state = state.toLoading();

    try {
      final result = await _repository.getPromotions(
        startDate: state.startDate,
        endDate: state.endDate,
        keyword: state.keyword.isEmpty ? null : state.keyword,
        accountId: state.accountId,
        page: 0,
      );

      state = state.copyWith(
        isLoading: false,
        items: result.items,
        totalElements: result.totalElements,
        totalPages: result.totalPages,
        currentPage: 0,
        isLastPage: result.isLast,
        hasSearched: true,
      );
    } catch (e) {
      state = state.toError(extractErrorMessage(e));
    }
  }

  /// 다음 페이지 로드 (무한 스크롤)
  Future<void> loadNextPage() async {
    if (state.isLoading || state.isLoadingMore || state.isLastPage) return;

    state = state.toLoadingMore();

    try {
      final nextPage = state.currentPage + 1;
      final result = await _repository.getPromotions(
        startDate: state.startDate,
        endDate: state.endDate,
        keyword: state.keyword.isEmpty ? null : state.keyword,
        accountId: state.accountId,
        page: nextPage,
      );

      state = state.copyWith(
        isLoadingMore: false,
        items: [...state.items, ...result.items],
        totalElements: result.totalElements,
        totalPages: result.totalPages,
        currentPage: nextPage,
        isLastPage: result.isLast,
      );
    } catch (e) {
      state = state.toError(extractErrorMessage(e));
    }
  }

  /// 기간 필터 변경 (조장/지점장: 기간 범위)
  void updateDateRange(String startDate, String endDate) {
    state = state.copyWith(startDate: startDate, endDate: endDate);
  }

  /// 단일 날짜 변경 (여사원: 레거시 singleDatePicker — start == end)
  void updateSingleDate(String date) {
    state = state.copyWith(startDate: date, endDate: date);
  }

  /// 거래처 필터 변경 (null = 거래처 전체)
  void updateAccount(int? accountId, String? accountName) {
    state = state.copyWith(
      accountId: accountId,
      accountName: accountName,
      clearAccount: accountId == null,
    );
  }

  /// 검색어 변경
  void updateKeyword(String keyword) {
    state = state.copyWith(keyword: keyword);
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

// ============================================
// 3. StateNotifier Provider
// ============================================

final promotionListProvider =
    StateNotifierProvider<PromotionListNotifier, PromotionListState>((ref) {
  final repository = ref.watch(promotionRepositoryProvider);
  return PromotionListNotifier(repository: repository);
});

// ============================================
// 4. 일 매출 등록 진입 — 오늘 담당 행사 목록
// ============================================

/// 홈 "행사매출 등록" → 일 매출 등록 진입화면의 "담당 행사 선택" 목록.
///
/// 서버 오늘 날짜 기준(레거시 `eventlistapi` StartDate=EndDate=today 동등).
final myPromotionAssignmentsProvider =
    FutureProvider.autoDispose<List<MyPromotionAssignment>>((ref) async {
  final repository = ref.watch(promotionRepositoryProvider);
  return repository.getMyAssignments();
});
