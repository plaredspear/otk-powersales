import '../../domain/entities/electronic_sales.dart';

/// 전산매출(ABC) 조회 상태.
///
/// 거래처 1곳 + 연월 기준 제품별 전산매출. 거래처 미선택 시 미조회 상태.
class ElectronicSalesState {
  /// 조회된 제품별 전산매출 목록
  final List<ElectronicSales> sales;

  /// 조회 년월 (예: "202601")
  final String yearMonth;

  /// 선택된 거래처 ID (없으면 미조회 상태)
  final int? selectedCustomerId;

  /// 선택된 거래처명
  final String? selectedCustomerName;

  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 총 금액
  final int totalAmount;

  /// 총 수량
  final int totalQuantity;

  /// 평균 증감율 (전년 비교 항목 있을 때만)
  final double? averageGrowthRate;

  const ElectronicSalesState({
    required this.sales,
    required this.yearMonth,
    this.selectedCustomerId,
    this.selectedCustomerName,
    this.isLoading = false,
    this.errorMessage,
    this.totalAmount = 0,
    this.totalQuantity = 0,
    this.averageGrowthRate,
  });

  /// 초기 상태 (현재 월, 거래처 미선택)
  factory ElectronicSalesState.initial() {
    final now = DateTime.now();
    final yearMonth = '${now.year}${now.month.toString().padLeft(2, '0')}';
    return ElectronicSalesState(sales: const [], yearMonth: yearMonth);
  }

  ElectronicSalesState copyWith({
    List<ElectronicSales>? sales,
    String? yearMonth,
    int? selectedCustomerId,
    String? selectedCustomerName,
    bool? isLoading,
    String? errorMessage,
    bool clearErrorMessage = false,
    int? totalAmount,
    int? totalQuantity,
    double? averageGrowthRate,
    bool clearAverageGrowthRate = false,
  }) {
    return ElectronicSalesState(
      sales: sales ?? this.sales,
      yearMonth: yearMonth ?? this.yearMonth,
      selectedCustomerId: selectedCustomerId ?? this.selectedCustomerId,
      selectedCustomerName: selectedCustomerName ?? this.selectedCustomerName,
      isLoading: isLoading ?? this.isLoading,
      errorMessage:
          clearErrorMessage ? null : (errorMessage ?? this.errorMessage),
      totalAmount: totalAmount ?? this.totalAmount,
      totalQuantity: totalQuantity ?? this.totalQuantity,
      averageGrowthRate: clearAverageGrowthRate
          ? null
          : (averageGrowthRate ?? this.averageGrowthRate),
    );
  }
}
