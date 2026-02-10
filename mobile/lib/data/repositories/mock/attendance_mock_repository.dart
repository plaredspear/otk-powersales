import '../../../domain/entities/attendance_result.dart';
import '../../../domain/entities/attendance_status.dart';
import '../../../domain/entities/store_schedule_item.dart';
import '../../../domain/repositories/attendance_repository.dart';

/// 출근등록 Mock Repository
///
/// Backend API 개발 전 프론트엔드 개발을 위한 Mock 데이터 제공.
/// Flutter-First 전략에 따라 하드코딩된 데이터로 UI/UX 검증.
class AttendanceMockRepository implements AttendanceRepository {
  /// 근무자 유형 (PATROL: 순회, IRREGULAR: 격고, FIXED: 고정)
  final String _workerType;

  /// 등록된 출근 기록 (storeId → workType)
  final Map<int, String> _registeredAttendances = {};

  /// 자동 증가 ID
  int _nextAttendanceId = 1001;

  AttendanceMockRepository({String workerType = 'PATROL'})
      : _workerType = workerType;

  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 300));
  }

  /// Mock 거래처 데이터
  List<StoreScheduleItem> get _mockStores {
    if (_workerType == 'FIXED') {
      return [
        StoreScheduleItem(
          storeId: 201,
          storeName: '이마트 부산본점',
          storeCode: 'ST-00201',
          workCategory: '상시',
          address: '부산시 부산진구 부전동 168-6',
          isRegistered: _registeredAttendances.containsKey(201),
          registeredWorkType: _registeredAttendances[201],
        ),
      ];
    }

    return [
      StoreScheduleItem(
        storeId: 101,
        storeName: '이마트 해운대점',
        storeCode: 'ST-00101',
        workCategory: '진열',
        address: '부산시 해운대구 센텀2로 25',
        isRegistered: _registeredAttendances.containsKey(101),
        registeredWorkType: _registeredAttendances[101],
      ),
      StoreScheduleItem(
        storeId: 102,
        storeName: '홈플러스 서면점',
        storeCode: 'ST-00102',
        workCategory: '순회',
        address: '부산시 부산진구 서면로 68번길 9',
        isRegistered: _registeredAttendances.containsKey(102),
        registeredWorkType: _registeredAttendances[102],
      ),
      StoreScheduleItem(
        storeId: 103,
        storeName: '롯데마트 광복점',
        storeCode: 'ST-00103',
        workCategory: '진열',
        address: '부산시 중구 중앙대로 2',
        isRegistered: _registeredAttendances.containsKey(103),
        registeredWorkType: _registeredAttendances[103],
      ),
      StoreScheduleItem(
        storeId: 104,
        storeName: '이마트 사상점',
        storeCode: 'ST-00104',
        workCategory: '순회',
        address: '부산시 사상구 학감대로 272',
        isRegistered: _registeredAttendances.containsKey(104),
        registeredWorkType: _registeredAttendances[104],
      ),
      StoreScheduleItem(
        storeId: 105,
        storeName: '홈플러스 센텀시티점',
        storeCode: 'ST-00105',
        workCategory: '진열',
        address: '부산시 해운대구 센텀남대로 59',
        isRegistered: _registeredAttendances.containsKey(105),
        registeredWorkType: _registeredAttendances[105],
      ),
    ];
  }

  @override
  Future<StoreListResult> getStoreList() async {
    await _simulateDelay();

    final stores = _mockStores;
    final registeredCount =
        stores.where((store) => store.isRegistered).length;

    return StoreListResult(
      workerType: _workerType,
      stores: stores,
      totalCount: stores.length,
      registeredCount: registeredCount,
      currentDate: DateTime.now().toIso8601String().substring(0, 10),
    );
  }

  @override
  Future<AttendanceResult> registerAttendance({
    required int storeId,
    required String workType,
  }) async {
    await _simulateDelay();

    // 이미 등록된 거래처 확인
    if (_registeredAttendances.containsKey(storeId)) {
      throw Exception('이미 출근등록된 거래처입니다');
    }

    // 격고 근무자 등록 한도 확인 (최대 2개)
    if (_workerType == 'IRREGULAR' && _registeredAttendances.length >= 2) {
      throw Exception('격고 근무자는 최대 2개 거래처만 등록 가능합니다');
    }

    // 등록 처리
    _registeredAttendances[storeId] = workType;

    // 거래처명 조회
    final store = _mockStores.firstWhere(
      (s) => s.storeId == storeId,
      orElse: () => throw Exception('해당 거래처를 찾을 수 없습니다'),
    );

    final stores = _mockStores;
    final registeredCount =
        stores.where((s) => s.isRegistered).length;

    return AttendanceResult(
      attendanceId: _nextAttendanceId++,
      storeId: storeId,
      storeName: store.storeName,
      workType: workType,
      registeredAt: DateTime.now(),
      totalCount: stores.length,
      registeredCount: registeredCount,
    );
  }

  @override
  Future<AttendanceStatusResult> getAttendanceStatus() async {
    await _simulateDelay();

    final stores = _mockStores;
    final statusList = stores.map((store) {
      return AttendanceStatus(
        storeId: store.storeId,
        storeName: store.storeName,
        status: store.isRegistered ? 'COMPLETED' : 'PENDING',
        workType: store.registeredWorkType,
        registeredAt: store.isRegistered ? DateTime.now() : null,
      );
    }).toList();

    final registeredCount =
        statusList.where((s) => s.isCompleted).length;

    return AttendanceStatusResult(
      totalCount: stores.length,
      registeredCount: registeredCount,
      statusList: statusList,
      currentDate: DateTime.now().toIso8601String().substring(0, 10),
    );
  }
}
