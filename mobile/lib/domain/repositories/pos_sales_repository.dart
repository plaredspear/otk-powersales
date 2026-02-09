import '../entities/pos_sales.dart';

/// POS 매출 Repository 인터페이스
///
/// 대형마트 EDI 연동을 통한 POS 매출 데이터 조회를 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class PosSalesRepository {
  /// POS 매출 데이터 조회
  ///
  /// [startDate]: 조회 시작일
  /// [endDate]: 조회 종료일
  /// [storeName]: 매장명 필터 (선택적)
  /// [productName]: 제품명 필터 (선택적)
  ///
  /// Returns: 조건에 맞는 POS 매출 목록
  Future<List<PosSales>> getPosSales({
    required DateTime startDate,
    required DateTime endDate,
    String? storeName,
    String? productName,
  });

  /// 특정 제품의 POS 매출 조회
  ///
  /// [productCode]: 제품 코드
  /// [startDate]: 조회 시작일
  /// [endDate]: 조회 종료일
  ///
  /// Returns: 제품별 POS 매출 목록
  Future<List<PosSales>> getPosSalesByProduct({
    required String productCode,
    required DateTime startDate,
    required DateTime endDate,
  });

  /// 특정 매장의 POS 매출 조회
  ///
  /// [storeName]: 매장명
  /// [startDate]: 조회 시작일
  /// [endDate]: 조회 종료일
  ///
  /// Returns: 매장별 POS 매출 목록
  Future<List<PosSales>> getPosSalesByStore({
    required String storeName,
    required DateTime startDate,
    required DateTime endDate,
  });
}
