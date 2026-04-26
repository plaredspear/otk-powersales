import '../../domain/entities/electronic_sales.dart';

/// 전산매출 조회 필터 조건
class ElectronicSalesFilter {
  /// 년월 (필수, 예: "202601")
  final String yearMonth;

  /// 거래처명 (선택)
  final String? customerName;

  /// 제품명 (선택)
  final String? productName;

  /// 제품 코드 (선택)
  final String? productCode;

  const ElectronicSalesFilter({
    required this.yearMonth,
    this.customerName,
    this.productName,
    this.productCode,
  });

  /// 기본 필터 (현재 월)
  factory ElectronicSalesFilter.defaultFilter() {
    final now = DateTime.now();
    final yearMonth = '${now.year}${now.month.toString().padLeft(2, '0')}';
    return ElectronicSalesFilter(yearMonth: yearMonth);
  }

  ElectronicSalesFilter copyWith({
    String? yearMonth,
    String? customerName,
    String? productName,
    String? productCode,
  }) {
    return ElectronicSalesFilter(
      yearMonth: yearMonth ?? this.yearMonth,
      customerName: customerName ?? this.customerName,
      productName: productName ?? this.productName,
      productCode: productCode ?? this.productCode,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is ElectronicSalesFilter &&
        other.yearMonth == yearMonth &&
        other.customerName == customerName &&
        other.productName == productName &&
        other.productCode == productCode;
  }

  @override
  int get hashCode {
    return Object.hash(
      yearMonth,
      customerName,
      productName,
      productCode,
    );
  }
}

/// 전산매출 조회 상태
class ElectronicSalesState {
  /// 조회된 전산매출 목록
  final List<ElectronicSales> sales;

  /// 현재 적용된 필터
  final ElectronicSalesFilter filter;

  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 총 판매금액
  final int totalAmount;

  /// 총 판매수량
  final int totalQuantity;

  /// 평균 증감율 (%)
  final double? averageGrowthRate;

  /// 거래처 목록 (중복 제거)
  final List<String> customerList;

  /// 제품 목록 (중복 제거)
  final List<String> productList;

  const ElectronicSalesState({
    required this.sales,
    required this.filter,
    this.isLoading = false,
    this.errorMessage,
    this.totalAmount = 0,
    this.totalQuantity = 0,
    this.averageGrowthRate,
    this.customerList = const [],
    this.productList = const [],
  });

  /// 초기 상태
  factory ElectronicSalesState.initial() {
    return ElectronicSalesState(
      sales: const [],
      filter: ElectronicSalesFilter.defaultFilter(),
    );
  }

  ElectronicSalesState copyWith({
    List<ElectronicSales>? sales,
    ElectronicSalesFilter? filter,
    bool? isLoading,
    String? errorMessage,
    int? totalAmount,
    int? totalQuantity,
    double? averageGrowthRate,
    List<String>? customerList,
    List<String>? productList,
  }) {
    return ElectronicSalesState(
      sales: sales ?? this.sales,
      filter: filter ?? this.filter,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      totalAmount: totalAmount ?? this.totalAmount,
      totalQuantity: totalQuantity ?? this.totalQuantity,
      averageGrowthRate: averageGrowthRate ?? this.averageGrowthRate,
      customerList: customerList ?? this.customerList,
      productList: productList ?? this.productList,
    );
  }
}
