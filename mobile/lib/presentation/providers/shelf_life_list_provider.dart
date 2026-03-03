import 'package:dio/dio.dart';
import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../data/datasources/shelf_life_api_datasource.dart';
import '../../data/repositories/shelf_life_repository_impl.dart';
import '../../domain/entities/shelf_life_item.dart';
import '../../domain/repositories/shelf_life_repository.dart';
import '../../domain/usecases/get_shelf_life_list_usecase.dart';
import 'shelf_life_list_state.dart';

// ============================================
// 1. Dependency Providers
// ============================================

/// ShelfLife Repository Provider
final shelfLifeRepositoryProvider = Provider<ShelfLifeRepository>((ref) {
  final dio = ref.watch(dioProvider);
  final dataSource = ShelfLifeApiDataSource(dio);
  return ShelfLifeRepositoryImpl(remoteDataSource: dataSource);
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
  final Dio _dio;

  ShelfLifeListNotifier({
    required GetShelfLifeList getShelfLifeList,
    required Dio dio,
  })  : _getShelfLifeList = getShelfLifeList,
        _dio = dio,
        super(ShelfLifeListState.initial());

  /// 초기 데이터 로딩 (거래처 목록 + 자동 검색)
  Future<void> initialize() async {
    // 거래처 목록 로드
    await _loadStores();

    // 기본 필터로 자동 검색
    await searchShelfLife();
  }

  /// GET /api/v1/stores/my 호출하여 거래처 목록 로드
  Future<void> _loadStores() async {
    state = state.copyWith(isStoresLoading: true);
    try {
      final response = await _dio.get('/api/v1/stores/my');
      final data = response.data['data'] as Map<String, dynamic>;
      final storesList = data['stores'] as List<dynamic>;
      final storesMap = <String, String>{};
      for (final store in storesList) {
        final storeMap = store as Map<String, dynamic>;
        final code = storeMap['store_code'] as String;
        final name = storeMap['store_name'] as String;
        storesMap[code] = name;
      }
      state = state.copyWith(stores: storesMap, isStoresLoading: false);
    } catch (e) {
      // 거래처 로딩 실패 시 빈 목록 유지, "전체" 상태 유지
      state = state.copyWith(isStoresLoading: false);
    }
  }

  /// 거래처 선택
  void selectStore(String? accountCode, String? accountName) {
    if (accountCode == null) {
      state = state.copyWith(clearStoreFilter: true);
    } else {
      state = state.copyWith(
        selectedAccountCode: accountCode,
        selectedAccountName: accountName,
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
      accountCode: state.selectedAccountCode,
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

/// ShelfLifeList StateNotifier Provider
final shelfLifeListProvider =
    StateNotifierProvider<ShelfLifeListNotifier, ShelfLifeListState>((ref) {
  final useCase = ref.watch(getShelfLifeListUseCaseProvider);
  final dio = ref.watch(dioProvider);

  return ShelfLifeListNotifier(
    getShelfLifeList: useCase,
    dio: dio,
  );
});
