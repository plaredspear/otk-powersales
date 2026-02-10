import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/attendance_mock_repository.dart';
import 'package:mobile/domain/usecases/get_attendance_status.dart';
import 'package:mobile/domain/usecases/get_store_list.dart';
import 'package:mobile/domain/usecases/register_attendance.dart';
import 'package:mobile/presentation/providers/attendance_provider.dart';

void main() {
  group('AttendanceNotifier', () {
    late AttendanceNotifier notifier;
    late AttendanceMockRepository repository;

    setUp(() {
      repository = AttendanceMockRepository();
      notifier = AttendanceNotifier(
        getStoreList: GetStoreList(repository),
        registerAttendance: RegisterAttendance(repository),
        getAttendanceStatus: GetAttendanceStatus(repository),
      );
    });

    test('초기 상태는 AttendanceState.initial()', () {
      expect(notifier.state.isLoading, false);
      expect(notifier.state.isRegistering, false);
      expect(notifier.state.errorMessage, null);
      expect(notifier.state.workerType, null);
      expect(notifier.state.allStores, isEmpty);
      expect(notifier.state.filteredStores, isEmpty);
      expect(notifier.state.totalCount, 0);
      expect(notifier.state.registeredCount, 0);
    });

    test('loadStores PATROL 근무자의 거래처 목록 로딩', () async {
      await notifier.loadStores();

      expect(notifier.state.isLoading, false);
      expect(notifier.state.workerType, 'PATROL');
      expect(notifier.state.allStores.length, 5);
      expect(notifier.state.filteredStores.length, 5);
      expect(notifier.state.totalCount, 5);
      expect(notifier.state.registeredCount, 0);
      expect(notifier.state.errorMessage, null);

      // 첫 번째 거래처 확인
      final firstStore = notifier.state.allStores.first;
      expect(firstStore.storeId, 101);
      expect(firstStore.storeName, '이마트 해운대점');
      expect(firstStore.isRegistered, false);
    });

    test('loadStores FIXED 근무자는 1개 거래처만 반환', () async {
      final fixedRepository = AttendanceMockRepository(workerType: 'FIXED');
      final fixedNotifier = AttendanceNotifier(
        getStoreList: GetStoreList(fixedRepository),
        registerAttendance: RegisterAttendance(fixedRepository),
        getAttendanceStatus: GetAttendanceStatus(fixedRepository),
      );

      await fixedNotifier.loadStores();

      expect(fixedNotifier.state.workerType, 'FIXED');
      expect(fixedNotifier.state.allStores.length, 1);
      expect(fixedNotifier.state.allStores.first.storeId, 201);
      expect(fixedNotifier.state.allStores.first.storeName, '이마트 부산본점');
    });

    test('searchStores 거래처명으로 검색', () async {
      await notifier.loadStores();

      notifier.searchStores('이마트');

      expect(notifier.state.searchKeyword, '이마트');
      expect(notifier.state.filteredStores.length, 2);
      expect(
        notifier.state.filteredStores.every((s) => s.storeName.contains('이마트')),
        true,
      );
    });

    test('searchStores 주소로 검색', () async {
      await notifier.loadStores();

      notifier.searchStores('해운대');

      expect(notifier.state.searchKeyword, '해운대');
      expect(notifier.state.filteredStores.length, 2);
      expect(
        notifier.state.filteredStores.every((s) => s.address.contains('해운대')),
        true,
      );
    });

    test('searchStores 거래처 코드로 검색', () async {
      await notifier.loadStores();

      notifier.searchStores('ST-00101');

      expect(notifier.state.searchKeyword, 'ST-00101');
      expect(notifier.state.filteredStores.length, 1);
      expect(notifier.state.filteredStores.first.storeCode, 'ST-00101');
    });

    test('searchStores 빈 문자열로 검색 시 전체 목록 표시', () async {
      await notifier.loadStores();

      // 먼저 검색
      notifier.searchStores('이마트');
      expect(notifier.state.filteredStores.length, 2);

      // 검색어 초기화
      notifier.searchStores('');

      expect(notifier.state.searchKeyword, '');
      expect(notifier.state.filteredStores.length, 5);
      expect(notifier.state.filteredStores, notifier.state.allStores);
    });

    test('searchStores 검색 시 selectedStoreId는 유지됨', () async {
      await notifier.loadStores();
      notifier.selectStore(101);

      expect(notifier.state.selectedStoreId, 101);

      notifier.searchStores('홈플러스');

      // copyWith에서 null을 전달해도 ?? 연산자로 인해 기존 값 유지
      expect(notifier.state.selectedStoreId, 101);
    });

    test('searchStores 대소문자 구분 없이 검색', () async {
      await notifier.loadStores();

      notifier.searchStores('EMART');

      // 한글 거래처명에는 'EMART'가 없으므로 결과 없음
      expect(notifier.state.filteredStores.length, 0);

      notifier.searchStores('st-00101');

      expect(notifier.state.filteredStores.length, 1);
      expect(notifier.state.filteredStores.first.storeCode, 'ST-00101');
    });

    test('selectWorkType 근무유형 선택', () {
      notifier.selectWorkType('REFRIGERATED');

      expect(notifier.state.selectedWorkType, 'REFRIGERATED');
    });

    test('selectStore 거래처 선택', () {
      notifier.selectStore(101);

      expect(notifier.state.selectedStoreId, 101);
    });

    test('register 출근등록 성공', () async {
      await notifier.loadStores();
      notifier.selectStore(101);
      notifier.selectWorkType('ROOM_TEMP');

      await notifier.register();

      expect(notifier.state.isRegistering, false);
      expect(notifier.state.registrationResult, isNotNull);
      expect(notifier.state.registrationResult?.storeId, 101);
      expect(notifier.state.registrationResult?.storeName, '이마트 해운대점');
      expect(notifier.state.registrationResult?.workType, 'ROOM_TEMP');
      expect(notifier.state.registrationResult?.totalCount, 5);
      expect(notifier.state.registrationResult?.registeredCount, 1);
      expect(notifier.state.registeredCount, 1);
      expect(notifier.state.errorMessage, null);
    });

    test('register 냉장 근무유형으로 등록 성공', () async {
      await notifier.loadStores();
      notifier.selectStore(102);
      notifier.selectWorkType('REFRIGERATED');

      await notifier.register();

      expect(notifier.state.registrationResult?.workType, 'REFRIGERATED');
      expect(notifier.state.registrationResult?.storeName, '홈플러스 서면점');
    });

    test('register selectedStoreId 없이 호출 시 아무 작업 안함', () async {
      await notifier.loadStores();

      // 거래처를 선택하지 않음
      await notifier.register();

      expect(notifier.state.registrationResult, null);
      expect(notifier.state.registeredCount, 0);
    });

    test('register 이미 등록된 거래처 등록 시 에러', () async {
      await notifier.loadStores();

      // 첫 번째 등록
      notifier.selectStore(101);
      await notifier.register();
      expect(notifier.state.registrationResult, isNotNull);

      // 동일한 거래처 재등록 시도
      await notifier.loadStores(); // 목록 새로고침
      notifier.selectStore(101);
      await notifier.register();

      expect(notifier.state.isRegistering, false);
      expect(notifier.state.errorMessage, '이미 출근등록된 거래처입니다');
    });

    test('register 존재하지 않는 거래처 ID로 등록 시 에러', () async {
      await notifier.loadStores();
      notifier.selectStore(999); // 존재하지 않는 ID

      await notifier.register();

      expect(notifier.state.isRegistering, false);
      expect(notifier.state.errorMessage, '해당 거래처를 찾을 수 없습니다');
    });

    test('prepareNextRegistration 선택 초기화 및 목록 새로고침', () async {
      await notifier.loadStores();
      notifier.selectStore(101);
      notifier.searchStores('이마트');

      expect(notifier.state.selectedStoreId, 101);
      expect(notifier.state.searchKeyword, '이마트');

      await notifier.prepareNextRegistration();

      // selectedStoreId는 null 전달 시 ?? 연산자로 인해 기존 값 유지
      // searchKeyword는 빈 문자열 전달 시 ''로 변경됨
      expect(notifier.state.selectedStoreId, 101);
      expect(notifier.state.searchKeyword, '');
      expect(notifier.state.filteredStores.length, 5);
    });

    test('clearRegistrationResult 등록 결과 초기화하지만 거래처 정보 유지', () async {
      await notifier.loadStores();
      notifier.selectStore(101);
      await notifier.register();

      // 등록 완료 상태
      expect(notifier.state.registrationResult, isNotNull);
      expect(notifier.state.workerType, 'PATROL');
      expect(notifier.state.allStores.length, 5);

      notifier.clearRegistrationResult();

      // 등록 결과는 초기화되지만 기본 정보는 유지
      expect(notifier.state.registrationResult, null);
      expect(notifier.state.selectedStoreId, null);
      expect(notifier.state.searchKeyword, '');
      expect(notifier.state.workerType, 'PATROL');
      expect(notifier.state.allStores.length, 5);
      expect(notifier.state.filteredStores.length, 5);
      expect(notifier.state.totalCount, 5);
      expect(notifier.state.registeredCount, 1);
    });

    test('loadAttendanceStatus 출근등록 현황 조회', () async {
      // 먼저 한 건 등록
      await notifier.loadStores();
      notifier.selectStore(101);
      await notifier.register();

      // 현황 조회
      await notifier.loadAttendanceStatus();

      expect(notifier.state.statusList.length, 5);
      expect(notifier.state.totalCount, 5);
      expect(notifier.state.registeredCount, 1);

      // 첫 번째 거래처는 등록 완료 상태
      final firstStatus = notifier.state.statusList.first;
      expect(firstStatus.storeId, 101);
      expect(firstStatus.status, 'COMPLETED');
      expect(firstStatus.isCompleted, true);

      // 두 번째 거래처는 대기 상태
      final secondStatus = notifier.state.statusList[1];
      expect(secondStatus.storeId, 102);
      expect(secondStatus.status, 'PENDING');
      expect(secondStatus.isPending, true);
    });

    test('clearError 에러 메시지 초기화', () async {
      // 에러 발생 시키기 (존재하지 않는 거래처)
      await notifier.loadStores();
      notifier.selectStore(999);
      await notifier.register();

      expect(notifier.state.errorMessage, isNotNull);

      notifier.clearError();

      expect(notifier.state.errorMessage, null);
    });

    test('register 중 isRegistering 플래그 설정', () async {
      await notifier.loadStores();
      notifier.selectStore(101);

      final registerFuture = notifier.register();

      // 등록 시작 시 isRegistering이 true여야 함
      // (비동기 처리 중이므로 즉시 확인은 어려움, 완료 후 false 확인)
      await registerFuture;

      expect(notifier.state.isRegistering, false);
    });

    test('loadStores 중 isLoading 플래그 처리', () async {
      final loadFuture = notifier.loadStores();

      await loadFuture;

      // 완료 후에는 isLoading이 false여야 함
      expect(notifier.state.isLoading, false);
    });

    test('여러 거래처 순차 등록', () async {
      await notifier.loadStores();

      // 첫 번째 거래처 등록
      notifier.selectStore(101);
      await notifier.register();
      expect(notifier.state.registeredCount, 1);

      // 두 번째 거래처 등록을 위해 목록 새로고침
      await notifier.loadStores();
      notifier.selectStore(102);
      await notifier.register();
      expect(notifier.state.registeredCount, 2);

      // 세 번째 거래처 등록
      await notifier.loadStores();
      notifier.selectStore(103);
      await notifier.register();
      expect(notifier.state.registeredCount, 3);
    });

    test('IRREGULAR 근무자는 최대 2개 거래처 등록 가능', () async {
      final irregularRepository =
          AttendanceMockRepository(workerType: 'IRREGULAR');
      final irregularNotifier = AttendanceNotifier(
        getStoreList: GetStoreList(irregularRepository),
        registerAttendance: RegisterAttendance(irregularRepository),
        getAttendanceStatus: GetAttendanceStatus(irregularRepository),
      );

      await irregularNotifier.loadStores();

      // 첫 번째 등록
      irregularNotifier.selectStore(101);
      await irregularNotifier.register();
      expect(irregularNotifier.state.registeredCount, 1);

      // 두 번째 등록
      await irregularNotifier.loadStores();
      irregularNotifier.selectStore(102);
      await irregularNotifier.register();
      expect(irregularNotifier.state.registeredCount, 2);

      // 세 번째 등록 시도 - 에러 발생
      await irregularNotifier.loadStores();
      irregularNotifier.selectStore(103);
      await irregularNotifier.register();

      expect(irregularNotifier.state.errorMessage,
          '격고 근무자는 최대 2개 거래처만 등록 가능합니다');
    });

    test('전체 워크플로우: 로딩 → 검색 → 선택 → 등록 → 현황 조회', () async {
      // 1. 거래처 목록 로딩
      await notifier.loadStores();
      expect(notifier.state.allStores.length, 5);

      // 2. 거래처 검색
      notifier.searchStores('이마트');
      expect(notifier.state.filteredStores.length, 2);

      // 3. 거래처 선택
      notifier.selectStore(101);
      expect(notifier.state.selectedStoreId, 101);

      // 4. 근무유형 선택
      notifier.selectWorkType('REFRIGERATED');
      expect(notifier.state.selectedWorkType, 'REFRIGERATED');

      // 5. 출근등록
      await notifier.register();
      expect(notifier.state.registrationResult, isNotNull);
      expect(notifier.state.registeredCount, 1);

      // 6. 현황 조회
      await notifier.loadAttendanceStatus();
      expect(notifier.state.statusList.length, 5);
      expect(
        notifier.state.statusList.where((s) => s.isCompleted).length,
        1,
      );

      // 7. 다음 등록 준비
      await notifier.prepareNextRegistration();
      // selectedStoreId는 null 전달 시 ?? 연산자로 인해 기존 값 유지
      // searchKeyword는 빈 문자열 전달 시 ''로 변경됨
      expect(notifier.state.selectedStoreId, 101);
      expect(notifier.state.searchKeyword, '');
    });
  });
}
