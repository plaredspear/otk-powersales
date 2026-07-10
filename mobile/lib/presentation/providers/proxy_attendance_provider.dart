import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/proxy_attendance_api_datasource.dart';
import '../../data/repositories/proxy_attendance_repository_impl.dart';
import '../../domain/entities/branch_option.dart';
import '../../domain/entities/leader_daily_status.dart';
import '../../domain/repositories/proxy_attendance_repository.dart';

// ============================================
// 1. Dependency Providers
// ============================================

final proxyAttendanceRepositoryProvider =
    Provider<ProxyAttendanceRepository>((ref) {
  final dio = ref.watch(dioProvider);
  return ProxyAttendanceRepositoryImpl(ProxyAttendanceApiDataSource(dio));
});

// ============================================
// 2. 지점 목록 State + Notifier
// ============================================

/// 대리출근 지점 선택 옵션 목록 상태.
class ProxyBranchesState {
  final bool isLoading;
  final String? errorMessage;
  final List<BranchOption> branches;
  final bool hasLoaded;

  const ProxyBranchesState({
    this.isLoading = false,
    this.errorMessage,
    this.branches = const [],
    this.hasLoaded = false,
  });

  ProxyBranchesState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
    List<BranchOption>? branches,
    bool? hasLoaded,
  }) {
    return ProxyBranchesState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      branches: branches ?? this.branches,
      hasLoaded: hasLoaded ?? this.hasLoaded,
    );
  }
}

class ProxyBranchesNotifier extends StateNotifier<ProxyBranchesState> {
  final ProxyAttendanceRepository _repository;

  ProxyBranchesNotifier(this._repository) : super(const ProxyBranchesState());

  /// 지점 목록 조회. 실패 시 errorMessage 설정, 항상 hasLoaded=true.
  Future<void> load() async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final branches = await _repository.getBranches();
      state = state.copyWith(
        isLoading: false,
        branches: branches,
        hasLoaded: true,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: extractErrorMessage(e),
        hasLoaded: true,
      );
    }
  }
}

final proxyBranchesProvider = StateNotifierProvider.autoDispose<
    ProxyBranchesNotifier, ProxyBranchesState>((ref) {
  return ProxyBranchesNotifier(ref.watch(proxyAttendanceRepositoryProvider));
});

// ============================================
// 3. 일별 현황 State + Notifier (지점 선택형)
// ============================================

/// 대리출근 일별 현황 화면 상태.
///
/// 조장 화면과 달리 [selectedBranch] 가 없으면(초기) 조회하지 않고 빈 화면을 표시한다.
class ProxyAttendanceState {
  final BranchOption? selectedBranch;
  final DateTime selectedDate;
  final bool isLoading;
  final String? errorMessage;
  final LeaderDailyStatus? data;
  final bool hasLoaded;

  const ProxyAttendanceState({
    this.selectedBranch,
    required this.selectedDate,
    this.isLoading = false,
    this.errorMessage,
    this.data,
    this.hasLoaded = false,
  });

  ProxyAttendanceState copyWith({
    BranchOption? selectedBranch,
    DateTime? selectedDate,
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
    LeaderDailyStatus? data,
    bool clearData = false,
    bool? hasLoaded,
  }) {
    return ProxyAttendanceState(
      selectedBranch: selectedBranch ?? this.selectedBranch,
      selectedDate: selectedDate ?? this.selectedDate,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      data: clearData ? null : (data ?? this.data),
      hasLoaded: hasLoaded ?? this.hasLoaded,
    );
  }
}

class ProxyAttendanceNotifier extends StateNotifier<ProxyAttendanceState> {
  final ProxyAttendanceRepository _repository;

  ProxyAttendanceNotifier(this._repository)
      : super(ProxyAttendanceState(selectedDate: _today()));

  static DateTime _today() {
    final now = DateTime.now();
    return DateTime(now.year, now.month, now.day);
  }

  /// 지점 변경 후 일별 현황 재조회.
  Future<void> selectBranch(BranchOption branch) async {
    state = state.copyWith(selectedBranch: branch);
    await load();
  }

  /// 선택 지점·날짜 기준 일별 현황 조회. 지점 미선택 시 조회하지 않음(빈 화면).
  Future<void> load() async {
    final branch = state.selectedBranch;
    if (branch == null) return;
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final data = await _repository.getDailyStatus(
        branch.branchCode,
        state.selectedDate,
      );
      state = state.copyWith(isLoading: false, data: data, hasLoaded: true);
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: extractErrorMessage(e),
        hasLoaded: true,
      );
    }
  }

  /// 날짜 변경 후 재조회.
  Future<void> changeDate(DateTime date) async {
    state = state.copyWith(selectedDate: date);
    await load();
  }

  /// 대리출근 등록 후 재조회. 성공 시 null, 실패 시 에러 메시지 반환.
  /// 진열=[displayWorkScheduleId], 행사·기배정=[scheduleId] 중 하나 전달.
  Future<String?> registerProxyAttendance({
    required int targetEmployeeId,
    int? scheduleId,
    int? displayWorkScheduleId,
  }) async {
    final branch = state.selectedBranch;
    if (branch == null) return '지점을 먼저 선택해주세요.';
    try {
      await _repository.registerProxyAttendance(
        branchCode: branch.branchCode,
        targetEmployeeId: targetEmployeeId,
        scheduleId: scheduleId,
        displayWorkScheduleId: displayWorkScheduleId,
      );
      await load();
      return null;
    } catch (e) {
      return extractErrorMessage(e);
    }
  }

  /// 에러 메시지 1회성 소비(SnackBar 표시 후 호출).
  void clearError() => state = state.copyWith(clearError: true);
}

final proxyAttendanceProvider = StateNotifierProvider.autoDispose<
    ProxyAttendanceNotifier, ProxyAttendanceState>(
  (ref) => ProxyAttendanceNotifier(
    ref.watch(proxyAttendanceRepositoryProvider),
  ),
);
