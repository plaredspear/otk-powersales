import '../../../domain/entities/target.dart';
import '../../../domain/entities/progress.dart';
import '../../../domain/repositories/target_repository.dart';
import '../../mock/target_mock_data.dart';

/// 목표 Mock Repository 구현체
///
/// TargetRepository 인터페이스를 구현하여 Mock 데이터를 반환합니다.
/// 실제 API 연동 전까지 UI 개발 및 테스트용으로 사용됩니다.
class TargetMockRepository implements TargetRepository {
  /// 메모리 내 저장소 (생성/수정/삭제용)
  final List<Target> _storage = List.from(TargetMockData.data);

  @override
  Future<List<Target>> getTargets({
    required String yearMonth,
    String? customerCode,
    String? category,
  }) async {
    // Mock 데이터에서 년월로 필터링
    var results = _storage.where((target) => target.yearMonth == yearMonth).toList();

    // 거래처 코드 필터링 (선택적)
    if (customerCode != null && customerCode.isNotEmpty) {
      results = results
          .where((target) => target.customerCode == customerCode)
          .toList();
    }

    // 카테고리 필터링 (선택적)
    if (category != null && category.isNotEmpty) {
      results = results
          .where((target) => target.category == category)
          .toList();
    }

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 목표금액순 정렬 (내림차순)
    results.sort((a, b) => b.targetAmount.compareTo(a.targetAmount));

    return results;
  }

  @override
  Future<Target> getTargetById(String id) async {
    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    try {
      return _storage.firstWhere((target) => target.id == id);
    } catch (e) {
      throw Exception('목표를 찾을 수 없습니다: $id');
    }
  }

  @override
  Future<Target?> getTargetByCustomer({
    required String yearMonth,
    required String customerCode,
  }) async {
    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    try {
      return _storage.firstWhere((target) =>
          target.yearMonth == yearMonth &&
          target.customerCode == customerCode);
    } catch (e) {
      return null;
    }
  }

  @override
  Future<Target> saveTarget(Target target) async {
    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 기존 목표 찾기
    final existingIndex = _storage.indexWhere((t) => t.id == target.id);

    if (existingIndex >= 0) {
      // 수정: 기존 목표 업데이트
      final updatedTarget = target.copyWith(
        updatedAt: DateTime.now(),
      );
      _storage[existingIndex] = updatedTarget;
      return updatedTarget;
    } else {
      // 생성: 새 목표 추가
      final newTarget = target.copyWith(
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );
      _storage.add(newTarget);
      return newTarget;
    }
  }

  @override
  Future<bool> deleteTarget(String id) async {
    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    final initialLength = _storage.length;
    _storage.removeWhere((target) => target.id == id);

    return _storage.length < initialLength;
  }

  @override
  Future<Progress> getProgress(String targetId) async {
    // 목표 조회
    final target = await getTargetById(targetId);

    // 진도율 계산
    return Progress.calculate(
      targetAmount: target.targetAmount,
      actualAmount: target.actualAmount,
    );
  }

  @override
  Future<Map<String, Progress>> getProgressList({
    required String yearMonth,
  }) async {
    // 해당 년월의 모든 목표 조회
    final targets = await getTargets(yearMonth: yearMonth);

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 각 목표에 대한 진도율 계산
    final progressMap = <String, Progress>{};
    for (final target in targets) {
      progressMap[target.id] = Progress.calculate(
        targetAmount: target.targetAmount,
        actualAmount: target.actualAmount,
      );
    }

    return progressMap;
  }

  @override
  Future<List<Target>> getInsufficientTargets({
    required String yearMonth,
    double thresholdPercentage = 100.0,
  }) async {
    // 해당 년월의 모든 목표 조회
    final targets = await getTargets(yearMonth: yearMonth);

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 진도율이 임계값 미만인 목표만 필터링
    final insufficientTargets = targets.where((target) {
      final progress = Progress.calculate(
        targetAmount: target.targetAmount,
        actualAmount: target.actualAmount,
      );
      return progress.percentage < thresholdPercentage;
    }).toList();

    // 진도율 부족순 정렬 (진도율 낮은 순)
    insufficientTargets.sort((a, b) {
      final progressA = Progress.calculate(
        targetAmount: a.targetAmount,
        actualAmount: a.actualAmount,
      );
      final progressB = Progress.calculate(
        targetAmount: b.targetAmount,
        actualAmount: b.actualAmount,
      );
      return progressA.percentage.compareTo(progressB.percentage);
    });

    return insufficientTargets;
  }
}
