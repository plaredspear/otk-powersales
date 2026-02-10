import '../../domain/entities/attendance_result.dart';
import '../../domain/entities/attendance_status.dart';
import '../../domain/entities/store_schedule_item.dart';

/// 출근등록 화면 상태
class AttendanceState {
  final bool isLoading;
  final bool isRegistering;
  final String? errorMessage;

  /// 근무자 유형 (PATROL, IRREGULAR, FIXED)
  final String? workerType;

  /// 거래처 목록 (전체)
  final List<StoreScheduleItem> allStores;

  /// 필터링된 거래처 목록 (검색 적용)
  final List<StoreScheduleItem> filteredStores;

  /// 총 거래처 수
  final int totalCount;

  /// 등록 완료 수
  final int registeredCount;

  /// 선택된 근무유형 ('ROOM_TEMP' 또는 'REFRIGERATED')
  final String selectedWorkType;

  /// 선택된 거래처 ID
  final int? selectedStoreId;

  /// 검색 키워드
  final String searchKeyword;

  /// 등록 결과 (등록 완료 후)
  final AttendanceResult? registrationResult;

  /// 출근등록 현황 리스트
  final List<AttendanceStatus> statusList;

  const AttendanceState({
    this.isLoading = false,
    this.isRegistering = false,
    this.errorMessage,
    this.workerType,
    this.allStores = const [],
    this.filteredStores = const [],
    this.totalCount = 0,
    this.registeredCount = 0,
    this.selectedWorkType = 'ROOM_TEMP',
    this.selectedStoreId,
    this.searchKeyword = '',
    this.registrationResult,
    this.statusList = const [],
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

  /// 고정근무자 여부
  bool get isFixedWorker => workerType == 'FIXED';

  /// 모든 거래처 등록 완료 여부
  bool get isAllRegistered =>
      totalCount > 0 && registeredCount >= totalCount;

  /// 남은 거래처 수
  int get remainingCount => totalCount - registeredCount;

  /// 미등록 거래처 목록
  List<StoreScheduleItem> get unregisteredStores =>
      filteredStores.where((s) => !s.isRegistered).toList();

  AttendanceState copyWith({
    bool? isLoading,
    bool? isRegistering,
    String? errorMessage,
    String? workerType,
    List<StoreScheduleItem>? allStores,
    List<StoreScheduleItem>? filteredStores,
    int? totalCount,
    int? registeredCount,
    String? selectedWorkType,
    int? selectedStoreId,
    String? searchKeyword,
    AttendanceResult? registrationResult,
    List<AttendanceStatus>? statusList,
  }) {
    return AttendanceState(
      isLoading: isLoading ?? this.isLoading,
      isRegistering: isRegistering ?? this.isRegistering,
      errorMessage: errorMessage,
      workerType: workerType ?? this.workerType,
      allStores: allStores ?? this.allStores,
      filteredStores: filteredStores ?? this.filteredStores,
      totalCount: totalCount ?? this.totalCount,
      registeredCount: registeredCount ?? this.registeredCount,
      selectedWorkType: selectedWorkType ?? this.selectedWorkType,
      selectedStoreId: selectedStoreId ?? this.selectedStoreId,
      searchKeyword: searchKeyword ?? this.searchKeyword,
      registrationResult: registrationResult ?? this.registrationResult,
      statusList: statusList ?? this.statusList,
    );
  }
}
