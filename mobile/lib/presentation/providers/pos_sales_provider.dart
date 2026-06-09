import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/pos_product_api_datasource.dart';
import '../../data/datasources/pos_sales_api_datasource.dart';
import '../../data/repositories/pos_product_repository_impl.dart';
import '../../data/repositories/pos_sales_repository_impl.dart';
import '../../domain/entities/pos_product.dart';
import '../../domain/repositories/pos_product_repository.dart';
import '../../domain/repositories/pos_sales_repository.dart';
import '../../domain/usecases/get_pos_sales.dart';
import '../../domain/usecases/pos_product_usecase.dart';
import 'pos_sales_state.dart';

/// POS 매출 DataSource Provider (실 API)
final posSalesRemoteDataSourceProvider =
    Provider<PosSalesApiDataSource>((ref) {
  return PosSalesApiDataSource(ref.watch(dioProvider));
});

/// POS 매출 Repository Provider (실 API — MonthlySalesController)
final posSalesRepositoryProvider = Provider<PosSalesRepository>((ref) {
  return PosSalesRepositoryImpl(
    remoteDataSource: ref.watch(posSalesRemoteDataSourceProvider),
  );
});

/// POS 매출 UseCase Provider
final getPosSalesUseCaseProvider = Provider<GetPosSalesUseCase>((ref) {
  return GetPosSalesUseCase(ref.watch(posSalesRepositoryProvider));
});

/// POS 제품 검색 DataSource Provider (실 API)
final posProductRemoteDataSourceProvider =
    Provider<PosProductApiDataSource>((ref) {
  return PosProductApiDataSource(ref.watch(dioProvider));
});

/// POS 제품 검색 Repository Provider (실 API — ProductController)
final posProductRepositoryProvider = Provider<PosProductRepository>((ref) {
  return PosProductRepositoryImpl(
    remoteDataSource: ref.watch(posProductRemoteDataSourceProvider),
  );
});

/// POS 제품 검색 UseCase Provider
final posProductUseCaseProvider = Provider<PosProductUseCase>((ref) {
  return PosProductUseCase(ref.watch(posProductRepositoryProvider));
});

/// POS 매출 상태 관리 Notifier
class PosSalesNotifier extends StateNotifier<PosSalesState> {
  PosSalesNotifier(this._getPosSales) : super(PosSalesState.initial());

  final GetPosSalesUseCase _getPosSales;

  /// 기간(시작/종료일) 설정.
  void setDateRange(String startDate, String endDate) {
    state = state.copyWith(startDate: startDate, endDate: endDate);
  }

  /// 거래처 선택 — 선택 직후 합계 조회를 자동 실행 (레거시 진입 자동조회 정합).
  Future<void> selectCustomer(int customerId, String customerName) async {
    state = state.copyWith(
      selectedCustomerId: customerId,
      selectedCustomerName: customerName,
    );
    await fetchSales();
  }

  /// 매출 조회 제품 추가 (중복 제품코드+바코드는 무시). 추가 시 기본 체크.
  void addProduct(PosProduct product) {
    final key = PosSalesState.keyOf(product);
    final exists = state.addedProducts.any((p) => PosSalesState.keyOf(p) == key);
    if (exists) {
      state = state.copyWith(lastAddedProductName: product.productName);
      return;
    }
    state = state.copyWith(
      addedProducts: [...state.addedProducts, product],
      checkedKeys: {...state.checkedKeys, key},
      lastAddedProductName: product.productName,
    );
  }

  /// 매출 조회 제품 제거.
  void removeProduct(String key) {
    state = state.copyWith(
      addedProducts: state.addedProducts
          .where((p) => PosSalesState.keyOf(p) != key)
          .toList(),
      checkedKeys: {...state.checkedKeys}..remove(key),
    );
  }

  /// 제품 체크/해제 토글.
  void toggleChecked(String key) {
    final next = {...state.checkedKeys};
    if (next.contains(key)) {
      next.remove(key);
    } else {
      next.add(key);
    }
    state = state.copyWith(checkedKeys: next);
  }

  /// 최근 스캔 바코드 표시 값 설정 (레거시 `#barcodeNo`).
  void setLastScannedBarcode(String barcode) {
    state = state.copyWith(lastScannedBarcode: barcode);
  }

  /// POS 매출 조회.
  ///
  /// 체크된 제품의 바코드가 있으면 명세 모드(품목별 명세 + 합계), 없으면 합계 모드(합계금액만).
  Future<void> fetchSales() async {
    final customerId = state.selectedCustomerId;
    if (customerId == null) return;

    final barcodes = state.checkedBarcodes;
    final detailMode = barcodes.isNotEmpty;

    state = state.copyWith(isLoading: true, clearErrorMessage: true);

    try {
      final result = await _getPosSales.call(
        customerId: customerId,
        startDate: state.startDate,
        endDate: state.endDate,
        barcodes: barcodes,
      );

      state = state.copyWith(
        isLoading: false,
        hasQueried: true,
        detailMode: detailMode,
        resultItems: detailMode ? result.items : const [],
        totalAmount: result.totalAmount,
        totalQuantity: result.totalQuantity,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: extractErrorMessage(e),
      );
    }
  }

  /// 에러 메시지 제거.
  void clearError() {
    state = state.copyWith(clearErrorMessage: true);
  }

  /// 초기화 (거래처/기간/제품/결과 전체 초기화).
  void reset() {
    state = PosSalesState.initial();
  }
}

/// POS 매출 StateNotifier Provider
final posSalesProvider =
    StateNotifierProvider<PosSalesNotifier, PosSalesState>((ref) {
  return PosSalesNotifier(ref.watch(getPosSalesUseCaseProvider));
});
