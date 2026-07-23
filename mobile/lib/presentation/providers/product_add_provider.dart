import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/product_add_remote_datasource.dart';
import 'product_add_state.dart';

/// 제품추가 데이터소스 Provider.
final productAddDataSourceProvider = Provider<ProductAddRemoteDataSource>((ref) {
  return ProductAddRemoteDataSource(ref.watch(dioProvider));
});

/// 제품추가(소비기한) 팝업 상태 관리 Notifier.
///
/// 제품 검색(필터)과 거래처 주문이력 조회를 담당한다. 단일 선택 전용이므로
/// 선택 상태는 보유하지 않고, 화면에서 탭하면 즉시 반환한다.
class ProductAddNotifier extends StateNotifier<ProductAddState> {
  final ProductAddRemoteDataSource _dataSource;

  ProductAddNotifier(this._dataSource) : super(ProductAddState.initial());

  /// 페이지 크기 — 레거시 productPop pageSize(20)와 동일.
  static const int _pageSize = 20;

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

  /// 제품 필터 검색 실행 (첫 페이지부터). 레거시처럼 총 건수를 표시하고,
  /// 나머지는 [loadNextPage] 무한 스크롤로 이어서 로드한다(잘림 없음).
  Future<void> search({String? productName, String? barcode}) async {
    final name = productName?.trim() ?? '';
    final code = barcode?.trim() ?? '';
    final middle = state.selectedMiddle ?? '';
    final sub = state.selectedSub ?? '';

    state = state.copyWith(isLoading: true, clearError: true);

    try {
      final page = await _dataSource.searchByFilter(
        productName: name.isEmpty ? null : name,
        barcode: code.isEmpty ? null : code,
        category2: middle.isEmpty ? null : middle,
        category3: sub.isEmpty ? null : sub,
        page: 0,
        size: _pageSize,
      );

      state = state.copyWith(
        isLoading: false,
        searchResults: _parseProducts(page),
        totalCount: _parseTotal(page),
        currentPage: 0,
        isLastPage: page['last'] as bool? ?? true,
        hasSearched: true,
        appliedName: name,
        appliedBarcode: code,
        appliedMiddle: middle,
        appliedSub: sub,
        clearError: true,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        searchResults: const [],
        totalCount: 0,
        currentPage: 0,
        isLastPage: true,
        hasSearched: true,
        errorMessage: extractErrorMessage(e),
      );
    }
  }

  /// 다음 페이지 로드 (무한 스크롤). 검색 시 적용된 조건 스냅샷으로 이어서 조회한다.
  Future<void> loadNextPage() async {
    if (state.isLoading || state.isLoadingMore || state.isLastPage) return;

    state = state.copyWith(isLoadingMore: true);

    try {
      final nextPage = state.currentPage + 1;
      final page = await _dataSource.searchByFilter(
        productName: state.appliedName.isEmpty ? null : state.appliedName,
        barcode: state.appliedBarcode.isEmpty ? null : state.appliedBarcode,
        category2: state.appliedMiddle.isEmpty ? null : state.appliedMiddle,
        category3: state.appliedSub.isEmpty ? null : state.appliedSub,
        page: nextPage,
        size: _pageSize,
      );

      // productCode 기준 중복 제거 — offset 페이지네이션 경계로 같은 제품이
      // 다음 페이지에 다시 와도 중복 카드가 뜨지 않도록 방어.
      final seen = state.searchResults.map((p) => p.productCode).toSet();
      final merged = [
        ...state.searchResults,
        ..._parseProducts(page).where((p) => seen.add(p.productCode)),
      ];

      state = state.copyWith(
        isLoadingMore: false,
        searchResults: merged,
        totalCount: _parseTotal(page),
        currentPage: nextPage,
        isLastPage: page['last'] as bool? ?? true,
      );
    } catch (e) {
      // 추가 로드 실패 시 기존 목록은 유지하고 로더만 끈다.
      state = state.copyWith(isLoadingMore: false);
    }
  }

  List<ProductAddItem> _parseProducts(Map<String, dynamic> page) {
    return (page['content'] as List<dynamic>? ?? const [])
        .map((e) => ProductAddItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  int _parseTotal(Map<String, dynamic> page) {
    return (page['totalElements'] as num?)?.toInt() ??
        (page['content'] as List<dynamic>? ?? const []).length;
  }

  /// 주문이력 날짜 범위 설정 (최대 3일 제약은 호출부에서 보정).
  void setHistoryRange(DateTime from, DateTime to) {
    state = state.copyWith(historyFrom: from, historyTo: to);
  }

  /// 거래처 주문이력 조회.
  Future<void> searchOrderHistory(int accountId) async {
    state = state.copyWith(isLoading: true, clearError: true);

    try {
      final raw = await _dataSource.fetchOrderHistory(
        accountId: accountId,
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
