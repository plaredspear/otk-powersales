import '../entities/monthly_sales.dart';
import '../repositories/monthly_sales_repository.dart';

/// 월매출 조회 UseCase
///
/// 거래처별 월매출 통계를 조회합니다.
class GetMonthlySalesUseCase {
  final MonthlySalesRepository _repository;

  GetMonthlySalesUseCase(this._repository);

  /// 월매출 통계 조회 실행
  ///
  /// [customerId]: 거래처 ID (null이면 전체 거래처 합산)
  /// [yearMonth]: 조회 연월 (YYYYMM 형식, 예: "202608")
  ///
  /// Returns: 월매출 통계 정보
  ///   - 제품유형별 매출 (상온, 냉장/냉동)
  ///   - 전년 동월 실적
  ///   - 월 평균 실적
  ///
  /// Throws:
  /// - [ArgumentError] yearMonth 형식이 유효하지 않은 경우
  Future<MonthlySales> call({
    String? customerId,
    required String yearMonth,
  }) async {
    // yearMonth 검증 (YYYYMM 형식)
    if (!_isValidYearMonth(yearMonth)) {
      throw ArgumentError('연월은 YYYYMM 형식이어야 합니다 (예: 202608)');
    }

    // Repository에서 월매출 통계 조회
    return await _repository.getMonthlySales(
      customerId: customerId,
      yearMonth: yearMonth,
    );
  }

  /// yearMonth 형식 검증 (YYYYMM)
  bool _isValidYearMonth(String yearMonth) {
    // 길이 체크
    if (yearMonth.length != 6) return false;

    // 숫자 체크
    final parsed = int.tryParse(yearMonth);
    if (parsed == null) return false;

    // 월 범위 체크 (01~12)
    final month = int.tryParse(yearMonth.substring(4, 6));
    if (month == null || month < 1 || month > 12) return false;

    return true;
  }
}
