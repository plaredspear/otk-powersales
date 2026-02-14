import '../../domain/entities/daily_schedule_info.dart';
import '../../domain/entities/monthly_schedule_day.dart';

/// 월간 캘린더 화면 상태
///
/// 월간 일정 데이터와 현재 표시 중인 연월을 관리한다.
class MyScheduleCalendarState {
  /// 로딩 상태
  final bool isLoading;

  /// 현재 표시 중인 연도
  final int currentYear;

  /// 현재 표시 중인 월 (1-12)
  final int currentMonth;

  /// 해당 월의 근무일 목록
  final List<MonthlyScheduleDay> workDays;

  /// 에러 메시지
  final String? errorMessage;

  const MyScheduleCalendarState({
    this.isLoading = false,
    required this.currentYear,
    required this.currentMonth,
    this.workDays = const [],
    this.errorMessage,
  });

  /// 초기 상태 (현재 월)
  factory MyScheduleCalendarState.initial() {
    final now = DateTime.now();
    return MyScheduleCalendarState(
      currentYear: now.year,
      currentMonth: now.month,
    );
  }

  /// 로딩 상태로 전환
  MyScheduleCalendarState toLoading() {
    return MyScheduleCalendarState(
      isLoading: true,
      currentYear: currentYear,
      currentMonth: currentMonth,
      workDays: workDays,
      errorMessage: null,
    );
  }

  /// 성공 상태로 전환
  MyScheduleCalendarState toData(List<MonthlyScheduleDay> data) {
    return MyScheduleCalendarState(
      isLoading: false,
      currentYear: currentYear,
      currentMonth: currentMonth,
      workDays: data,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  MyScheduleCalendarState toError(String message) {
    return MyScheduleCalendarState(
      isLoading: false,
      currentYear: currentYear,
      currentMonth: currentMonth,
      workDays: workDays,
      errorMessage: message,
    );
  }

  /// 다른 월로 이동
  MyScheduleCalendarState toMonth(int year, int month) {
    return MyScheduleCalendarState(
      isLoading: false,
      currentYear: year,
      currentMonth: month,
      workDays: const [],
      errorMessage: null,
    );
  }

  /// 데이터 로딩 완료 여부
  bool get isLoaded => workDays.isNotEmpty && !isLoading && errorMessage == null;

  /// 에러 상태 여부
  bool get isError => errorMessage != null;

  MyScheduleCalendarState copyWith({
    bool? isLoading,
    int? currentYear,
    int? currentMonth,
    List<MonthlyScheduleDay>? workDays,
    String? errorMessage,
  }) {
    return MyScheduleCalendarState(
      isLoading: isLoading ?? this.isLoading,
      currentYear: currentYear ?? this.currentYear,
      currentMonth: currentMonth ?? this.currentMonth,
      workDays: workDays ?? this.workDays,
      errorMessage: errorMessage,
    );
  }
}

/// 일정 상세 화면 상태
///
/// 특정 날짜의 일정 상세 데이터와 필터 상태를 관리한다.
class MyScheduleDetailState {
  /// 로딩 상태
  final bool isLoading;

  /// 일정 상세 정보
  final DailyScheduleInfo? scheduleInfo;

  /// "등록 전" 필터 활성 여부
  final bool showOnlyUnregistered;

  /// 에러 메시지
  final String? errorMessage;

  const MyScheduleDetailState({
    this.isLoading = false,
    this.scheduleInfo,
    this.showOnlyUnregistered = false,
    this.errorMessage,
  });

  /// 초기 상태
  factory MyScheduleDetailState.initial() {
    return const MyScheduleDetailState();
  }

  /// 로딩 상태로 전환
  MyScheduleDetailState toLoading() {
    return MyScheduleDetailState(
      isLoading: true,
      scheduleInfo: scheduleInfo,
      showOnlyUnregistered: showOnlyUnregistered,
      errorMessage: null,
    );
  }

  /// 성공 상태로 전환
  MyScheduleDetailState toData(DailyScheduleInfo data) {
    return MyScheduleDetailState(
      isLoading: false,
      scheduleInfo: data,
      showOnlyUnregistered: showOnlyUnregistered,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  MyScheduleDetailState toError(String message) {
    return MyScheduleDetailState(
      isLoading: false,
      scheduleInfo: scheduleInfo,
      showOnlyUnregistered: showOnlyUnregistered,
      errorMessage: message,
    );
  }

  /// 필터 토글
  MyScheduleDetailState toggleFilter() {
    return MyScheduleDetailState(
      isLoading: isLoading,
      scheduleInfo: scheduleInfo,
      showOnlyUnregistered: !showOnlyUnregistered,
      errorMessage: errorMessage,
    );
  }

  /// 필터 적용된 거래처 목록
  List<dynamic> get filteredStores {
    if (scheduleInfo == null) return [];
    if (!showOnlyUnregistered) return scheduleInfo!.stores;
    return scheduleInfo!.stores.where((store) => !store.isRegistered).toList();
  }

  /// 미등록 거래처 수
  int get unregisteredCount {
    if (scheduleInfo == null) return 0;
    return scheduleInfo!.stores.where((store) => !store.isRegistered).length;
  }

  /// 데이터 로딩 완료 여부
  bool get isLoaded =>
      scheduleInfo != null && !isLoading && errorMessage == null;

  /// 에러 상태 여부
  bool get isError => errorMessage != null;

  MyScheduleDetailState copyWith({
    bool? isLoading,
    DailyScheduleInfo? scheduleInfo,
    bool? showOnlyUnregistered,
    String? errorMessage,
  }) {
    return MyScheduleDetailState(
      isLoading: isLoading ?? this.isLoading,
      scheduleInfo: scheduleInfo ?? this.scheduleInfo,
      showOnlyUnregistered: showOnlyUnregistered ?? this.showOnlyUnregistered,
      errorMessage: errorMessage,
    );
  }
}
