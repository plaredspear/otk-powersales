import '../../domain/entities/attendance_result.dart';
import '../../domain/entities/attendance_status.dart';
import '../../domain/entities/account_schedule_item.dart';

/// 출근등록 화면 상태
class AttendanceState {
  final bool isLoading;
  final bool isRegistering;
  final String? errorMessage;

  /// 거래처 목록 (전체)
  final List<AccountScheduleItem> allAccounts;

  /// 필터링된 거래처 목록 (검색 적용)
  final List<AccountScheduleItem> filteredAccounts;

  /// 총 거래처 수
  final int totalCount;

  /// 등록 완료 수
  final int registeredCount;

  /// 선택된 스케줄 ID (source에 따라 scheduleId 또는 displayWorkScheduleId)
  final int? selectedScheduleId;

  /// 선택된 항목의 source ("schedule" 또는 "master")
  final String? selectedSource;

  /// 검색 키워드
  final String searchKeyword;

  /// 등록 결과 (등록 완료 후)
  final AttendanceResult? registrationResult;

  /// 출근등록 현황 리스트
  final List<AttendanceStatus> statusList;

  /// 안전점검 완료 여부
  final bool safetyCheckCompleted;

  const AttendanceState({
    this.isLoading = false,
    this.isRegistering = false,
    this.errorMessage,
    this.allAccounts = const [],
    this.filteredAccounts = const [],
    this.totalCount = 0,
    this.registeredCount = 0,
    this.selectedScheduleId,
    this.selectedSource,
    this.searchKeyword = '',
    this.registrationResult,
    this.statusList = const [],
    this.safetyCheckCompleted = false,
  });

  factory AttendanceState.initial() {
    return const AttendanceState();
  }

  /// 로딩 상태
  AttendanceState toLoading() {
    return copyWith(isLoading: true, errorMessage: null);
  }

  /// 등록 중 상태
  AttendanceState toRegistering() {
    return copyWith(isRegistering: true, errorMessage: null);
  }

  /// 에러 상태
  AttendanceState toError(String message) {
    return copyWith(
      isLoading: false,
      isRegistering: false,
      errorMessage: message,
    );
  }

  /// 모든 거래처 등록 완료 여부
  bool get isAllRegistered =>
      totalCount > 0 && registeredCount >= totalCount;

  /// 남은 거래처 수
  int get remainingCount => totalCount - registeredCount;

  /// 미등록 거래처 목록
  List<AccountScheduleItem> get unregisteredAccounts =>
      filteredAccounts.where((s) => !s.isRegistered).toList();

  /// 고정 근무자 여부 (��든 거래처의 workCategory3이 "고정")
  bool get isFixedWorker =>
      allAccounts.isNotEmpty &&
      allAccounts.every((a) => a.workCategory3 == '고정');

  AttendanceState copyWith({
    bool? isLoading,
    bool? isRegistering,
    String? errorMessage,
    List<AccountScheduleItem>? allAccounts,
    List<AccountScheduleItem>? filteredAccounts,
    int? totalCount,
    int? registeredCount,
    int? selectedScheduleId,
    String? selectedSource,
    String? searchKeyword,
    AttendanceResult? registrationResult,
    List<AttendanceStatus>? statusList,
    bool? safetyCheckCompleted,
    bool clearSelection = false,
  }) {
    return AttendanceState(
      isLoading: isLoading ?? this.isLoading,
      isRegistering: isRegistering ?? this.isRegistering,
      errorMessage: errorMessage,
      allAccounts: allAccounts ?? this.allAccounts,
      filteredAccounts: filteredAccounts ?? this.filteredAccounts,
      totalCount: totalCount ?? this.totalCount,
      registeredCount: registeredCount ?? this.registeredCount,
      selectedScheduleId: clearSelection ? null : (selectedScheduleId ?? this.selectedScheduleId),
      selectedSource: clearSelection ? null : (selectedSource ?? this.selectedSource),
      searchKeyword: searchKeyword ?? this.searchKeyword,
      registrationResult: registrationResult ?? this.registrationResult,
      statusList: statusList ?? this.statusList,
      safetyCheckCompleted: safetyCheckCompleted ?? this.safetyCheckCompleted,
    );
  }
}
