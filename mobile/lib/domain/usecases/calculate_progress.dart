import '../entities/progress.dart';
import '../entities/target.dart';

/// 진도율 계산 UseCase
///
/// 목표와 실적을 기반으로 진도율을 계산합니다.
class CalculateProgress {
  /// 목표와 실적으로 진도율을 계산합니다.
  Progress call({
    required int target,
    required int actual,
  }) {
    return Progress.calculate(
      targetAmount: target,
      actualAmount: actual,
    );
  }

  /// Target 엔티티로부터 진도율을 계산합니다.
  Progress fromTarget(Target target) {
    return Progress.calculate(
      targetAmount: target.targetAmount,
      actualAmount: target.actualAmount,
    );
  }

  /// 여러 Target의 평균 진도율을 계산합니다.
  Progress calculateAverage(List<Target> targets) {
    if (targets.isEmpty) {
      return Progress.calculate(targetAmount: 0, actualAmount: 0);
    }

    final totalTarget = targets.fold<int>(
      0,
      (sum, t) => sum + t.targetAmount,
    );
    final totalActual = targets.fold<int>(
      0,
      (sum, t) => sum + t.actualAmount,
    );

    return Progress.calculate(
      targetAmount: totalTarget,
      actualAmount: totalActual,
    );
  }

  /// 카테고리별 진도율을 계산합니다.
  Map<String, Progress> calculateByCategory(List<Target> targets) {
    final Map<String, List<Target>> groupedByCategory = {};

    for (final target in targets) {
      final category = target.category ?? '미분류';
      if (!groupedByCategory.containsKey(category)) {
        groupedByCategory[category] = [];
      }
      groupedByCategory[category]!.add(target);
    }

    return groupedByCategory.map(
      (category, categoryTargets) => MapEntry(
        category,
        calculateAverage(categoryTargets),
      ),
    );
  }

  /// 거래처별 진도율을 계산합니다.
  Map<String, Progress> calculateByCustomer(List<Target> targets) {
    final Map<String, List<Target>> groupedByCustomer = {};

    for (final target in targets) {
      if (!groupedByCustomer.containsKey(target.customerName)) {
        groupedByCustomer[target.customerName] = [];
      }
      groupedByCustomer[target.customerName]!.add(target);
    }

    return groupedByCustomer.map(
      (customer, customerTargets) => MapEntry(
        customer,
        calculateAverage(customerTargets),
      ),
    );
  }

  /// 진도율이 부족한 Target만 필터링합니다.
  List<Target> filterUnderPerforming(List<Target> targets) {
    return targets.where((target) {
      final progress = fromTarget(target);
      return progress.status == ProgressStatus.insufficient;
    }).toList();
  }

  /// 진도율이 초과한 Target만 필터링합니다.
  List<Target> filterOverPerforming(List<Target> targets) {
    return targets.where((target) {
      final progress = fromTarget(target);
      return progress.status == ProgressStatus.exceeded;
    }).toList();
  }
}
