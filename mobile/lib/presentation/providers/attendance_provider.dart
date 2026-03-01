import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/attendance_api_datasource.dart';
import '../../data/repositories/attendance_repository_impl.dart';
import '../../domain/repositories/attendance_repository.dart';
import '../../domain/usecases/get_attendance_status.dart';
import '../../domain/usecases/get_store_list.dart';
import '../../domain/usecases/register_attendance.dart';
import 'attendance_state.dart';

// --- Dependency Providers ---

final attendanceRepositoryProvider = Provider<AttendanceRepository>((ref) {
  final dio = ref.watch(dioProvider);
  final dataSource = AttendanceApiDataSource(dio);
  return AttendanceRepositoryImpl(dataSource: dataSource);
});

final getStoreListUseCaseProvider = Provider<GetStoreList>((ref) {
  final repository = ref.watch(attendanceRepositoryProvider);
  return GetStoreList(repository);
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
  final GetStoreList _getStoreList;
  final RegisterAttendance _registerAttendance;
  final GetAttendanceStatus _getAttendanceStatus;

  AttendanceNotifier({
    required GetStoreList getStoreList,
    required RegisterAttendance registerAttendance,
    required GetAttendanceStatus getAttendanceStatus,
  })  : _getStoreList = getStoreList,
        _registerAttendance = registerAttendance,
        _getAttendanceStatus = getAttendanceStatus,
        super(AttendanceState.initial());

  /// 거래처 목록 로딩
  Future<void> loadStores() async {
    state = state.toLoading();

    try {
      final result = await _getStoreList.call();

      state = state.copyWith(
        isLoading: false,
        allStores: result.stores,
        filteredStores: result.stores,
        totalCount: result.totalCount,
        registeredCount: result.registeredCount,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 거래처 검색
  void searchStores(String keyword) {
    final lowerKeyword = keyword.toLowerCase();
    final filtered = state.allStores.where((store) {
      if (keyword.isEmpty) return true;
      return store.storeName.toLowerCase().contains(lowerKeyword) ||
          store.address.toLowerCase().contains(lowerKeyword);
    }).toList();

    state = state.copyWith(
      searchKeyword: keyword,
      filteredStores: filtered,
      selectedScheduleSfid: null,
    );
  }

  /// 근무유형 선택
  void selectWorkType(String workType) {
    state = state.copyWith(selectedWorkType: workType);
  }

  /// 거래처 선택
  void selectStore(String scheduleSfid) {
    state = state.copyWith(selectedScheduleSfid: scheduleSfid);
  }

  /// 출근등록
  Future<void> register({double? latitude, double? longitude}) async {
    final scheduleSfid = state.selectedScheduleSfid;
    if (scheduleSfid == null) return;

    if (latitude == null || longitude == null) {
      state = state.toError('GPS 좌표를 가져올 수 없습니다');
      return;
    }

    state = state.toRegistering();

    try {
      final result = await _registerAttendance.call(
        scheduleSfid: scheduleSfid,
        latitude: latitude,
        longitude: longitude,
        workType: state.selectedWorkType,
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
      selectedScheduleSfid: null,
      searchKeyword: '',
    );

    // 거래처 목록 새로고침
    await loadStores();
  }

  /// 등록 결과 초기화
  void clearRegistrationResult() {
    state = AttendanceState(
      allStores: state.allStores,
      filteredStores: state.allStores,
      totalCount: state.totalCount,
      registeredCount: state.registeredCount,
      selectedWorkType: state.selectedWorkType,
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
    getStoreList: ref.watch(getStoreListUseCaseProvider),
    registerAttendance: ref.watch(registerAttendanceUseCaseProvider),
    getAttendanceStatus: ref.watch(getAttendanceStatusUseCaseProvider),
  );
});
