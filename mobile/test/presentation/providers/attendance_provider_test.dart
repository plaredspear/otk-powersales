import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_result.dart';
import 'package:mobile/domain/entities/attendance_status.dart';
import 'package:mobile/domain/entities/account_schedule_item.dart';
import 'package:mobile/domain/repositories/attendance_repository.dart';
import 'package:mobile/domain/usecases/get_attendance_status.dart';
import 'package:mobile/domain/usecases/get_account_list.dart';
import 'package:mobile/domain/usecases/register_attendance.dart';
import 'package:mobile/presentation/providers/attendance_provider.dart';

void main() {
  group('AttendanceNotifier', () {
    late AttendanceNotifier notifier;
    late FakeAttendanceRepository repository;

    setUp(() {
      repository = FakeAttendanceRepository();
      notifier = AttendanceNotifier(
        getAccountList: GetAccountList(repository),
        registerAttendance: RegisterAttendance(repository),
        getAttendanceStatus: GetAttendanceStatus(repository),
      );
    });

    test('초기 상태는 AttendanceState.initial()', () {
      expect(notifier.state.isLoading, false);
      expect(notifier.state.isRegistering, false);
      expect(notifier.state.errorMessage, null);
      expect(notifier.state.allAccounts, isEmpty);
      expect(notifier.state.filteredAccounts, isEmpty);
      expect(notifier.state.totalCount, 0);
      expect(notifier.state.registeredCount, 0);
    });

    test('loadAccounts 거래처 목록 로딩', () async {
      await notifier.loadAccounts();

      expect(notifier.state.isLoading, false);
      expect(notifier.state.allAccounts.length, 5);
      expect(notifier.state.filteredAccounts.length, 5);
      expect(notifier.state.totalCount, 5);
      expect(notifier.state.registeredCount, 0);
      expect(notifier.state.errorMessage, null);

      // 첫 번째 거래처 확인
      final firstAccount = notifier.state.allAccounts.first;
      expect(firstAccount.scheduleId, 12345);
      expect(firstAccount.accountName, '이마트 해운대점');
      expect(firstAccount.isRegistered, false);
    });

    test('searchAccounts 거래처명으로 검색', () async {
      await notifier.loadAccounts();

      notifier.searchAccounts('이마트');

      expect(notifier.state.searchKeyword, '이마트');
      expect(notifier.state.filteredAccounts.length, 2);
      expect(
        notifier.state.filteredAccounts
            .every((s) => s.accountName.contains('이마트')),
        true,
      );
    });

    test('searchAccounts 주소로 검색', () async {
      await notifier.loadAccounts();

      notifier.searchAccounts('해운대');

      expect(notifier.state.searchKeyword, '해운대');
      expect(notifier.state.filteredAccounts.length, 2);
      expect(
        notifier.state.filteredAccounts
            .every((s) => s.address.contains('해운대')),
        true,
      );
    });

    test('searchAccounts 거래처코드로 검색', () async {
      await notifier.loadAccounts();

      notifier.searchAccounts('2001');

      expect(notifier.state.searchKeyword, '2001');
      expect(notifier.state.filteredAccounts.length, 2);
      expect(
        notifier.state.filteredAccounts
            .every((s) => s.accountTypeCode == '2001'),
        true,
      );
    });

    test('searchAccounts accountTypeCode가 null인 거래처는 코드 검색에서 제외', () async {
      await notifier.loadAccounts();

      // 12349는 accountTypeCode가 null
      notifier.searchAccounts('4001');

      expect(notifier.state.filteredAccounts.length, 1);
      expect(notifier.state.filteredAccounts.first.scheduleId, 12348);
    });

    test('searchAccounts 빈 문자열로 검색 시 전체 목록 표시', () async {
      await notifier.loadAccounts();

      // 먼저 검색
      notifier.searchAccounts('이마트');
      expect(notifier.state.filteredAccounts.length, 2);

      // 검색어 초기화
      notifier.searchAccounts('');

      expect(notifier.state.searchKeyword, '');
      expect(notifier.state.filteredAccounts.length, 5);
      expect(notifier.state.filteredAccounts, notifier.state.allAccounts);
    });

    test('selectAccount schedule 소스 거래처 선택', () {
      notifier.selectAccount(_mockAccounts[0]);

      expect(notifier.state.selectedScheduleId, 12345);
      expect(notifier.state.selectedSource, 'schedule');
    });

    test('selectAccount master 소스 거래처 선택', () {
      const masterAccount = AccountScheduleItem(
        scheduleId: 0,
        displayWorkScheduleId: 999,
        accountName: '진열마스터 거래처',
        workCategory: '진열',
        address: '서울시 강남구',
        isRegistered: false,
        source: 'master',
      );
      notifier.selectAccount(masterAccount);

      expect(notifier.state.selectedScheduleId, 999);
      expect(notifier.state.selectedSource, 'master');
    });

    test('register 출근등록 성공', () async {
      await notifier.loadAccounts();
      notifier.selectAccount(_mockAccounts[0]);

      final result =
          await notifier.register(latitude: 35.1696, longitude: 129.1318);

      expect(notifier.state.isRegistering, false);
      expect(result, isNotNull);
      expect(result?.scheduleId, 12345);
      expect(result?.accountName, '이마트 해운대점');
      expect(result?.distanceKm, 0.12);
      expect(result?.totalCount, 5);
      expect(result?.registeredCount, 1);
      expect(notifier.state.registeredCount, 1);
      expect(notifier.state.errorMessage, null);
    });

    test('register selectedScheduleId 없이 호출 시 아무 작업 안함', () async {
      await notifier.loadAccounts();

      // 거래처를 선택하지 않음
      final result = await notifier.register(latitude: 35.0, longitude: 129.0);

      expect(result, null);
      expect(notifier.state.registeredCount, 0);
    });

    test('register GPS 좌표 없이 호출 시 에러', () async {
      await notifier.loadAccounts();
      notifier.selectAccount(_mockAccounts[0]);

      await notifier.register();

      expect(notifier.state.errorMessage, 'GPS 좌표를 가져올 수 없습니다');
    });

    test('register 이미 등록된 거래처 등록 시 에러', () async {
      await notifier.loadAccounts();

      // 첫 번째 등록
      notifier.selectAccount(_mockAccounts[0]);
      final firstResult =
          await notifier.register(latitude: 35.0, longitude: 129.0);
      expect(firstResult, isNotNull);

      // 동일한 거래처 재등록 시도
      repository.exceptionToThrow = Exception('이미 출근 등록된 스케줄입니다');
      notifier.selectAccount(_mockAccounts[0]);
      await notifier.register(latitude: 35.0, longitude: 129.0);

      expect(notifier.state.isRegistering, false);
      expect(notifier.state.errorMessage, '이미 출근 등록된 스케줄입니다');
    });

    test('prepareNextRegistration 선택 초기화 및 목록 새로고침', () async {
      await notifier.loadAccounts();
      notifier.selectAccount(_mockAccounts[0]);

      expect(notifier.state.selectedScheduleId, 12345);

      await notifier.prepareNextRegistration();

      expect(notifier.state.selectedScheduleId, null);
      expect(notifier.state.searchKeyword, '');
      expect(notifier.state.filteredAccounts.length, 5);
    });

    test('loadAttendanceStatus 출근등록 현황 조회', () async {
      await notifier.loadAttendanceStatus();

      expect(notifier.state.statusList.length, 5);
      expect(notifier.state.totalCount, 5);
      expect(notifier.state.registeredCount, 1);

      // 첫 번째 거래처는 등록 완료 상태
      final firstStatus = notifier.state.statusList.first;
      expect(firstStatus.scheduleId, 12345);
      expect(firstStatus.status, 'REGISTERED');
      expect(firstStatus.isCompleted, true);

      // 두 번째 거래처는 대기 상태
      final secondStatus = notifier.state.statusList[1];
      expect(secondStatus.scheduleId, 12346);
      expect(secondStatus.status, 'PENDING');
      expect(secondStatus.isPending, true);
    });

    test('register master 소스 출근등록 성공', () async {
      const masterAccount = AccountScheduleItem(
        scheduleId: 0,
        displayWorkScheduleId: 999,
        accountName: '진열마스터 거래처',
        workCategory: '진열',
        address: '서울시 강남구',
        isRegistered: false,
        source: 'master',
      );
      repository.accountsOverride = [masterAccount];
      await notifier.loadAccounts();
      notifier.selectAccount(masterAccount);

      final result = await notifier.register(latitude: 35.0, longitude: 129.0);

      expect(notifier.state.isRegistering, false);
      expect(result, isNotNull);
      expect(result?.accountName, '진열마스터 거래처');
      expect(notifier.state.errorMessage, null);
    });

    test('clearError 에러 메시지 초기화', () async {
      // 에러 발생 시키기
      repository.exceptionToThrow = Exception('테스트 에러');
      notifier.selectAccount(_mockAccounts[0]);
      await notifier.register(latitude: 35.0, longitude: 129.0);

      expect(notifier.state.errorMessage, isNotNull);

      notifier.clearError();

      expect(notifier.state.errorMessage, null);
    });

    group('isFixedWorker', () {
      test('고정 근무자 - 전부 workCategory3=="고정" -> isFixedWorker true', () async {
        repository.accountsOverride = _fixedWorkerAccounts;
        await notifier.loadAccounts();

        expect(notifier.state.isFixedWorker, true);
      });

      test('고정 근무자 자동 선택 - 미등록 거래처 1개 -> 자동 선택', () async {
        repository.accountsOverride = _fixedWorkerAccounts;
        await notifier.loadAccounts();

        expect(notifier.state.selectedScheduleId, 100);
      });

      test('순회 근무자 - workCategory3가 혼합 -> isFixedWorker false', () async {
        repository.accountsOverride = _mixedWorkerAccounts;
        await notifier.loadAccounts();

        expect(notifier.state.isFixedWorker, false);
        expect(notifier.state.selectedScheduleId, null);
      });

      test('workCategory3가 null 혼재 -> isFixedWorker false', () async {
        repository.accountsOverride = _nullMixedAccounts;
        await notifier.loadAccounts();

        expect(notifier.state.isFixedWorker, false);
      });

      test('빈 목록 -> isFixedWorker false', () async {
        repository.accountsOverride = [];
        await notifier.loadAccounts();

        expect(notifier.state.isFixedWorker, false);
      });
    });

    group('safetyCheckCompleted', () {
      test('loadAccounts 안전점검 완료 시 safetyCheckCompleted true', () async {
        repository.safetyCheckCompleted = true;
        await notifier.loadAccounts();

        expect(notifier.state.safetyCheckCompleted, true);
      });

      test('loadAccounts 안전점검 미완료 시 safetyCheckCompleted false', () async {
        repository.safetyCheckCompleted = false;
        await notifier.loadAccounts();

        expect(notifier.state.safetyCheckCompleted, false);
      });

      test('초기 상태 safetyCheckCompleted는 false', () {
        expect(notifier.state.safetyCheckCompleted, false);
      });
    });

    test('전체 워크플로우: 로딩 → 검색 → 선택 → 등록 → 현황 조회', () async {
      // 1. 거래처 목록 로딩
      await notifier.loadAccounts();
      expect(notifier.state.allAccounts.length, 5);

      // 2. 거래처 검색
      notifier.searchAccounts('이마트');
      expect(notifier.state.filteredAccounts.length, 2);

      // 3. 거래처 선택
      notifier.selectAccount(_mockAccounts[0]);
      expect(notifier.state.selectedScheduleId, 12345);

      // 4. 출근등록
      final result =
          await notifier.register(latitude: 35.1696, longitude: 129.1318);
      expect(result, isNotNull);
      expect(notifier.state.registeredCount, 1);

      // 5. 현황 조회
      await notifier.loadAttendanceStatus();
      expect(notifier.state.statusList.length, 5);
      expect(
        notifier.state.statusList.where((s) => s.isCompleted).length,
        1,
      );

      // 6. 다음 등록 준비
      await notifier.prepareNextRegistration();
      expect(notifier.state.searchKeyword, '');
    });
  });
}

