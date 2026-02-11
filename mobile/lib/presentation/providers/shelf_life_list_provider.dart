import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/mock/my_store_mock_repository.dart';
import '../../data/repositories/mock/shelf_life_mock_repository.dart';
import '../../domain/entities/shelf_life_item.dart';
import '../../domain/repositories/shelf_life_repository.dart';
import '../../domain/usecases/get_shelf_life_list_usecase.dart';
import 'shelf_life_list_state.dart';

// ============================================
// 1. Dependency Providers
// ============================================

/// ShelfLife Repository Provider
final shelfLifeRepositoryProvider = Provider<ShelfLifeRepository>((ref) {
  return ShelfLifeMockRepository();
});

/// GetShelfLifeList UseCase Provider
final getShelfLifeListUseCaseProvider =
    Provider<GetShelfLifeList>((ref) {
  final repository = ref.watch(shelfLifeRepositoryProvider);
  return GetShelfLifeList(repository);
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

/// 유통기한 관리 목록 상태 관리 Notifier
class ShelfLifeListNotifier extends StateNotifier<ShelfLifeListState> {
  final GetShelfLifeList _getShelfLifeList;

  ShelfLifeListNotifier({
    required GetShelfLifeList getShelfLifeList,
  })  : _getShelfLifeList = getShelfLifeList,
        super(ShelfLifeListState.initial());

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
    await searchShelfLife();
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

  /// 검색 시작일 변경
  void updateFromDate(DateTime date) {
    state = state.copyWith(fromDate: date);
  }

  /// 검색 종료일 변경
  void updateToDate(DateTime date) {
    state = state.copyWith(toDate: date);
  }

  /// 유통기한 목록 검색
  Future<void> searchShelfLife() async {
    final filter = ShelfLifeFilter(
      storeId: state.selectedStoreId,
      fromDate: state.fromDate,
      toDate: state.toDate,
    );

    state = state.toLoading();

    try {
      final items = await _getShelfLifeList.call(filter);

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

/// ShelfLifeList StateNotifier Provider
final shelfLifeListProvider =
    StateNotifierProvider<ShelfLifeListNotifier, ShelfLifeListState>((ref) {
  final useCase = ref.watch(getShelfLifeListUseCaseProvider);

  return ShelfLifeListNotifier(
    getShelfLifeList: useCase,
  );
});
