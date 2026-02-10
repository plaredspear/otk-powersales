import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/mock/attendance_mock_repository.dart';
import '../../domain/repositories/attendance_repository.dart';
import '../../domain/usecases/get_attendance_status.dart';
import '../../domain/usecases/get_store_list.dart';
import '../../domain/usecases/register_attendance.dart';
import 'attendance_state.dart';

// --- Dependency Providers ---

final attendanceRepositoryProvider = Provider<AttendanceRepository>((ref) {
  return AttendanceMockRepository();
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
        workerType: result.workerType,
        allStores: result.stores,
        filteredStores: result.stores,
        totalCount: result.totalCount,
        registeredCount: result.registeredCount,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 거래처 검색
  void searchStores(String keyword) {
    final lowerKeyword = keyword.toLowerCase();
    final filtered = state.allStores.where((store) {
      if (keyword.isEmpty) return true;
      return store.storeName.toLowerCase().contains(lowerKeyword) ||
          store.address.toLowerCase().contains(lowerKeyword) ||
          store.storeCode.toLowerCase().contains(lowerKeyword);
    }).toList();

    state = state.copyWith(
      searchKeyword: keyword,
      filteredStores: filtered,
      selectedStoreId: null,
    );
  }

  /// 근무유형 선택
  void selectWorkType(String workType) {
    state = state.copyWith(selectedWorkType: workType);
  }

  /// 거래처 선택
  void selectStore(int storeId) {
    state = state.copyWith(selectedStoreId: storeId);
  }

  /// 출근등록
  Future<void> register() async {
    final storeId = state.selectedStoreId;
    if (storeId == null) return;

    state = state.toRegistering();

    try {
      final result = await _registerAttendance.call(
        storeId: storeId,
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
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 다음 등록 (등록 완료 후 목록으로 복귀)
  Future<void> prepareNextRegistration() async {
    state = state.copyWith(
      selectedStoreId: null,
      searchKeyword: '',
    );

    // 거래처 목록 새로고침
    await loadStores();
  }

  /// 등록 결과 초기화
  void clearRegistrationResult() {
    state = AttendanceState(
      workerType: state.workerType,
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
        e.toString().replaceFirst('Exception: ', ''),
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
