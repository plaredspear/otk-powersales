import '../../domain/entities/pos_product.dart';
import '../../domain/entities/pos_sales.dart';

/// POS 매출 조회 상태.
///
/// 레거시 `posmain.jsp` 동등 — 거래처 1곳 + 기간 + 매출 조회 제품(누적/체크) 기준 POS 매출.
class PosSalesState {
  /// 조회 시작일 (YYYY-MM-DD)
  final String startDate;

  /// 조회 종료일 (YYYY-MM-DD)
  final String endDate;

  /// 선택된 거래처 ID (없으면 미선택 상태)
  final int? selectedCustomerId;

  /// 선택된 거래처명
  final String? selectedCustomerName;

  /// 매출 조회 제품 (제품명 검색/바코드 스캔으로 누적)
  final List<PosProduct> addedProducts;

  /// 체크된 제품 키(`productCode|barcode`) 집합 — 조회 시 이 제품의 바코드만 필터
  final Set<String> checkedKeys;

  /// 최근 추가 제품명 (레거시 `#productNm`)
  final String? lastAddedProductName;

  /// 최근 추가/스캔 바코드 (레거시 `#barcodeNo`)
  final String? lastScannedBarcode;

  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 매출 조회 1회 이상 실행 여부
  final bool hasQueried;

  /// 제품별 명세 (명세 모드에서만 표시)
  final List<PosSales> resultItems;

  /// 합계금액 (원)
  final int totalAmount;

  /// 합계수량 (EA)
  final int totalQuantity;

  /// 명세 모드 여부 (바코드 필터 조회 → 제품별 명세 노출). false 면 합계만 표시.
  final bool detailMode;

  const PosSalesState({
    required this.startDate,
    required this.endDate,
    this.selectedCustomerId,
    this.selectedCustomerName,
    this.addedProducts = const [],
    this.checkedKeys = const {},
    this.lastAddedProductName,
    this.lastScannedBarcode,
    this.isLoading = false,
    this.errorMessage,
    this.hasQueried = false,
    this.resultItems = const [],
    this.totalAmount = 0,
    this.totalQuantity = 0,
    this.detailMode = false,
  });

  /// 제품 식별 키.
  static String keyOf(PosProduct p) => '${p.productCode}|${p.barcode}';

  /// 체크되고 바코드가 있는 제품의 바코드 목록 (조회 필터).
  List<String> get checkedBarcodes => addedProducts
      .where((p) => checkedKeys.contains(keyOf(p)) && p.barcode.isNotEmpty)
      .map((p) => p.barcode)
      .toList();

  /// 초기 상태 (당월 1일 ~ 오늘, 거래처 미선택) — 레거시 daterangepicker 기본값.
  factory PosSalesState.initial() {
    final now = DateTime.now();
    final first = DateTime(now.year, now.month, 1);
    return PosSalesState(
      startDate: _fmt(first),
      endDate: _fmt(now),
    );
  }

  static String _fmt(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  PosSalesState copyWith({
    String? startDate,
    String? endDate,
    int? selectedCustomerId,
    String? selectedCustomerName,
    List<PosProduct>? addedProducts,
    Set<String>? checkedKeys,
    String? lastAddedProductName,
    String? lastScannedBarcode,
    bool? isLoading,
    String? errorMessage,
    bool clearErrorMessage = false,
    bool? hasQueried,
    List<PosSales>? resultItems,
    int? totalAmount,
    int? totalQuantity,
    bool? detailMode,
  }) {
    return PosSalesState(
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      selectedCustomerId: selectedCustomerId ?? this.selectedCustomerId,
      selectedCustomerName: selectedCustomerName ?? this.selectedCustomerName,
      addedProducts: addedProducts ?? this.addedProducts,
      checkedKeys: checkedKeys ?? this.checkedKeys,
      lastAddedProductName: lastAddedProductName ?? this.lastAddedProductName,
      lastScannedBarcode: lastScannedBarcode ?? this.lastScannedBarcode,
      isLoading: isLoading ?? this.isLoading,
      errorMessage:
          clearErrorMessage ? null : (errorMessage ?? this.errorMessage),
      hasQueried: hasQueried ?? this.hasQueried,
      resultItems: resultItems ?? this.resultItems,
      totalAmount: totalAmount ?? this.totalAmount,
      totalQuantity: totalQuantity ?? this.totalQuantity,
      detailMode: detailMode ?? this.detailMode,
    );
  }
}
