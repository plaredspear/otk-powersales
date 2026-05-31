import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/leader_account.dart';
import 'package:mobile/domain/entities/leader_daily_status.dart';
import 'package:mobile/domain/entities/leader_schedule_created.dart';
import 'package:mobile/domain/entities/leader_team_member.dart';
import 'package:mobile/domain/repositories/leader_schedule_repository.dart';
import 'package:mobile/domain/usecases/create_team_member_schedule_usecase.dart';
import 'package:mobile/presentation/providers/leader_schedule_provider.dart';

/// 테스트용 Fake LeaderScheduleRepository.
class FakeLeaderScheduleRepository implements LeaderScheduleRepository {
  List<LeaderTeamMember> teamMembers = [];
  List<LeaderAccount> accounts = [];
  LeaderScheduleCreated? createResult;
  Object? exceptionToThrow;

  String? lastKeyword;
  int? lastTargetEmployeeId;
  DateTime? lastWorkingDate;
  int? lastAccountId;
  String? lastWorkingCategory3;
  String? lastWorkingCategory1;

  @override
  Future<List<LeaderTeamMember>> getTeamMembers() async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return teamMembers;
  }

  @override
  Future<List<LeaderAccount>> getAccounts({String? keyword}) async {
    lastKeyword = keyword;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return accounts;
  }

  @override
  Future<LeaderDailyStatus> getDailyStatus(DateTime date) async {
    throw UnimplementedError();
  }

  @override
  Future<void> updateScheduleAccount({
    required int scheduleId,
    required int accountId,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<void> deleteSchedule(int scheduleId) async {
    throw UnimplementedError();
  }

  @override
  Future<LeaderScheduleCreated> createTeamMemberSchedule({
    required int targetEmployeeId,
    required DateTime workingDate,
    required int accountId,
    required String workingCategory3,
    String? workingCategory1,
  }) async {
    lastTargetEmployeeId = targetEmployeeId;
    lastWorkingDate = workingDate;
    lastAccountId = accountId;
    lastWorkingCategory3 = workingCategory3;
    lastWorkingCategory1 = workingCategory1;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return createResult!;
  }
}

void main() {
  late FakeLeaderScheduleRepository fakeRepo;
  late CreateTeamMemberScheduleUseCase useCase;

  setUp(() {
    fakeRepo = FakeLeaderScheduleRepository();
    useCase = CreateTeamMemberScheduleUseCase(fakeRepo);
  });

  group('LeaderTeamMember.isInactive', () {
    test('휴직 status -> isInactive=true', () {
      final m = const LeaderTeamMember(
        id: 1,
        employeeCode: '20300001',
        name: '팀원',
        status: '휴직',
        costCenterCode: 'C001',
      );
      expect(m.isInactive, true);
    });

    test('퇴직 status -> isInactive=true', () {
      final m = const LeaderTeamMember(
        id: 1,
        employeeCode: '20300001',
        name: '팀원',
        status: '퇴직',
        costCenterCode: 'C001',
      );
      expect(m.isInactive, true);
    });

    test('활동 status -> isInactive=false', () {
      final m = const LeaderTeamMember(
        id: 1,
        employeeCode: '20300001',
        name: '팀원',
        status: '활동',
        costCenterCode: 'C001',
      );
      expect(m.isInactive, false);
    });
  });

  group('LeaderTeamMembersNotifier', () {
    test('초기 상태 - empty', () {
      final notifier = LeaderTeamMembersNotifier(fakeRepo);
      expect(notifier.state.isLoading, false);
      expect(notifier.state.members, isEmpty);
      expect(notifier.state.hasLoaded, false);
    });

    test('load 성공 -> members 설정', () async {
      fakeRepo.teamMembers = [
        const LeaderTeamMember(
          id: 1,
          employeeCode: '20300001',
          name: '팀원1',
          status: '활동',
          costCenterCode: 'C001',
        ),
      ];
      final notifier = LeaderTeamMembersNotifier(fakeRepo);
      await notifier.load();
      expect(notifier.state.members.length, 1);
      expect(notifier.state.hasLoaded, true);
      expect(notifier.state.errorMessage, isNull);
    });

    test('load 실패 -> errorMessage 설정', () async {
      fakeRepo.exceptionToThrow = Exception('서버 오류');
      final notifier = LeaderTeamMembersNotifier(fakeRepo);
      await notifier.load();
      expect(notifier.state.isLoading, false);
      expect(notifier.state.errorMessage, contains('서버 오류'));
      expect(notifier.state.hasLoaded, true);
    });

    test('load API 403 NOT_LEADER -> 한국어 메시지', () async {
      fakeRepo.exceptionToThrow = DioException(
        requestOptions: RequestOptions(path: '/api/v1/mobile/leader/team-members'),
        response: Response(
          requestOptions: RequestOptions(path: '/api/v1/mobile/leader/team-members'),
          statusCode: 403,
          data: {
            'success': false,
            'error': {'code': 'NOT_LEADER', 'message': '조장 권한이 필요합니다.'},
          },
        ),
        type: DioExceptionType.badResponse,
      );
      final notifier = LeaderTeamMembersNotifier(fakeRepo);
      await notifier.load();
      expect(notifier.state.errorMessage, '조장 권한이 필요합니다.');
    });
  });

  group('LeaderScheduleCreateNotifier - 입력 검증 + canSubmit', () {
    LeaderScheduleCreateNotifier makeNotifier() => LeaderScheduleCreateNotifier(
          repository: fakeRepo,
          createUseCase: useCase,
          targetEmployeeId: 5012,
        );

    test('초기에는 canSubmit=false', () {
      expect(makeNotifier().state.canSubmit, false);
    });

    test('일자만 선택 -> 거래처/카테고리3 누락이라 canSubmit=false', () {
      final n = makeNotifier();
      n.selectWorkingDate(DateTime(2026, 5, 15));
      expect(n.state.canSubmit, false);
    });

    test('모든 필수 입력 -> canSubmit=true', () {
      final n = makeNotifier();
      n.selectWorkingDate(DateTime(2026, 5, 15));
      n.selectAccount(_account());
      n.selectCategory3('고정');
      expect(n.state.canSubmit, true);
    });

    test('거래처 누락 -> canSubmit=false', () {
      final n = makeNotifier();
      n.selectWorkingDate(DateTime(2026, 5, 15));
      n.selectCategory3('고정');
      expect(n.state.canSubmit, false);
    });

    test('카테고리3 누락 -> canSubmit=false', () {
      final n = makeNotifier();
      n.selectWorkingDate(DateTime(2026, 5, 15));
      n.selectAccount(_account());
      expect(n.state.canSubmit, false);
    });
  });

  group('LeaderScheduleCreateNotifier - submit', () {
    LeaderScheduleCreateNotifier makeNotifier() => LeaderScheduleCreateNotifier(
          repository: fakeRepo,
          createUseCase: useCase,
          targetEmployeeId: 5012,
        );

    test('정상 등록 -> isSubmitted=true + repo 호출 인자 검증', () async {
      fakeRepo.createResult = LeaderScheduleCreated(
        scheduleId: 78901,
        targetEmployeeId: 5012,
        workingDate: DateTime(2026, 5, 15),
        workingType: '근무',
        workingCategory3: '고정',
        proxyRegisteredBy: 4001,
      );
      final n = makeNotifier();
      n.selectWorkingDate(DateTime(2026, 5, 15));
      n.selectAccount(_account());
      n.selectCategory3('고정');
      n.selectCategory1('진열');

      await n.submit();

      expect(n.state.isSubmitted, true);
      expect(n.state.isLoading, false);
      expect(fakeRepo.lastTargetEmployeeId, 5012);
      expect(fakeRepo.lastAccountId, 90234);
      expect(fakeRepo.lastWorkingCategory3, '고정');
      expect(fakeRepo.lastWorkingCategory1, '진열');
    });

    test('canSubmit=false 일 때 submit 호출 -> repo 미호출', () async {
      final n = makeNotifier();
      await n.submit();
      expect(fakeRepo.lastTargetEmployeeId, isNull);
      expect(n.state.isSubmitted, false);
    });

    test('서버 409 DUPLICATE_WORK_SCHEDULE -> 한국어 SnackBar 메시지', () async {
      fakeRepo.exceptionToThrow = DioException(
        requestOptions: RequestOptions(path: '/api/v1/mobile/leader/team-member-schedule'),
        response: Response(
          requestOptions: RequestOptions(path: '/api/v1/mobile/leader/team-member-schedule'),
          statusCode: 409,
          data: {
            'success': false,
            'error': {
              'code': 'DUPLICATE_WORK_SCHEDULE',
              'message': '해당 날짜에 예정된 근무 일정이 존재합니다.',
            },
          },
        ),
        type: DioExceptionType.badResponse,
      );
      final n = makeNotifier();
      n.selectWorkingDate(DateTime(2026, 5, 15));
      n.selectAccount(_account());
      n.selectCategory3('고정');

      await n.submit();

      expect(n.state.isSubmitted, false);
      expect(n.state.errorMessage, '해당 날짜에 예정된 근무 일정이 존재합니다.');
    });

    test('서버 409 CATEGORY3_CONFLICT -> 한국어 SnackBar 메시지', () async {
      fakeRepo.exceptionToThrow = DioException(
        requestOptions: RequestOptions(path: '/api/v1/mobile/leader/team-member-schedule'),
        response: Response(
          requestOptions: RequestOptions(path: '/api/v1/mobile/leader/team-member-schedule'),
          statusCode: 409,
          data: {
            'success': false,
            'error': {
              'code': 'CATEGORY3_CONFLICT',
              'message': '동일 날짜와 직원으로 다른 유형의 일정이 존재합니다.',
            },
          },
        ),
        type: DioExceptionType.badResponse,
      );
      final n = makeNotifier();
      n.selectWorkingDate(DateTime(2026, 5, 15));
      n.selectAccount(_account());
      n.selectCategory3('고정');

      await n.submit();

      expect(n.state.errorMessage, '동일 날짜와 직원으로 다른 유형의 일정이 존재합니다.');
    });
  });

  group('LeaderScheduleCreateNotifier.loadAccounts', () {
    test('keyword 전달', () async {
      fakeRepo.accounts = [_account()];
      final n = LeaderScheduleCreateNotifier(
        repository: fakeRepo,
        createUseCase: useCase,
        targetEmployeeId: 5012,
      );
      await n.loadAccounts(keyword: 'alpha');
      expect(fakeRepo.lastKeyword, 'alpha');
      expect(n.state.accounts.length, 1);
      expect(n.state.isAccountsLoading, false);
    });

    test('실패 -> accountsError 설정', () async {
      fakeRepo.exceptionToThrow = Exception('네트워크 오류');
      final n = LeaderScheduleCreateNotifier(
        repository: fakeRepo,
        createUseCase: useCase,
        targetEmployeeId: 5012,
      );
      await n.loadAccounts();
      expect(n.state.accountsError, contains('네트워크 오류'));
      expect(n.state.isAccountsLoading, false);
    });
  });

  group('CreateTeamMemberScheduleUseCase 입력 검증', () {
    test('일자 누락 -> ArgumentError', () {
      expect(
        () => useCase.call(
          targetEmployeeId: 5012,
          workingDate: null,
          accountId: 90234,
          workingCategory3: '고정',
        ),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('거래처 누락 -> ArgumentError', () {
      expect(
        () => useCase.call(
          targetEmployeeId: 5012,
          workingDate: DateTime(2026, 5, 15),
          accountId: null,
          workingCategory3: '고정',
        ),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('카테고리3 누락 -> ArgumentError', () {
      expect(
        () => useCase.call(
          targetEmployeeId: 5012,
          workingDate: DateTime(2026, 5, 15),
          accountId: 90234,
          workingCategory3: null,
        ),
        throwsA(isA<ArgumentError>()),
      );
    });
  });
}

LeaderAccount _account() => const LeaderAccount(
      id: 90234,
      name: 'AlphaMart',
      address1: 'Seoul',
      branchCode: 'C001',
      accountGroup: '1000',
      accountType: 'TYPE_A',
    );
