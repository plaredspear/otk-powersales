import '../entities/target.dart';
import '../entities/progress.dart';
import '../repositories/target_repository.dart';

/// 목표 조회 UseCase
///
/// 목표 데이터를 조회하고 다양한 필터링을 제공합니다.
class GetTargets {
  final TargetRepository repository;

  GetTargets(this.repository);

  /// 월별 모든 목표를 조회합니다.
  Future<List<Target>> call(String yearMonth) async {
    return await repository.getTargets(yearMonth: yearMonth);
  }

  /// ID로 특정 목표를 조회합니다.
  Future<Target> getById(String id) async {
    return await repository.getTargetById(id);
  }

  /// 카테고리별로 목표를 조회합니다.
  Future<List<Target>> getByCategory({
    required String yearMonth,
    required String category,
  }) async {
    return await repository.getTargets(
      yearMonth: yearMonth,
      category: category,
    );
  }

  /// 거래처별로 목표를 조회합니다.
  Future<Target?> getByCustomer({
    required String yearMonth,
    required String customerCode,
  }) async {
    return await repository.getTargetByCustomer(
      yearMonth: yearMonth,
      customerCode: customerCode,
    );
  }

  /// 진도율 부족 목표만 조회합니다.
  Future<List<Target>> getInsufficient({
    required String yearMonth,
    double thresholdPercentage = 100.0,
  }) async {
    return await repository.getInsufficientTargets(
      yearMonth: yearMonth,
      thresholdPercentage: thresholdPercentage,
    );
  }

  /// 특정 목표의 진도율을 조회합니다.
  Future<Progress> getProgress(String targetId) async {
    return await repository.getProgress(targetId);
  }

  /// 월별 전체 진도율 목록을 조회합니다.
  Future<Map<String, Progress>> getProgressList(String yearMonth) async {
    return await repository.getProgressList(yearMonth: yearMonth);
  }

  /// 총 목표 금액을 계산합니다.
  Future<int> calculateTotalTargetAmount(String yearMonth) async {
    final targets = await repository.getTargets(yearMonth: yearMonth);
    return targets.fold<int>(
      0,
      (sum, target) => sum + target.targetAmount,
    );
  }

  /// 총 실적 금액을 계산합니다.
  Future<int> calculateTotalActualAmount(String yearMonth) async {
    final targets = await repository.getTargets(yearMonth: yearMonth);
    return targets.fold<int>(
      0,
      (sum, target) => sum + target.actualAmount,
    );
  }

  /// 전체 진도율을 계산합니다.
  Future<Progress> calculateOverallProgress(String yearMonth) async {
    final totalTarget = await calculateTotalTargetAmount(yearMonth);
    final totalActual = await calculateTotalActualAmount(yearMonth);

    return Progress.calculate(
      targetAmount: totalTarget,
      actualAmount: totalActual,
    );
  }
}
