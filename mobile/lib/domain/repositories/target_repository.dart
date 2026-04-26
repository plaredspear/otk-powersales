import '../entities/target.dart';
import '../entities/progress.dart';

/// 목표 Repository 인터페이스
///
/// 거래처별 월 목표금액 및 진도율 조회를 추상화합니다.
/// 알라딘/SAP 시스템과 연동하여 목표를 관리합니다.
abstract class TargetRepository {
  /// 월별 목표 목록 조회
  ///
  /// [yearMonth]: 조회 년월 (예: '202601')
  /// [customerCode]: 거래처 코드 필터 (선택적)
  /// [category]: 카테고리 필터 (선택적)
  ///
  /// Returns: 조건에 맞는 목표 목록
  Future<List<Target>> getTargets({
    required String yearMonth,
    String? customerCode,
    String? category,
  });

  /// 특정 목표 조회
  ///
  /// [id]: 목표 ID
  ///
  /// Returns: 목표 엔티티
  /// Throws: 목표를 찾을 수 없는 경우 Exception
  Future<Target> getTargetById(String id);

  /// 거래처별 목표 조회
  ///
  /// [yearMonth]: 조회 년월
  /// [customerCode]: 거래처 코드
  ///
  /// Returns: 거래처의 월별 목표
  Future<Target?> getTargetByCustomer({
    required String yearMonth,
    required String customerCode,
  });

  /// 목표 생성 또는 수정
  ///
  /// [target]: 목표 엔티티
  ///
  /// Returns: 저장된 목표 엔티티
  Future<Target> saveTarget(Target target);

  /// 목표 삭제
  ///
  /// [id]: 목표 ID
  ///
  /// Returns: 삭제 성공 여부
  Future<bool> deleteTarget(String id);

  /// 진도율 계산
  ///
  /// [targetId]: 목표 ID
  ///
  /// Returns: 계산된 진도율 엔티티
  Future<Progress> getProgress(String targetId);

  /// 월별 전체 진도율 목록 조회
  ///
  /// [yearMonth]: 조회 년월
  ///
  /// Returns: 목표별 진도율 목록 (Map<목표ID, 진도율>)
  Future<Map<String, Progress>> getProgressList({
    required String yearMonth,
  });

  /// 진도율 부족 목표 목록 조회 (알림용)
  ///
  /// [yearMonth]: 조회 년월
  /// [thresholdPercentage]: 임계값 진도율 (기본값: 100.0)
  ///
  /// Returns: 진도율이 임계값 미만인 목표 목록
  Future<List<Target>> getInsufficientTargets({
    required String yearMonth,
    double thresholdPercentage = 100.0,
  });
}
