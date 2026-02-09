import '../entities/electronic_sales.dart';

/// 전산매출 Repository 인터페이스
///
/// Orora 영업 고객품목실적일 연동
/// 물류배부 실적 제외 전산실적 조회
abstract class ElectronicSalesRepository {
  /// 전산매출 조회
  ///
  /// [yearMonth] 년월 (예: "202601")
  /// [customerName] 거래처명 (선택)
  /// [productName] 제품명 (선택)
  /// [productCode] 제품 코드 (선택)
  ///
  /// Returns: 전산매출 목록
  Future<List<ElectronicSales>> getElectronicSales({
    required String yearMonth,
    String? customerName,
    String? productName,
    String? productCode,
  });

  /// 거래처별 전산매출 합계 조회
  ///
  /// [yearMonth] 년월 (예: "202601")
  /// [customerName] 거래처명
  ///
  /// Returns: 해당 거래처의 전체 실적 합계
  Future<ElectronicSales?> getCustomerTotal({
    required String yearMonth,
    required String customerName,
  });

  /// 제품별 전산매출 합계 조회
  ///
  /// [yearMonth] 년월 (예: "202601")
  /// [productCode] 제품 코드
  ///
  /// Returns: 해당 제품의 전체 실적 합계
  Future<ElectronicSales?> getProductTotal({
    required String yearMonth,
    required String productCode,
  });
}