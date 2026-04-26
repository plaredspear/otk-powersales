import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../domain/usecases/get_targets.dart';
import '../../domain/usecases/calculate_progress.dart';
import '../../data/repositories/mock/target_mock_repository.dart';
import 'target_state.dart';

/// TargetRepository Provider
final targetRepositoryProvider = Provider((ref) {
  return TargetMockRepository();
});

/// GetTargets UseCase Provider
final getTargetsUseCaseProvider = Provider((ref) {
  final repository = ref.watch(targetRepositoryProvider);
  return GetTargets(repository);
});

/// CalculateProgress UseCase Provider
final calculateProgressUseCaseProvider = Provider((ref) {
  return CalculateProgress();
});

/// TargetNotifier
///
/// 목표/진도율 상태를 관리하는 Notifier
class TargetNotifier extends StateNotifier<TargetState> {
  final GetTargets getTargetsUseCase;
  final CalculateProgress calculateProgressUseCase;

  TargetNotifier({
    required this.getTargetsUseCase,
    required this.calculateProgressUseCase,
    required String initialYearMonth,
  }) : super(TargetState.initial(initialYearMonth));

  /// 목표 목록 조회
  Future<void> fetchTargets() async {
    try {
      state = state.toLoading();

      // 목표 목록 조회
      final targets = await getTargetsUseCase(state.filter.yearMonth);

      // 진도율 목록 조회
      final progressList =
          await getTargetsUseCase.getProgressList(state.filter.yearMonth);

      // 총 목표/실적 금액 계산
      final totalTargetAmount =
          await getTargetsUseCase.calculateTotalTargetAmount(
        state.filter.yearMonth,
      );
      final totalActualAmount =
          await getTargetsUseCase.calculateTotalActualAmount(
        state.filter.yearMonth,
      );

      // 전체 진도율 계산
      final overallProgress = await getTargetsUseCase.calculateOverallProgress(
        state.filter.yearMonth,
      );

      state = state.toData(
        targets: targets,
        progressList: progressList,
        totalTargetAmount: totalTargetAmount,
        totalActualAmount: totalActualAmount,
        overallProgress: overallProgress,
      );
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// 필터 변경
  Future<void> updateFilter(TargetFilter filter) async {
    state = state.copyWith(filter: filter);
    await fetchTargets();
  }

  /// 년월 변경
  Future<void> changeYearMonth(String yearMonth) async {
    final newFilter = state.filter.copyWith(yearMonth: yearMonth);
    await updateFilter(newFilter);
  }

  /// 카테고리 필터 설정
  Future<void> filterByCategory(String? category) async {
    final newFilter = state.filter.copyWith(category: category);
    await updateFilter(newFilter);
  }

  /// 거래처 필터 설정
  Future<void> filterByCustomer(String? customerCode) async {
    final newFilter = state.filter.copyWith(customerCode: customerCode);
    await updateFilter(newFilter);
  }

  /// 진도율 부족 필터 토글
  Future<void> toggleInsufficientFilter() async {
    final newFilter = state.filter.copyWith(
      onlyInsufficient: !state.filter.onlyInsufficient,
    );
    await updateFilter(newFilter);
  }

  /// 필터 초기화
  Future<void> clearFilter() async {
    final newFilter = state.filter.clear();
    await updateFilter(newFilter);
  }

  /// 새로고침
  Future<void> refresh() async {
    await fetchTargets();
  }
}

/// Target Provider
final targetProvider =
    StateNotifierProvider<TargetNotifier, TargetState>((ref) {
  final getTargetsUseCase = ref.watch(getTargetsUseCaseProvider);
  final calculateProgressUseCase = ref.watch(calculateProgressUseCaseProvider);

  // 현재 년월 (기본값: 202602)
  final now = DateTime.now();
  final yearMonth =
      '${now.year}${now.month.toString().padLeft(2, '0')}';

  return TargetNotifier(
    getTargetsUseCase: getTargetsUseCase,
    calculateProgressUseCase: calculateProgressUseCase,
    initialYearMonth: yearMonth,
  );
});