// --- Fake Repository ---

class FakeAttendanceRepository implements AttendanceRepository {
  Exception? exceptionToThrow;
  final Set<int> _registeredIds = {};
  List<AccountScheduleItem>? accountsOverride;
  bool safetyCheckCompleted = true;

  @override
  Future<AccountListResult> getAccountList({String? keyword}) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    final accounts = accountsOverride ?? _mockAccounts;
    return AccountListResult(
      accounts: accounts,
      totalCount: accounts.length,
      registeredCount:
          accounts.where((s) => s.isRegistered).length,
      currentDate: '2026-03-01',
      safetyCheckCompleted: safetyCheckCompleted,
    );
  }

  @override
  Future<AttendanceResult> registerAttendance({
    required int scheduleId,
    int? displayWorkScheduleId,
    required double latitude,
    required double longitude,
  }) async {
    if (exceptionToThrow != null) {
      final e = exceptionToThrow!;
      exceptionToThrow = null;
      throw e;
    }
    final accounts = accountsOverride ?? _mockAccounts;
    final AccountScheduleItem account;
    if (displayWorkScheduleId != null && displayWorkScheduleId > 0) {
      account = accounts.firstWhere(
        (s) => s.displayWorkScheduleId == displayWorkScheduleId,
      );
    } else {
      account = accounts.firstWhere(
        (s) => s.scheduleId == scheduleId,
      );
    }
    _registeredIds.add(scheduleId);
    return AttendanceResult(
      scheduleId: scheduleId,
      accountName: account.accountName,
      workType: 'ROOM_TEMP',
      distanceKm: 0.12,
      totalCount: accounts.length,
      registeredCount: _registeredIds.length,
    );
  }

  @override
  Future<AttendanceStatusResult> getAttendanceStatus() async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return AttendanceStatusResult(
      totalCount: 5,
      registeredCount: 1,
      statusList: _mockStatusList,
      currentDate: '2026-03-01',
    );
  }
}

