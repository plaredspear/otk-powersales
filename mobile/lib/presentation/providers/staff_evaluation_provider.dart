import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../data/datasources/staff_evaluation_api_datasource.dart';
import '../../data/datasources/staff_evaluation_remote_datasource.dart';
import '../../data/repositories/staff_evaluation_repository_impl.dart';
import '../../domain/repositories/staff_evaluation_repository.dart';
import 'staff_evaluation_state.dart';

// --- Dependency Providers ---

final staffEvaluationRemoteDataSourceProvider =
    Provider<StaffEvaluationRemoteDataSource>((ref) {
  return StaffEvaluationApiDataSource(ref.watch(dioProvider));
});

final staffEvaluationRepositoryProvider =
    Provider<StaffEvaluationRepository>((ref) {
  return StaffEvaluationRepositoryImpl(
    remoteDataSource: ref.watch(staffEvaluationRemoteDataSourceProvider),
  );
});

// --- Notifier ---

/// 여사원 평가조회 화면 상태 관리 Notifier
///
/// 연월 이동(이전/다음) + 평가 조회를 관리한다. 거래처 선택이 없는
/// 본인 기준 조회라 화면 진입 시 즉시 로딩한다.
class StaffEvaluationNotifier extends StateNotifier<StaffEvaluationState> {
  final StaffEvaluationRepository _repository;

  StaffEvaluationNotifier({
    required StaffEvaluationRepository repository,
  })  : _repository = repository,
        super(StaffEvaluationState.initial());

  Future<void> initialize() async {
    if (state.evaluation == null && !state.isLoading) {
      await load();
    }
  }

  Future<void> load() async {
    try {
      state = state.toLoading();
      final result =
          await _repository.getStaffEvaluation(yearMonth: state.yearMonth);
      state = state.copyWith(isLoading: false, evaluation: result);
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  Future<void> goToPreviousMonth() async {
    if (!state.canGoToPreviousMonth || state.isLoading) return;
    state = state.copyWith(yearMonth: state.getPreviousMonth());
    await load();
  }

  Future<void> goToNextMonth() async {
    if (!state.canGoToNextMonth || state.isLoading) return;
    state = state.copyWith(yearMonth: state.getNextMonth());
    await load();
  }

  Future<void> refresh() async => load();
}

final staffEvaluationProvider =
    StateNotifierProvider<StaffEvaluationNotifier, StaffEvaluationState>((ref) {
  return StaffEvaluationNotifier(
    repository: ref.watch(staffEvaluationRepositoryProvider),
  );
});
