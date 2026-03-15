import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_result.dart';
import 'package:mobile/domain/entities/attendance_status.dart';
import 'package:mobile/domain/entities/store_schedule_item.dart';
import 'package:mobile/domain/repositories/attendance_repository.dart';
import 'package:mobile/domain/usecases/get_attendance_status.dart';
import 'package:mobile/domain/usecases/get_store_list.dart';
import 'package:mobile/domain/usecases/register_attendance.dart';
import 'package:mobile/presentation/providers/attendance_provider.dart';

void main() {
  group('AttendanceNotifier', () {
    late AttendanceNotifier notifier;
    late FakeAttendanceRepository repository;

    setUp(() {
      repository = FakeAttendanceRepository();
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
      expect(notifier.state.allStores, isEmpty);
      expect(notifier.state.filteredStores, isEmpty);
      expect(notifier.state.totalCount, 0);
      expect(notifier.state.registeredCount, 0);
    });

    test('loadStores 거래처 목록 로딩', () async {
      await notifier.loadStores();

      expect(notifier.state.isLoading, false);
      expect(notifier.state.allStores.length, 5);
      expect(notifier.state.filteredStores.length, 5);
      expect(notifier.state.totalCount, 5);
      expect(notifier.state.registeredCount, 0);
      expect(notifier.state.errorMessage, null);

      // 첫 번째 거래처 확인
      final firstStore = notifier.state.allStores.first;
      expect(firstStore.scheduleId, 12345);
      expect(firstStore.storeName, '이마트 해운대점');
      expect(firstStore.isRegistered, false);
    });

    test('searchStores 거래처명으로 검색', () async {
      await notifier.loadStores();

      notifier.searchStores('이마트');

      expect(notifier.state.searchKeyword, '이마트');
      expect(notifier.state.filteredStores.length, 2);
      expect(
        notifier.state.filteredStores
            .every((s) => s.storeName.contains('이마트')),
        true,
      );
    });

    test('searchStores 주소로 검색', () async {
      await notifier.loadStores();

      notifier.searchStores('해운대');

      expect(notifier.state.searchKeyword, '해운대');
      expect(notifier.state.filteredStores.length, 2);
      expect(
        notifier.state.filteredStores
            .every((s) => s.address.contains('해운대')),
        true,
      );
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

    test('selectWorkType 근무유형 선택', () {
      notifier.selectWorkType('REFRIGERATED');

      expect(notifier.state.selectedWorkType, 'REFRIGERATED');
    });

    test('selectStore 거래처 선택', () {
      notifier.selectStore(12345);

      expect(notifier.state.selectedScheduleId, 12345);
    });

    test('register 출근등록 성공', () async {
      await notifier.loadStores();
      notifier.selectStore(12345);
      notifier.selectWorkType('ROOM_TEMP');

      await notifier.register(latitude: 35.1696, longitude: 129.1318);

      expect(notifier.state.isRegistering, false);
      expect(notifier.state.registrationResult, isNotNull);
      expect(notifier.state.registrationResult?.scheduleId,
          12345);
      expect(notifier.state.registrationResult?.storeName, '이마트 해운대점');
      expect(notifier.state.registrationResult?.workType, 'ROOM_TEMP');
      expect(notifier.state.registrationResult?.distanceKm, 0.12);
      expect(notifier.state.registrationResult?.totalCount, 5);
      expect(notifier.state.registrationResult?.registeredCount, 1);
      expect(notifier.state.registeredCount, 1);
      expect(notifier.state.errorMessage, null);
    });

    test('register selectedScheduleId 없이 호출 시 아무 작업 안함', () async {
      await notifier.loadStores();

      // 거래처를 선택하지 않음
      await notifier.register(latitude: 35.0, longitude: 129.0);

      expect(notifier.state.registrationResult, null);
      expect(notifier.state.registeredCount, 0);
    });

    test('register GPS 좌표 없이 호출 시 에러', () async {
      await notifier.loadStores();
      notifier.selectStore(12345);

      await notifier.register();

      expect(notifier.state.errorMessage, 'GPS 좌표를 가져올 수 없습니다');
    });

    test('register 이미 등록된 거래처 등록 시 에러', () async {
      await notifier.loadStores();

      // 첫 번째 등록
      notifier.selectStore(12345);
      await notifier.register(latitude: 35.0, longitude: 129.0);
      expect(notifier.state.registrationResult, isNotNull);

      // 동일한 거래처 재등록 시도
      repository.exceptionToThrow = Exception('이미 출근 등록된 스케줄입니다');
      notifier.selectStore(12345);
      await notifier.register(latitude: 35.0, longitude: 129.0);

      expect(notifier.state.isRegistering, false);
      expect(notifier.state.errorMessage, '이미 출근 등록된 스케줄입니다');
    });

    test('prepareNextRegistration 선택 초기화 및 목록 새로고침', () async {
      await notifier.loadStores();
      notifier.selectStore(12345);
      notifier.searchStores('이마트');

      expect(notifier.state.selectedScheduleId, 12345);
      expect(notifier.state.searchKeyword, '이마트');

      await notifier.prepareNextRegistration();

      expect(notifier.state.searchKeyword, '');
      expect(notifier.state.filteredStores.length, 5);
    });

    test('clearRegistrationResult 등록 결과 초기화하지만 거래처 정보 유지',
        () async {
      await notifier.loadStores();
      notifier.selectStore(12345);
      await notifier.register(latitude: 35.0, longitude: 129.0);

      // 등록 완료 상태
      expect(notifier.state.registrationResult, isNotNull);
      expect(notifier.state.allStores.length, 5);

      notifier.clearRegistrationResult();

      // 등록 결과는 초기화되지만 기본 정보는 유지
      expect(notifier.state.registrationResult, null);
      expect(notifier.state.selectedScheduleId, null);
      expect(notifier.state.searchKeyword, '');
      expect(notifier.state.allStores.length, 5);
      expect(notifier.state.filteredStores.length, 5);
      expect(notifier.state.totalCount, 5);
      expect(notifier.state.registeredCount, 1);
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

    test('clearError 에러 메시지 초기화', () async {
      // 에러 발생 시키기
      repository.exceptionToThrow = Exception('테스트 에러');
      notifier.selectStore(99999);
      await notifier.register(latitude: 35.0, longitude: 129.0);

      expect(notifier.state.errorMessage, isNotNull);

      notifier.clearError();

      expect(notifier.state.errorMessage, null);
    });

    test('전체 워크플로우: 로딩 → 검색 → 선택 → 등록 → 현황 조회', () async {
      // 1. 거래처 목록 로딩
      await notifier.loadStores();
      expect(notifier.state.allStores.length, 5);

      // 2. 거래처 검색
      notifier.searchStores('이마트');
      expect(notifier.state.filteredStores.length, 2);

      // 3. 거래처 선택
      notifier.selectStore(12345);
      expect(notifier.state.selectedScheduleId, 12345);

      // 4. 근무유형 선택
      notifier.selectWorkType('REFRIGERATED');
      expect(notifier.state.selectedWorkType, 'REFRIGERATED');

      // 5. 출근등록
      await notifier.register(latitude: 35.1696, longitude: 129.1318);
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
      expect(notifier.state.searchKeyword, '');
    });
  });
}

// --- Fake Repository ---

class FakeAttendanceRepository implements AttendanceRepository {
  Exception? exceptionToThrow;
  final Set<int> _registeredIds = {};

  @override
  Future<StoreListResult> getStoreList({String? keyword}) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return StoreListResult(
      stores: _mockStores,
      totalCount: _mockStores.length,
      registeredCount:
          _mockStores.where((s) => s.isRegistered).length,
      currentDate: '2026-03-01',
    );
  }

  @override
  Future<AttendanceResult> registerAttendance({
    required int scheduleId,
    required double latitude,
    required double longitude,
    String? workType,
  }) async {
    if (exceptionToThrow != null) {
      final e = exceptionToThrow!;
      exceptionToThrow = null;
      throw e;
    }
    _registeredIds.add(scheduleId);
    final store = _mockStores.firstWhere(
      (s) => s.scheduleId == scheduleId,
    );
    return AttendanceResult(
      scheduleId: scheduleId,
      storeName: store.storeName,
      workType: workType ?? 'ROOM_TEMP',
      distanceKm: 0.12,
      totalCount: _mockStores.length,
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

final _mockStores = [
  const StoreScheduleItem(
    scheduleId: 12345,
    storeName: '이마트 해운대점',
    workCategory: '진열',
    address: '부산시 해운대구 센텀2로 25',
    isRegistered: false,
  ),
  const StoreScheduleItem(
    scheduleId: 12346,
    storeName: '홈플러스 서면점',
    workCategory: '순회',
    address: '부산시 부산진구 서면로 68번길 9',
    isRegistered: false,
  ),
  const StoreScheduleItem(
    scheduleId: 12347,
    storeName: '롯데마트 광복점',
    workCategory: '진열',
    address: '부산시 중구 중앙대로 2',
    isRegistered: false,
  ),
  const StoreScheduleItem(
    scheduleId: 12348,
    storeName: '이마트 사상점',
    workCategory: '순회',
    address: '부산시 사상구 학감대로 272',
    isRegistered: false,
  ),
  const StoreScheduleItem(
    scheduleId: 12349,
    storeName: '홈플러스 센텀시티점',
    workCategory: '진열',
    address: '부산시 해운대구 센텀남대로 59',
    isRegistered: false,
  ),
];

final _mockStatusList = [
  const AttendanceStatus(
    scheduleId: 12345,
    storeName: '이마트 해운대점',
    workCategory: '진열',
    status: 'REGISTERED',
    workType: 'ROOM_TEMP',
  ),
  const AttendanceStatus(
    scheduleId: 12346,
    storeName: '홈플러스 서면점',
    workCategory: '순회',
    status: 'PENDING',
  ),
  const AttendanceStatus(
    scheduleId: 12347,
    storeName: '롯데마트 광복점',
    workCategory: '진열',
    status: 'PENDING',
  ),
  const AttendanceStatus(
    scheduleId: 12348,
    storeName: '이마트 사상점',
    workCategory: '순회',
    status: 'PENDING',
  ),
  const AttendanceStatus(
    scheduleId: 12349,
    storeName: '홈플러스 센텀시티점',
    workCategory: '진열',
    status: 'PENDING',
  ),
];