final _mockAccounts = [
  const AccountScheduleItem(
    scheduleId: 12345,
    accountName: '이마트 해운대점',
    accountTypeCode: '2001',
    workCategory: '진열',
    address: '부산시 해운대구 센텀2로 25',
    isRegistered: false,
  ),
  const AccountScheduleItem(
    scheduleId: 12346,
    accountName: '홈플러스 서면점',
    accountTypeCode: '3001',
    workCategory: '순회',
    address: '부산시 부산진구 서면로 68번길 9',
    isRegistered: false,
  ),
  const AccountScheduleItem(
    scheduleId: 12347,
    accountName: '롯데마트 광복점',
    accountTypeCode: '2001',
    workCategory: '진열',
    address: '부산시 중구 중앙대로 2',
    isRegistered: false,
  ),
  const AccountScheduleItem(
    scheduleId: 12348,
    accountName: '이마트 사상점',
    accountTypeCode: '4001',
    workCategory: '순회',
    address: '부산시 사상구 학감대로 272',
    isRegistered: false,
  ),
  const AccountScheduleItem(
    scheduleId: 12349,
    accountName: '홈플러스 센텀시티점',
    workCategory: '진열',
    address: '부산시 해운대구 센텀남대로 59',
    isRegistered: false,
  ),
];

