import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../data/datasources/product_expiration_api_datasource.dart';
import '../../data/repositories/product_expiration_repository_impl.dart';
import '../../domain/entities/product_expiration_item.dart';
import '../../domain/repositories/product_expiration_repository.dart';
import '../../domain/usecases/get_product_expiration_list_usecase.dart';
import 'product_expiration_list_state.dart';

// ============================================
// 1. Dependency Providers
// ============================================

/// ProductExpiration Repository Provider
final productExpirationRepositoryProvider = Provider<ProductExpirationRepository>((ref) {
  final dio = ref.watch(dioProvider);
  final dataSource = ProductExpirationApiDataSource(dio);
  return ProductExpirationRepositoryImpl(remoteDataSource: dataSource);
});

/// GetProductExpirationList UseCase Provider
final getProductExpirationListUseCaseProvider =
    Provider<GetProductExpirationList>((ref) {
  final repository = ref.watch(productExpirationRepositoryProvider);
  return GetProductExpirationList(repository);
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

/// 소비기한 관리 목록 상태 관리 Notifier
class ProductExpirationListNotifier extends StateNotifier<ProductExpirationListState> {
  final GetProductExpirationList _getProductExpirationList;

  ProductExpirationListNotifier({
    required GetProductExpirationList getProductExpirationList,
  })  : _getProductExpirationList = getProductExpirationList,
        super(ProductExpirationListState.initial());

  /// 초기 데이터 로딩 (기본 필터로 자동 검색).
  ///
  /// 거래처 목록은 [AccountSelectorField] 바텀시트가 온디맨드로 조회하므로 프리로드하지 않는다.
  Future<void> initialize() async {
    await searchProductExpiration();
  }

  /// 거래처 선택
  void selectAccount(String? accountCode, String? accountName) {
    if (accountCode == null) {
      state = state.copyWith(clearAccountFilter: true);
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

  /// 소비기한 목록 검색
  Future<void> searchProductExpiration() async {
    final filter = ProductExpirationFilter(
      accountCode: state.selectedAccountCode,
      fromDate: state.fromDate,
      toDate: state.toDate,
    );

    state = state.toLoading();

    try {
      final items = await _getProductExpirationList.call(filter);

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

/// ProductExpirationList StateNotifier Provider
final productExpirationListProvider =
    StateNotifierProvider<ProductExpirationListNotifier, ProductExpirationListState>((ref) {
  final useCase = ref.watch(getProductExpirationListUseCaseProvider);

  return ProductExpirationListNotifier(
    getProductExpirationList: useCase,
  );
});
