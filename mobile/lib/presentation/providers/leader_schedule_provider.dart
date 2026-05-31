import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/leader_schedule_api_datasource.dart';
import '../../data/repositories/leader_schedule_repository_impl.dart';
import '../../domain/entities/leader_account.dart';
import '../../domain/entities/leader_daily_status.dart';
import '../../domain/entities/leader_team_member.dart';
import '../../domain/repositories/leader_schedule_repository.dart';
import '../../domain/usecases/create_team_member_schedule_usecase.dart';

// ============================================
// 1. Dependency Providers
// ============================================

final leaderScheduleRepositoryProvider =
    Provider<LeaderScheduleRepository>((ref) {
  final dio = ref.watch(dioProvider);
  return LeaderScheduleRepositoryImpl(LeaderScheduleApiDataSource(dio));
});

final createTeamMemberScheduleUseCaseProvider =
    Provider<CreateTeamMemberScheduleUseCase>((ref) {
  return CreateTeamMemberScheduleUseCase(
    ref.watch(leaderScheduleRepositoryProvider),
  );
});

// ============================================
// 2. 팀원 목록 State + Notifier
// ============================================

class LeaderTeamMembersState {
  final bool isLoading;
  final String? errorMessage;
  final List<LeaderTeamMember> members;
  final bool hasLoaded;

  const LeaderTeamMembersState({
    this.isLoading = false,
    this.errorMessage,
    this.members = const [],
    this.hasLoaded = false,
  });

  bool get isEmpty => hasLoaded && members.isEmpty;

  LeaderTeamMembersState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
    List<LeaderTeamMember>? members,
    bool? hasLoaded,
  }) {
    return LeaderTeamMembersState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      members: members ?? this.members,
      hasLoaded: hasLoaded ?? this.hasLoaded,
    );
  }
}

class LeaderTeamMembersNotifier extends StateNotifier<LeaderTeamMembersState> {
  final LeaderScheduleRepository _repository;

  LeaderTeamMembersNotifier(this._repository)
      : super(const LeaderTeamMembersState());

  Future<void> load() async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final members = await _repository.getTeamMembers();
      state = state.copyWith(
        isLoading: false,
        members: members,
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

  void clearError() => state = state.copyWith(clearError: true);
}

final leaderTeamMembersProvider = StateNotifierProvider.autoDispose<
    LeaderTeamMembersNotifier, LeaderTeamMembersState>((ref) {
  return LeaderTeamMembersNotifier(
    ref.watch(leaderScheduleRepositoryProvider),
  );
});

// ============================================
// 3. 일정 등록 State + Notifier
// ============================================

class LeaderScheduleCreateState {
  final bool isLoading;
  final String? errorMessage;
  final bool isSubmitted;
  final DateTime? workingDate;
  final LeaderAccount? selectedAccount;
  final String? workingCategory3;
  final String? workingCategory1;
  final List<LeaderAccount> accounts;
  final bool isAccountsLoading;
  final String? accountsError;

  const LeaderScheduleCreateState({
    this.isLoading = false,
    this.errorMessage,
    this.isSubmitted = false,
    this.workingDate,
    this.selectedAccount,
    this.workingCategory3,
    this.workingCategory1,
    this.accounts = const [],
    this.isAccountsLoading = false,
    this.accountsError,
  });

  bool get canSubmit =>
      workingDate != null &&
      selectedAccount != null &&
      workingCategory3 != null &&
      workingCategory3!.isNotEmpty &&
      !isLoading;

  LeaderScheduleCreateState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
    bool? isSubmitted,
    DateTime? workingDate,
    LeaderAccount? selectedAccount,
    bool clearSelectedAccount = false,
    String? workingCategory3,
    bool clearCategory3 = false,
    String? workingCategory1,
    bool clearCategory1 = false,
    List<LeaderAccount>? accounts,
    bool? isAccountsLoading,
    String? accountsError,
    bool clearAccountsError = false,
  }) {
    return LeaderScheduleCreateState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      isSubmitted: isSubmitted ?? this.isSubmitted,
      workingDate: workingDate ?? this.workingDate,
      selectedAccount: clearSelectedAccount
          ? null
          : (selectedAccount ?? this.selectedAccount),
      workingCategory3:
          clearCategory3 ? null : (workingCategory3 ?? this.workingCategory3),
      workingCategory1:
          clearCategory1 ? null : (workingCategory1 ?? this.workingCategory1),
      accounts: accounts ?? this.accounts,
      isAccountsLoading: isAccountsLoading ?? this.isAccountsLoading,
      accountsError:
          clearAccountsError ? null : (accountsError ?? this.accountsError),
    );
  }
}

