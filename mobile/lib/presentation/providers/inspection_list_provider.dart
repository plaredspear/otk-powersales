import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/mock/my_store_mock_repository.dart';
import '../../domain/entities/inspection_list_item.dart';
import '../../domain/repositories/inspection_repository.dart';
import '../../domain/usecases/get_inspection_list_usecase.dart';
import 'inspection_list_state.dart';

// ============================================
// 1. Mock Repository Provider (임시)
// ============================================

/// Inspection Repository Provider (Mock)
/// TODO: 실제 API 구현 후 InspectionRepositoryImpl로 교체
final inspectionRepositoryProvider = Provider<InspectionRepository>((ref) {
  // Mock implementation will be provided later
  throw UnimplementedError('InspectionRepository not implemented yet');
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

  /// 초기 데이터 로딩 (거래처 목록 + 자동 검색)
  Future<void> initialize() async {
    // 거래처 목록 로드
    final mockStoreRepo = MyStoreMockRepository();
    final storeResult = await mockStoreRepo.getMyStores();
    final storesMap = <int, String>{};
    for (final store in storeResult.stores) {
      storesMap[store.storeId] = store.storeName;
    }
    state = state.copyWith(stores: storesMap);

    // 기본 필터로 자동 검색
    await searchInspections();
  }

  /// 거래처 선택
  void selectStore(int? storeId, String? storeName) {
    if (storeId == null) {
      state = state.copyWith(clearStoreFilter: true);
    } else {
      state = state.copyWith(
        selectedStoreId: storeId,
        selectedStoreName: storeName,
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

  /// 검색 시작일 변경
  void updateFromDate(DateTime date) {
    state = state.copyWith(fromDate: date);
  }

  /// 검색 종료일 변경
  void updateToDate(DateTime date) {
    state = state.copyWith(toDate: date);
  }

  /// 현장점검 목록 검색
  Future<void> searchInspections() async {
    final filter = InspectionFilter(
      storeId: state.selectedStoreId,
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
        e.toString().replaceFirst('Exception: ', ''),
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
