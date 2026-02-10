import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/mock/my_store_mock_repository.dart';
import '../../domain/repositories/my_store_repository.dart';
import '../../domain/usecases/get_my_stores.dart';
import 'my_stores_state.dart';

// --- Dependency Providers ---

/// MyStore Repository Provider
final myStoreRepositoryProvider = Provider<MyStoreRepository>((ref) {
  return MyStoreMockRepository();
});

/// GetMyStores UseCase Provider
final getMyStoresUseCaseProvider = Provider<GetMyStores>((ref) {
  final repository = ref.watch(myStoreRepositoryProvider);
  return GetMyStores(repository);
});

// --- MyStoresNotifier ---

/// 내 거래처 상태 관리 Notifier
///
/// 거래처 목록 로딩, 클라이언트 사이드 검색, 검색 초기화를 관리합니다.
class MyStoresNotifier extends StateNotifier<MyStoresState> {
  final GetMyStores _getMyStores;

  MyStoresNotifier({
    required GetMyStores getMyStores,
  })  : _getMyStores = getMyStores,
        super(MyStoresState.initial());

  /// 내 거래처 목록 로딩
  Future<void> loadStores() async {
    state = state.toLoading();

    try {
      final result = await _getMyStores.call();

      state = state.copyWith(
        isLoading: false,
        allStores: result.stores,
        filteredStores: result.stores,
        totalCount: result.totalCount,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 거래처 검색 (클라이언트 사이드)
  ///
  /// 거래처명 또는 거래처 코드로 필터링합니다.
  void searchStores(String keyword) {
    if (keyword.isEmpty) {
      state = state.copyWith(
        searchKeyword: '',
        filteredStores: state.allStores,
      );
      return;
    }

    final lowerKeyword = keyword.toLowerCase();
    final filtered = state.allStores.where((store) {
      return store.storeName.toLowerCase().contains(lowerKeyword) ||
          store.storeCode.toLowerCase().contains(lowerKeyword);
    }).toList();

    state = state.copyWith(
      searchKeyword: keyword,
      filteredStores: filtered,
    );
  }

  /// 검색 초기화
  void clearSearch() {
    state = state.copyWith(
      searchKeyword: '',
      filteredStores: state.allStores,
    );
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

/// MyStores StateNotifier Provider
final myStoresProvider =
    StateNotifierProvider<MyStoresNotifier, MyStoresState>((ref) {
  return MyStoresNotifier(
    getMyStores: ref.watch(getMyStoresUseCaseProvider),
  );
});