final _fixedWorkerAccounts = [
  const AccountScheduleItem(
    scheduleId: 100,
    accountName: '고정 거래처',
    accountTypeCode: '2001',
    workCategory: '행사',
    workCategory3: '고정',
    address: '서울 강남구',
    isRegistered: false,
  ),
];

final _mixedWorkerAccounts = [
  const AccountScheduleItem(
    scheduleId: 200,
    accountName: '거래처 A',
    workCategory: '행사',
    workCategory3: '고정',
    address: '서울 강남구',
    isRegistered: false,
  ),
  const AccountScheduleItem(
    scheduleId: 201,
    accountName: '거래처 B',
    workCategory: '순회',
    workCategory3: '순회',
    address: '서울 서초구',
    isRegistered: false,
  ),
];

final _nullMixedAccounts = [
  const AccountScheduleItem(
    scheduleId: 300,
    accountName: '거래처 C',
    workCategory: '행사',
    workCategory3: '고정',
    address: '서울 강남구',
    isRegistered: false,
  ),
  const AccountScheduleItem(
    scheduleId: 301,
    accountName: '거래처 D',
    workCategory: '순회',
    address: '서울 서초구',
    isRegistered: false,
  ),
];

final _mockStatusList = [
  const AttendanceStatus(
    scheduleId: 12345,
    accountName: '이마트 해운대점',
    workCategory: '진열',
    status: 'REGISTERED',
    workType: 'ROOM_TEMP',
  ),
  const AttendanceStatus(
    scheduleId: 12346,
    accountName: '홈플러스 서면점',
    workCategory: '순회',
    status: 'PENDING',
  ),
  const AttendanceStatus(
    scheduleId: 12347,
    accountName: '롯데마트 광복점',
    workCategory: '진열',
    status: 'PENDING',
  ),
  const AttendanceStatus(
    scheduleId: 12348,
    accountName: '이마트 사상점',
    workCategory: '순회',
    status: 'PENDING',
  ),
  const AttendanceStatus(
    scheduleId: 12349,
    accountName: '홈플러스 센텀시티점',
    workCategory: '진열',
    status: 'PENDING',
  ),
];
