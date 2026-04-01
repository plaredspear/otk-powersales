import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/attendance_api_datasource.dart';
import '../../data/repositories/attendance_repository_impl.dart';
import '../../domain/repositories/attendance_repository.dart';
import '../../domain/usecases/get_attendance_status.dart';
import '../../domain/usecases/get_account_list.dart';
import '../../domain/usecases/register_attendance.dart';
import 'attendance_state.dart';

// --- Dependency Providers ---

final attendanceRepositoryProvider = Provider<AttendanceRepository>((ref) {
  final dio = ref.watch(dioProvider);
  final dataSource = AttendanceApiDataSource(dio);
  return AttendanceRepositoryImpl(dataSource: dataSource);
});

final getAccountListUseCaseProvider = Provider<GetAccountList>((ref) {
  final repository = ref.watch(attendanceRepositoryProvider);
  return GetAccountList(repository);
});

final registerAttendanceUseCaseProvider = Provider<RegisterAttendance>((ref) {
  final repository = ref.watch(attendanceRepositoryProvider);
  return RegisterAttendance(repository);
});

final getAttendanceStatusUseCaseProvider =
    Provider<GetAttendanceStatus>((ref) {
  final repository = ref.watch(attendanceRepositoryProvider);
  return GetAttendanceStatus(repository);
});

// --- AttendanceNotifier ---

class AttendanceNotifier extends StateNotifier<AttendanceState> {
  final GetAccountList _getAccountList;
  final RegisterAttendance _registerAttendance;
  final GetAttendanceStatus _getAttendanceStatus;

  AttendanceNotifier({
    required GetAccountList getAccountList,
    required RegisterAttendance registerAttendance,
    required GetAttendanceStatus getAttendanceStatus,
  })  : _getAccountList = getAccountList,
        _registerAttendance = registerAttendance,
        _getAttendanceStatus = getAttendanceStatus,
        super(AttendanceState.initial());

  /// 거래처 목록 로딩
  Future<void> loadAccounts() async {
    state = state.toLoading();

    try {
      final result = await _getAccountList.call();

      state = state.copyWith(
        isLoading: false,
        allAccounts: result.accounts,
        filteredAccounts: result.accounts,
        totalCount: result.totalCount,
        registeredCount: result.registeredCount,
        safetyCheckCompleted: result.safetyCheckCompleted,
        errorMessage: null,
      );

      // 고정 근무자: 미등록 거래처가 1개이면 자동 선택
      if (state.isFixedWorker) {
        final unregistered = state.unregisteredAccounts;
        if (unregistered.length == 1) {
          selectAccount(unregistered.first.scheduleId);
        }
      }
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 거래처 검색
  void searchAccounts(String keyword) {
    final lowerKeyword = keyword.toLowerCase();
    final filtered = state.allAccounts.where((account) {
      if (keyword.isEmpty) return true;
      return account.accountName.toLowerCase().contains(lowerKeyword) ||
          account.address.toLowerCase().contains(lowerKeyword) ||
          (account.accountTypeCode?.toLowerCase().contains(lowerKeyword) ?? false);
    }).toList();

    state = state.copyWith(
      searchKeyword: keyword,
      filteredAccounts: filtered,
      selectedScheduleId: null,
    );
  }

  /// 거래처 선택
  void selectAccount(int scheduleId) {
    state = state.copyWith(selectedScheduleId: scheduleId);
  }

  /// 출근등록
  Future<void> register({double? latitude, double? longitude}) async {
    final scheduleId = state.selectedScheduleId;
    if (scheduleId == null) return;

    if (latitude == null || longitude == null) {
      state = state.toError('GPS 좌표를 가져올 수 없습니다');
      return;
    }

    state = state.toRegistering();

    try {
      final result = await _registerAttendance.call(
        scheduleId: scheduleId,
        latitude: latitude,
        longitude: longitude,
      );

      state = state.copyWith(
        isRegistering: false,
        registrationResult: result,
        registeredCount: result.registeredCount,
        totalCount: result.totalCount,
        errorMessage: null,
      );
    } on ArgumentError catch (e) {
      state = state.toError(e.message as String);
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 다음 등록 (등록 완료 후 목록으로 복귀)
  Future<void> prepareNextRegistration() async {
    state = state.copyWith(
      selectedScheduleId: null,
      searchKeyword: '',
    );

    // 거래처 목록 새로고침
    await loadAccounts();
  }

  /// 등록 결과 초기화
  void clearRegistrationResult() {
    state = AttendanceState(
      allAccounts: state.allAccounts,
      filteredAccounts: state.allAccounts,
      totalCount: state.totalCount,
      registeredCount: state.registeredCount,
    );
  }

  /// 출근등록 현황 조회
  Future<void> loadAttendanceStatus() async {
    try {
      final result = await _getAttendanceStatus.call();

      state = state.copyWith(
        statusList: result.statusList,
        totalCount: result.totalCount,
        registeredCount: result.registeredCount,
      );
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

/// Attendance StateNotifier Provider
final attendanceProvider =
    StateNotifierProvider<AttendanceNotifier, AttendanceState>((ref) {
  return AttendanceNotifier(
    getAccountList: ref.watch(getAccountListUseCaseProvider),
    registerAttendance: ref.watch(registerAttendanceUseCaseProvider),
    getAttendanceStatus: ref.watch(getAttendanceStatusUseCaseProvider),
  );
});
