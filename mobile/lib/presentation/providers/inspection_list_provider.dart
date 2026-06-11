import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../data/datasources/inspection_api_datasource.dart';
import '../../data/repositories/inspection_repository_impl.dart';
import '../../domain/entities/inspection_list_item.dart';
import '../../domain/repositories/inspection_repository.dart';
import '../../domain/usecases/get_inspection_list_usecase.dart';
import 'inspection_list_state.dart';

// ============================================
// 1. Repository Provider
// ============================================

/// Inspection Repository Provider (실 API)
final inspectionRepositoryProvider = Provider<InspectionRepository>((ref) {
  final dataSource = InspectionApiDataSource(ref.watch(dioProvider));
  return InspectionRepositoryImpl(dataSource);
});

/// GetInspectionList UseCase Provider
final getInspectionListUseCaseProvider = Provider<GetInspectionListUseCase>((ref) {
  final repository = ref.watch(inspectionRepositoryProvider);
  return GetInspectionListUseCase(repository);
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

/// 현장점검 목록 화면 상태 관리 Notifier
class InspectionListNotifier extends StateNotifier<InspectionListState> {
  final GetInspectionListUseCase _getInspectionList;

  InspectionListNotifier({
    required GetInspectionListUseCase getInspectionList,
  })  : _getInspectionList = getInspectionList,
        super(InspectionListState.initial());

  /// 초기 데이터 로딩 (기본 필터로 자동 검색).
  ///
  /// 거래처 목록은 [AccountSelectorField] 바텀시트가 열릴 때 자체 로드하므로
  /// 여기서 미리 받지 않는다.
  Future<void> initialize() async {
    await searchInspections();
  }

  /// 거래처 선택
  void selectAccount(int? accountId, String? accountName) {
    if (accountId == null) {
      state = state.copyWith(clearAccountFilter: true);
    } else {
      state = state.copyWith(
        selectedAccountId: accountId,
        selectedAccountName: accountName,
      );
    }
  }

  /// 분류 선택
  void selectCategory(InspectionCategory? category) {
    if (category == null) {
      state = state.copyWith(clearCategoryFilter: true);
    } else {
      state = state.copyWith(selectedCategory: category);
    }
  }

  /// 검색 시작일 변경.
  ///
  /// 레거시 daterangepicker `maxSpan: {days: 7}` 정합 — 시작일 기준으로 종료일이
  /// 7일 범위를 벗어나거나 시작일보다 이르면 종료일을 시작일+7일로 자동 보정한다.
  void updateFromDate(DateTime date) {
    var toDate = state.toDate;
    if (toDate.isBefore(date) ||
        toDate.difference(date).inDays > InspectionFilter.maxRangeDays) {
      toDate = date.add(const Duration(days: InspectionFilter.maxRangeDays));
    }
    state = state.copyWith(fromDate: date, toDate: toDate);
  }

  /// 검색 종료일 변경.
  ///
  /// 레거시 daterangepicker `maxSpan: {days: 7}` 정합 — 종료일 기준으로 시작일이
  /// 7일 범위를 벗어나거나 종료일보다 늦으면 시작일을 종료일-7일로 자동 보정한다.
  void updateToDate(DateTime date) {
    var fromDate = state.fromDate;
    if (date.isBefore(fromDate) ||
        date.difference(fromDate).inDays > InspectionFilter.maxRangeDays) {
      fromDate = date.subtract(const Duration(days: InspectionFilter.maxRangeDays));
    }
    state = state.copyWith(fromDate: fromDate, toDate: date);
  }

  /// 현장점검 목록 검색
  Future<void> searchInspections() async {
    final filter = InspectionFilter(
      accountId: state.selectedAccountId,
      category: state.selectedCategory,
      fromDate: state.fromDate,
      toDate: state.toDate,
    );

    state = state.toLoading();

    try {
      final items = await _getInspectionList.call(filter);

      state = state.copyWith(
        isLoading: false,
        items: items,
        hasSearched: true,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

// ============================================
// 3. StateNotifier Provider Definition
// ============================================

/// InspectionList StateNotifier Provider
final inspectionListProvider =
    StateNotifierProvider<InspectionListNotifier, InspectionListState>((ref) {
  final useCase = ref.watch(getInspectionListUseCaseProvider);

  return InspectionListNotifier(
    getInspectionList: useCase,
  );
});
