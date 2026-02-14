import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/repositories/mock/my_schedule_mock_repository.dart';
import '../../domain/repositories/my_schedule_repository.dart';
import '../../domain/usecases/get_daily_schedule.dart';
import '../../domain/usecases/get_monthly_schedule.dart';
import 'my_schedule_state.dart';

/// MySchedule Repository Provider
final myScheduleRepositoryProvider = Provider<MyScheduleRepository>((ref) {
  return MyScheduleMockRepository();
});

/// GetMonthlySchedule UseCase Provider
final getMonthlyScheduleUseCaseProvider = Provider<GetMonthlySchedule>((ref) {
  final repository = ref.watch(myScheduleRepositoryProvider);
  return GetMonthlySchedule(repository);
});

/// GetDailySchedule UseCase Provider
final getDailyScheduleUseCaseProvider = Provider<GetDailySchedule>((ref) {
  final repository = ref.watch(myScheduleRepositoryProvider);
  return GetDailySchedule(repository);
});

/// 월간 캘린더 상태 관리 Notifier
///
/// 월간 일정 데이터의 로딩, 성공, 에러 상태를 관리한다.
class MyScheduleCalendarNotifier extends StateNotifier<MyScheduleCalendarState> {
  MyScheduleCalendarNotifier(this._getMonthlySchedule)
      : super(MyScheduleCalendarState.initial()) {
    // 초기 데이터 로드
    loadMonthlySchedule(state.currentYear, state.currentMonth);
  }

  final GetMonthlySchedule _getMonthlySchedule;

  /// 월간 일정 조회
  Future<void> loadMonthlySchedule(int year, int month) async {
    state = state.toLoading();

    try {
      final workDays = await _getMonthlySchedule(year, month);
      state = state.toData(workDays);
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// 이전 월로 이동
  Future<void> goToPreviousMonth() async {
    int year = state.currentYear;
    int month = state.currentMonth - 1;

    if (month < 1) {
      month = 12;
      year--;
    }

    state = state.toMonth(year, month);
    await loadMonthlySchedule(year, month);
  }

  /// 다음 월로 이동
  Future<void> goToNextMonth() async {
    int year = state.currentYear;
    int month = state.currentMonth + 1;

    if (month > 12) {
      month = 1;
      year++;
    }

    state = state.toMonth(year, month);
    await loadMonthlySchedule(year, month);
  }

  /// 특정 월로 이동
  Future<void> goToMonth(int year, int month) async {
    state = state.toMonth(year, month);
    await loadMonthlySchedule(year, month);
  }
}

/// MyScheduleCalendar StateNotifier Provider
final myScheduleCalendarProvider =
    StateNotifierProvider<MyScheduleCalendarNotifier, MyScheduleCalendarState>(
  (ref) {
    final useCase = ref.watch(getMonthlyScheduleUseCaseProvider);
    return MyScheduleCalendarNotifier(useCase);
  },
);

/// 일정 상세 상태 관리 Notifier
///
/// 일간 일정 상세 데이터와 필터 상태를 관리한다.
class MyScheduleDetailNotifier extends StateNotifier<MyScheduleDetailState> {
  MyScheduleDetailNotifier(this._getDailySchedule)
      : super(MyScheduleDetailState.initial());

  final GetDailySchedule _getDailySchedule;

  /// 일간 일정 상세 조회
  Future<void> loadDailySchedule(DateTime date) async {
    state = state.toLoading();

    try {
      final scheduleInfo = await _getDailySchedule(date);
      state = state.toData(scheduleInfo);
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// "등록 전" 필터 토글
  void toggleUnregisteredFilter() {
    state = state.toggleFilter();
  }
}

/// MyScheduleDetail StateNotifier Provider
final myScheduleDetailProvider =
    StateNotifierProvider<MyScheduleDetailNotifier, MyScheduleDetailState>(
  (ref) {
    final useCase = ref.watch(getDailyScheduleUseCaseProvider);
    return MyScheduleDetailNotifier(useCase);
  },
);
