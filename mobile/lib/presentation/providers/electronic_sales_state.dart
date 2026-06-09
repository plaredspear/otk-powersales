import '../../domain/entities/electronic_sales.dart';

/// 전산매출(ABC) 조회 결과 상태.
///
/// 거래처/기간/매출 조회 제품 같은 입력값은 화면이 보유하고, 본 상태는 "매출 조회" 실행
/// 결과(합계금액 + 제품별 실적 + 로딩/에러)만 담는다.
class ElectronicSalesState {
  /// 조회된 제품별 전산매출 목록 (제품 미선택 시 빈 목록)
  final List<ElectronicSales> sales;

  /// 합계금액 (원) — 서버 산출
  final int totalAmount;

  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// "매출 조회" 1회 이상 실행 여부 (초기 안내 ↔ 결과 화면 분기용)
  final bool hasSearched;

  const ElectronicSalesState({
    this.sales = const [],
    this.totalAmount = 0,
    this.isLoading = false,
    this.errorMessage,
    this.hasSearched = false,
  });

  /// 초기 상태 (미조회).
  factory ElectronicSalesState.initial() => const ElectronicSalesState();

  /// 총 수량 합계 (제품 목록 기준).
  int get totalQuantity => sales.fold(0, (sum, e) => sum + e.quantity);

  ElectronicSalesState copyWith({
    List<ElectronicSales>? sales,
    int? totalAmount,
    bool? isLoading,
    String? errorMessage,
    bool clearErrorMessage = false,
    bool? hasSearched,
  }) {
    return ElectronicSalesState(
      sales: sales ?? this.sales,
      totalAmount: totalAmount ?? this.totalAmount,
      isLoading: isLoading ?? this.isLoading,
      errorMessage:
          clearErrorMessage ? null : (errorMessage ?? this.errorMessage),
      hasSearched: hasSearched ?? this.hasSearched,
    );
  }
}
