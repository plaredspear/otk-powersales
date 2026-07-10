import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/branch_option.dart';
import 'package:mobile/domain/entities/leader_daily_status.dart';
import 'package:mobile/domain/entities/leader_team_member.dart';
import 'package:mobile/domain/repositories/proxy_attendance_repository.dart';
import 'package:mobile/presentation/providers/proxy_attendance_provider.dart';

/// 테스트용 Fake ProxyAttendanceRepository.
class FakeProxyAttendanceRepository implements ProxyAttendanceRepository {
  List<BranchOption> branches = [];
  LeaderDailyStatus? dailyStatus;
  Object? exceptionToThrow;

  // 마지막 호출 인자 기록
  String? lastBranchCodeForMembers;
  String? lastBranchCodeForStatus;
  DateTime? lastDate;
  String? lastRegisterBranchCode;
  int? lastRegisterTargetEmployeeId;
  int? lastRegisterScheduleId;
  int? lastRegisterDisplayWorkScheduleId;
  int registerCallCount = 0;

  @override
  Future<List<BranchOption>> getBranches() async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return branches;
  }

  @override
  Future<List<LeaderTeamMember>> getTeamMembers(String branchCode) async {
    lastBranchCodeForMembers = branchCode;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return const [];
  }

  @override
  Future<LeaderDailyStatus> getDailyStatus(
    String branchCode,
    DateTime date,
  ) async {
    lastBranchCodeForStatus = branchCode;
    lastDate = date;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return dailyStatus ??
        LeaderDailyStatus(
          date: date,
          summary: const LeaderDailyStatusSummary(),
        );
  }

  @override
  Future<void> registerProxyAttendance({
    required String branchCode,
    required int targetEmployeeId,
    int? scheduleId,
    int? displayWorkScheduleId,
  }) async {
    registerCallCount++;
    lastRegisterBranchCode = branchCode;
    lastRegisterTargetEmployeeId = targetEmployeeId;
    lastRegisterScheduleId = scheduleId;
    lastRegisterDisplayWorkScheduleId = displayWorkScheduleId;
    if (exceptionToThrow != null) throw exceptionToThrow!;
  }
}

void main() {
  const branch = BranchOption(branchCode: '5832', branchName: '원주1지점');

  group('ProxyBranchesNotifier', () {
    test('load 성공 → 지점 목록 세팅', () async {
      final repo = FakeProxyAttendanceRepository()..branches = [branch];
      final notifier = ProxyBranchesNotifier(repo);

      await notifier.load();

      expect(notifier.state.branches, [branch]);
      expect(notifier.state.hasLoaded, isTrue);
      expect(notifier.state.errorMessage, isNull);
    });

    test('load 실패 → errorMessage 세팅', () async {
      final repo = FakeProxyAttendanceRepository()
        ..exceptionToThrow = Exception('네트워크 오류');
      final notifier = ProxyBranchesNotifier(repo);

      await notifier.load();

      expect(notifier.state.errorMessage, isNotNull);
      expect(notifier.state.hasLoaded, isTrue);
    });
  });

  group('ProxyAttendanceNotifier', () {
    test('초기 상태 - 지점 미선택 + load 는 조회하지 않음', () async {
      final repo = FakeProxyAttendanceRepository();
      final notifier = ProxyAttendanceNotifier(repo);

      expect(notifier.state.selectedBranch, isNull);

      await notifier.load();

      // 지점 미선택이라 datasource 미호출 → 데이터 없음
      expect(repo.lastBranchCodeForStatus, isNull);
      expect(notifier.state.data, isNull);
    });

    test('selectBranch → 선택 지점으로 일별현황 조회', () async {
      final repo = FakeProxyAttendanceRepository();
      final notifier = ProxyAttendanceNotifier(repo);

      await notifier.selectBranch(branch);

      expect(notifier.state.selectedBranch, branch);
      expect(repo.lastBranchCodeForStatus, '5832');
      expect(notifier.state.data, isNotNull);
      expect(notifier.state.hasLoaded, isTrue);
    });

    test('changeDate → 선택 날짜로 재조회', () async {
      final repo = FakeProxyAttendanceRepository();
      final notifier = ProxyAttendanceNotifier(repo);
      await notifier.selectBranch(branch);

      final target = DateTime(2026, 6, 10);
      await notifier.changeDate(target);

      expect(notifier.state.selectedDate, target);
      expect(repo.lastDate, target);
    });

    test('registerProxyAttendance - 지점 미선택 시 안내 메시지 반환', () async {
      final repo = FakeProxyAttendanceRepository();
      final notifier = ProxyAttendanceNotifier(repo);

      final err = await notifier.registerProxyAttendance(targetEmployeeId: 10);

      expect(err, contains('지점'));
      expect(repo.registerCallCount, 0);
    });

    test('registerProxyAttendance - 성공 시 null + 선택 지점 전달 + 재조회', () async {
      final repo = FakeProxyAttendanceRepository();
      final notifier = ProxyAttendanceNotifier(repo);
      await notifier.selectBranch(branch);

      final err = await notifier.registerProxyAttendance(
        targetEmployeeId: 10,
        displayWorkScheduleId: 500,
      );

      expect(err, isNull);
      expect(repo.lastRegisterBranchCode, '5832');
      expect(repo.lastRegisterTargetEmployeeId, 10);
      expect(repo.lastRegisterDisplayWorkScheduleId, 500);
      expect(repo.registerCallCount, 1);
    });

    test('registerProxyAttendance - 실패 시 에러 메시지 반환', () async {
      final repo = FakeProxyAttendanceRepository();
      final notifier = ProxyAttendanceNotifier(repo);
      await notifier.selectBranch(branch);
      repo.exceptionToThrow = Exception('등록 실패');

      final err = await notifier.registerProxyAttendance(
        targetEmployeeId: 10,
        scheduleId: 1,
      );

      expect(err, isNotNull);
    });
  });
}
