import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/product_add_remote_datasource.dart';
import 'product_add_state.dart';

/// 제품추가 데이터소스 Provider.
final productAddDataSourceProvider = Provider<ProductAddRemoteDataSource>((ref) {
  return ProductAddRemoteDataSource(ref.watch(dioProvider));
});

/// 제품추가(유통기한) 팝업 상태 관리 Notifier.
///
/// 제품 검색(필터)과 거래처 주문이력 조회를 담당한다. 단일 선택 전용이므로
/// 선택 상태는 보유하지 않고, 화면에서 탭하면 즉시 반환한다.
class ProductAddNotifier extends StateNotifier<ProductAddState> {
  final ProductAddRemoteDataSource _dataSource;

  ProductAddNotifier(this._dataSource) : super(ProductAddState.initial());

  static String _fmtDate(DateTime d) {
    final m = d.month.toString().padLeft(2, '0');
    final day = d.day.toString().padLeft(2, '0');
    return '${d.year}-$m-$day';
  }

  /// 카테고리 목록 로드 (드롭다운 소스).
  Future<void> loadCategories() async {
    try {
      final raw = await _dataSource.fetchCategories();
      final categories = raw.map(ProductCategory.fromJson).toList();
      state = state.copyWith(categories: categories);
    } catch (e) {
      // 카테고리 로드 실패는 검색 자체를 막지 않는다 (드롭다운만 비활성).
      state = state.copyWith(errorMessage: extractErrorMessage(e));
    }
  }

  /// 중분류 선택 (변경 시 소분류 초기화).
  void selectMiddle(String? middle) {
    state = state.copyWith(
      selectedMiddle: middle,
      clearMiddle: middle == null,
      clearSub: true,
    );
  }

  /// 소분류 선택.
  void selectSub(String? sub) {
    state = state.copyWith(selectedSub: sub, clearSub: sub == null);
  }

  /// 제품 필터 검색 실행.
  Future<void> search({String? productName, String? barcode}) async {
    state = state.copyWith(isLoading: true, clearError: true);

    try {
      final page = await _dataSource.searchByFilter(
        productName: productName,
        barcode: barcode,
        category2: state.selectedMiddle,
        category3: state.selectedSub,
        page: 0,
        size: 100,
      );

      final content = (page['content'] as List<dynamic>? ?? const [])
          .map((e) => ProductAddItem.fromJson(e as Map<String, dynamic>))
          .toList();

      state = state.copyWith(
        isLoading: false,
        searchResults: content,
        hasSearched: true,
        clearError: true,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        searchResults: const [],
        hasSearched: true,
        errorMessage: extractErrorMessage(e),
      );
    }
  }

  /// 주문이력 날짜 범위 설정 (최대 3일 제약은 호출부에서 보정).
  void setHistoryRange(DateTime from, DateTime to) {
    state = state.copyWith(historyFrom: from, historyTo: to);
  }

  /// 거래처 주문이력 조회.
  Future<void> searchOrderHistory(String accountCode) async {
    state = state.copyWith(isLoading: true, clearError: true);

    try {
      final raw = await _dataSource.fetchOrderHistory(
        accountCode: accountCode,
        startDate: _fmtDate(state.historyFrom),
        endDate: _fmtDate(state.historyTo),
      );
      final groups = raw.map(OrderHistoryDateGroup.fromJson).toList();

      state = state.copyWith(
        isLoading: false,
        orderHistoryGroups: groups,
        hasSearchedHistory: true,
        clearError: true,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        orderHistoryGroups: const [],
        hasSearchedHistory: true,
        errorMessage: extractErrorMessage(e),
      );
    }
  }

  void clearError() {
    state = state.copyWith(clearError: true);
  }
}

/// 제품추가 Provider (autoDispose — 팝업 닫히면 상태 초기화).
final productAddProvider =
    StateNotifierProvider.autoDispose<ProductAddNotifier, ProductAddState>(
  (ref) => ProductAddNotifier(ref.watch(productAddDataSourceProvider)),
);
