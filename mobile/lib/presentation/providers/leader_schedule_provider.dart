import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/leader_schedule_api_datasource.dart';
import '../../data/repositories/leader_schedule_repository_impl.dart';
import '../../domain/entities/leader_account.dart';
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
