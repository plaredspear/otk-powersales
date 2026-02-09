import '../../domain/entities/pos_sales.dart';

/// POS 매출 조회 필터 조건
class PosSalesFilter {
  final DateTime startDate;
  final DateTime endDate;
  final String? storeName;
  final String? productName;

  const PosSalesFilter({
    required this.startDate,
    required this.endDate,
    this.storeName,
    this.productName,
  });

  /// 기본 필터 (최근 30일)
  factory PosSalesFilter.defaultFilter() {
    final now = DateTime.now();
    return PosSalesFilter(
      startDate: DateTime(now.year, now.month - 1, now.day),
      endDate: now,
    );
  }

  PosSalesFilter copyWith({
    DateTime? startDate,
    DateTime? endDate,
    String? storeName,
    String? productName,
  }) {
    return PosSalesFilter(
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      storeName: storeName ?? this.storeName,
      productName: productName ?? this.productName,
    );
  }
}

/// POS 매출 조회 상태
class PosSalesState {
  /// 조회된 POS 매출 목록
  final List<PosSales> sales;

  /// 현재 적용된 필터
  final PosSalesFilter filter;

  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 총 판매금액
  final int totalAmount;

  /// 총 판매수량
  final int totalQuantity;

  const PosSalesState({
    required this.sales,
    required this.filter,
    this.isLoading = false,
    this.errorMessage,
    this.totalAmount = 0,
    this.totalQuantity = 0,
  });

  /// 초기 상태
  factory PosSalesState.initial() {
    return PosSalesState(
      sales: const [],
      filter: PosSalesFilter.defaultFilter(),
    );
  }

  PosSalesState copyWith({
    List<PosSales>? sales,
    PosSalesFilter? filter,
    bool? isLoading,
    String? errorMessage,
    int? totalAmount,
    int? totalQuantity,
  }) {
    return PosSalesState(
      sales: sales ?? this.sales,
      filter: filter ?? this.filter,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      totalAmount: totalAmount ?? this.totalAmount,
      totalQuantity: totalQuantity ?? this.totalQuantity,
    );
  }
}