class LeaderScheduleCreateNotifier
    extends StateNotifier<LeaderScheduleCreateState> {
  final LeaderScheduleRepository _repository;
  final CreateTeamMemberScheduleUseCase _createUseCase;
  final int _targetEmployeeId;

  LeaderScheduleCreateNotifier({
    required LeaderScheduleRepository repository,
    required CreateTeamMemberScheduleUseCase createUseCase,
    required int targetEmployeeId,
  })  : _repository = repository,
        _createUseCase = createUseCase,
        _targetEmployeeId = targetEmployeeId,
        super(const LeaderScheduleCreateState());

  Future<void> loadAccounts({String? keyword}) async {
    state = state.copyWith(isAccountsLoading: true, clearAccountsError: true);
    try {
      final accounts = await _repository.getAccounts(keyword: keyword);
      state = state.copyWith(
        isAccountsLoading: false,
        accounts: accounts,
      );
    } catch (e) {
      state = state.copyWith(
        isAccountsLoading: false,
        accountsError: extractErrorMessage(e),
      );
    }
  }

  void selectWorkingDate(DateTime date) {
    state = state.copyWith(workingDate: date, clearError: true);
  }

  void selectAccount(LeaderAccount? account) {
    state = state.copyWith(
      selectedAccount: account,
      clearSelectedAccount: account == null,
      clearError: true,
    );
  }

  void selectCategory3(String? category3) {
    state = state.copyWith(
      workingCategory3: category3,
      clearCategory3: category3 == null,
      clearError: true,
    );
  }

  void selectCategory1(String? category1) {
    state = state.copyWith(
      workingCategory1: category1,
      clearCategory1: category1 == null,
      clearError: true,
    );
  }

  Future<void> submit() async {
    if (!state.canSubmit) return;
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      await _createUseCase.call(
        targetEmployeeId: _targetEmployeeId,
        workingDate: state.workingDate,
        accountId: state.selectedAccount?.id,
        workingCategory3: state.workingCategory3,
        workingCategory1: state.workingCategory1,
      );
      state = state.copyWith(isLoading: false, isSubmitted: true);
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: extractErrorMessage(e),
      );
    }
  }

  void clearError() => state = state.copyWith(clearError: true);
}

/// 일정 등록 화면용 Provider — 대상 팀원 ID 를 family 인자로 받음.
final leaderScheduleCreateProvider = StateNotifierProvider.autoDispose
    .family<LeaderScheduleCreateNotifier, LeaderScheduleCreateState, int>(
        (ref, targetEmployeeId) {
  return LeaderScheduleCreateNotifier(
    repository: ref.watch(leaderScheduleRepositoryProvider),
    createUseCase: ref.watch(createTeamMemberScheduleUseCaseProvider),
    targetEmployeeId: targetEmployeeId,
  );
});

// ============================================
// 4. 여사원 일별 현황 State + Notifier (조회 전용)
// ============================================

/// 여사원 일별 현황 화면 상태. 검색은 로드된 데이터에 대해 클라이언트 측 필터링.
class LeaderDailyStatusState {
  final DateTime selectedDate;
  final bool isLoading;
  final String? errorMessage;
  final LeaderDailyStatus? data;
  final String searchKeyword;
  final bool hasLoaded;

  const LeaderDailyStatusState({
    required this.selectedDate,
    this.isLoading = false,
    this.errorMessage,
    this.data,
    this.searchKeyword = '',
    this.hasLoaded = false,
  });

  bool _matches(String name, String code, String account) {
    if (searchKeyword.isEmpty) return true;
    final needle = searchKeyword.toLowerCase();
    return name.toLowerCase().contains(needle) ||
        code.toLowerCase().contains(needle) ||
        account.toLowerCase().contains(needle);
  }

  List<LeaderDailyWorker> get filteredDisplayWorkers =>
      (data?.displayWorkers ?? const [])
          .where((w) => _matches(w.employeeName, w.employeeCode, w.accountName))
          .toList();

  List<LeaderDailyWorker> get filteredEventWorkers =>
      (data?.eventWorkers ?? const [])
          .where((w) => _matches(w.employeeName, w.employeeCode, w.accountName))
          .toList();

  List<LeaderDailyEmployee> get filteredAnnualLeaveWorkers =>
      (data?.annualLeaveWorkers ?? const [])
          .where((e) => _matches(e.employeeName, e.employeeCode, ''))
          .toList();

  LeaderDailyStatusState copyWith({
    DateTime? selectedDate,
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
    LeaderDailyStatus? data,
    String? searchKeyword,
    bool? hasLoaded,
  }) {
    return LeaderDailyStatusState(
      selectedDate: selectedDate ?? this.selectedDate,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      data: data ?? this.data,
      searchKeyword: searchKeyword ?? this.searchKeyword,
      hasLoaded: hasLoaded ?? this.hasLoaded,
    );
  }
}

class LeaderDailyStatusNotifier extends StateNotifier<LeaderDailyStatusState> {
  final LeaderScheduleRepository _repository;

  LeaderDailyStatusNotifier(this._repository)
      : super(LeaderDailyStatusState(selectedDate: _today()));

  static DateTime _today() {
    final now = DateTime.now();
    return DateTime(now.year, now.month, now.day);
  }

  /// 선택 날짜 기준 일별 현황 조회. 실패 시 errorMessage 설정, 항상 hasLoaded=true.
  Future<void> load() async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final data = await _repository.getDailyStatus(state.selectedDate);
      state = state.copyWith(
        isLoading: false,
        data: data,
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

  /// 날짜 변경 후 재조회.
  Future<void> changeDate(DateTime date) async {
    state = state.copyWith(selectedDate: date);
    await load();
  }

  /// 검색어 설정 — 로드된 데이터에 대한 클라이언트 측 필터에만 사용(재조회 없음).
  void setSearchKeyword(String keyword) {
    state = state.copyWith(searchKeyword: keyword);
  }

  /// 진열 일정 거래처 변경(P7) 후 재조회. 성공 시 true, 실패 시 errorMessage 설정 후 false.
  /// 진행 중(isLoading)이면 재진입을 차단한다.
  Future<bool> updateScheduleAccount(int scheduleId, int accountId) async {
    if (state.isLoading) return false;
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      await _repository.updateScheduleAccount(
        scheduleId: scheduleId,
        accountId: accountId,
      );
      await load(); // load 가 isLoading 정리 + 재조회
      return true;
    } catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: extractErrorMessage(e));
      return false;
    }
  }

  /// 진열 일정 삭제(P7) 후 재조회. 성공 시 true, 실패 시 errorMessage 설정 후 false.
  /// 진행 중(isLoading)이면 재진입을 차단한다.
  Future<bool> deleteSchedule(int scheduleId) async {
    if (state.isLoading) return false;
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      await _repository.deleteSchedule(scheduleId);
      await load(); // load 가 isLoading 정리 + 재조회
      return true;
    } catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: extractErrorMessage(e));
      return false;
    }
  }

  /// 에러 메시지 1회성 소비(SnackBar 표시 후 호출).
  void clearError() => state = state.copyWith(clearError: true);
}

/// 여사원 일별 현황 Provider — 진입 시 오늘 날짜로 초기화.
final leaderDailyStatusProvider = StateNotifierProvider.autoDispose<
    LeaderDailyStatusNotifier, LeaderDailyStatusState>(
  (ref) => LeaderDailyStatusNotifier(
    ref.watch(leaderScheduleRepositoryProvider),
  ),
);
