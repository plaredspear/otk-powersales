import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/attendance_mock_repository.dart';

void main() {
  group('AttendanceMockRepository', () {
    late AttendanceMockRepository repository;

    setUp(() {
      repository = AttendanceMockRepository(workerType: 'PATROL');
    });

    group('생성자 및 근무자 유형', () {
      test('PATROL 근무자는 5개 거래처 보유', () async {
        // when
        final result = await repository.getStoreList();

        // then
        expect(result.workerType, 'PATROL');
        expect(result.stores.length, 5);
        expect(result.totalCount, 5);
      });

      test('FIXED 근무자는 1개 거래처 보유', () async {
        // given
        repository = AttendanceMockRepository(workerType: 'FIXED');

        // when
        final result = await repository.getStoreList();

        // then
        expect(result.workerType, 'FIXED');
        expect(result.stores.length, 1);
        expect(result.totalCount, 1);
        expect(result.stores.first.storeName, '이마트 부산본점');
        expect(result.stores.first.storeCode, 'ST-00201');
        expect(result.stores.first.storeId, 201);
      });

      test('기본값은 PATROL 근무자', () async {
        // given
        repository = AttendanceMockRepository();

        // when
        final result = await repository.getStoreList();

        // then
        expect(result.workerType, 'PATROL');
        expect(result.stores.length, 5);
      });
    });

    group('getStoreList() 메서드', () {
      test('초기 상태에서는 모든 거래처가 미등록', () async {
        // when
        final result = await repository.getStoreList();

        // then
        expect(result.registeredCount, 0);
        for (final store in result.stores) {
          expect(store.isRegistered, false);
          expect(store.registeredWorkType, null);
        }
      });

      test('StoreListResult 모든 필드 존재', () async {
        // when
        final result = await repository.getStoreList();

        // then
        expect(result.workerType, isNotEmpty);
        expect(result.stores, isNotEmpty);
        expect(result.totalCount, isPositive);
        expect(result.registeredCount, isA<int>());
        expect(result.currentDate, matches(RegExp(r'^\d{4}-\d{2}-\d{2}$')));
      });

      test('각 거래처는 모든 필수 정보를 포함', () async {
        // when
        final result = await repository.getStoreList();

        // then
        for (final store in result.stores) {
          expect(store.storeId, isPositive);
          expect(store.storeName, isNotEmpty);
          expect(store.storeCode, isNotEmpty);
          expect(store.workCategory, isNotEmpty);
          expect(store.address, isNotEmpty);
          expect(store.isRegistered, isA<bool>());
        }
      });
    });

    group('registerAttendance() 메서드', () {
      test('ROOM_TEMP 유형으로 출근 등록 성공', () async {
        // when
        final result = await repository.registerAttendance(
          storeId: 101,
          workType: 'ROOM_TEMP',
        );

        // then
        expect(result.attendanceId, 1001);
        expect(result.storeId, 101);
        expect(result.storeName, '이마트 해운대점');
        expect(result.workType, 'ROOM_TEMP');
        expect(result.registeredAt, isNotNull);
        expect(result.totalCount, 5);
        expect(result.registeredCount, 1);
      });

      test('등록 후 getStoreList에서 isRegistered가 true로 변경', () async {
        // given
        await repository.registerAttendance(
          storeId: 101,
          workType: 'ROOM_TEMP',
        );

        // when
        final result = await repository.getStoreList();

        // then
        final registeredStore =
            result.stores.firstWhere((s) => s.storeId == 101);
        expect(registeredStore.isRegistered, true);
        expect(registeredStore.registeredWorkType, 'ROOM_TEMP');
        expect(result.registeredCount, 1);
      });

      test('등록 후 registeredCount가 증가', () async {
        // given
        final before = await repository.getStoreList();
        expect(before.registeredCount, 0);

        // when
        await repository.registerAttendance(
          storeId: 101,
          workType: 'ROOM_TEMP',
        );
        await repository.registerAttendance(
          storeId: 102,
          workType: 'REFRIGERATED',
        );

        // then
        final after = await repository.getStoreList();
        expect(after.registeredCount, 2);
      });

      test('중복 등록 시 Exception 발생', () async {
        // given
        await repository.registerAttendance(
          storeId: 101,
          workType: 'ROOM_TEMP',
        );

        // when & then
        expect(
          () => repository.registerAttendance(
            storeId: 101,
            workType: 'REFRIGERATED',
          ),
          throwsA(isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('이미 출근등록된 거래처입니다'),
          )),
        );
      });

      test('존재하지 않는 storeId 등록 시 Exception 발생', () async {
        // when & then
        expect(
          () => repository.registerAttendance(
            storeId: 999,
            workType: 'ROOM_TEMP',
          ),
          throwsA(isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('해당 거래처를 찾을 수 없습니다'),
          )),
        );
      });
    });

    group('IRREGULAR 근무자 제한', () {
      test('IRREGULAR 근무자는 3번째 등록 시 Exception 발생', () async {
        // given
        repository = AttendanceMockRepository(workerType: 'IRREGULAR');
        await repository.registerAttendance(
          storeId: 101,
          workType: 'ROOM_TEMP',
        );
        await repository.registerAttendance(
          storeId: 102,
          workType: 'REFRIGERATED',
        );

        // when & then
        expect(
          () => repository.registerAttendance(
            storeId: 103,
            workType: 'ROOM_TEMP',
          ),
          throwsA(isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('최대 2개'),
          )),
        );
      });

      test('IRREGULAR 근무자 2개 등록까지는 정상 동작', () async {
        // given
        repository = AttendanceMockRepository(workerType: 'IRREGULAR');

        // when
        final result1 = await repository.registerAttendance(
          storeId: 101,
          workType: 'ROOM_TEMP',
        );
        final result2 = await repository.registerAttendance(
          storeId: 102,
          workType: 'REFRIGERATED',
        );

        // then
        expect(result1.registeredCount, 1);
        expect(result2.registeredCount, 2);
      });
    });

    group('getAttendanceStatus() 메서드', () {
      test('초기 상태 조회', () async {
        // when
        final result = await repository.getAttendanceStatus();

        // then
        expect(result.totalCount, 5);
        expect(result.registeredCount, 0);
        expect(result.statusList.length, 5);
        expect(result.currentDate, matches(RegExp(r'^\d{4}-\d{2}-\d{2}$')));

        for (final status in result.statusList) {
          expect(status.status, 'PENDING');
          expect(status.workType, null);
          expect(status.registeredAt, null);
        }
      });

      test('등록 후 상태 반영', () async {
        // given
        await repository.registerAttendance(
          storeId: 101,
          workType: 'ROOM_TEMP',
        );
        await repository.registerAttendance(
          storeId: 102,
          workType: 'REFRIGERATED',
        );

        // when
        final result = await repository.getAttendanceStatus();

        // then
        expect(result.totalCount, 5);
        expect(result.registeredCount, 2);

        final store101 =
            result.statusList.firstWhere((s) => s.storeId == 101);
        expect(store101.status, 'COMPLETED');
        expect(store101.workType, 'ROOM_TEMP');
        expect(store101.registeredAt, isNotNull);

        final store102 =
            result.statusList.firstWhere((s) => s.storeId == 102);
        expect(store102.status, 'COMPLETED');
        expect(store102.workType, 'REFRIGERATED');
        expect(store102.registeredAt, isNotNull);

        final store103 =
            result.statusList.firstWhere((s) => s.storeId == 103);
        expect(store103.status, 'PENDING');
        expect(store103.workType, null);
      });

      test('AttendanceStatus getter 메서드 동작 확인', () async {
        // given
        await repository.registerAttendance(
          storeId: 101,
          workType: 'ROOM_TEMP',
        );

        // when
        final result = await repository.getAttendanceStatus();

        // then
        final completedStatus =
            result.statusList.firstWhere((s) => s.storeId == 101);
        final pendingStatus =
            result.statusList.firstWhere((s) => s.storeId == 102);

        expect(completedStatus.isCompleted, true);
        expect(completedStatus.isPending, false);
        expect(pendingStatus.isCompleted, false);
        expect(pendingStatus.isPending, true);
      });
    });

    group('attendanceId 자동 증가', () {
      test('등록마다 attendanceId가 자동 증가', () async {
        // when
        final result1 = await repository.registerAttendance(
          storeId: 101,
          workType: 'ROOM_TEMP',
        );
        final result2 = await repository.registerAttendance(
          storeId: 102,
          workType: 'REFRIGERATED',
        );
        final result3 = await repository.registerAttendance(
          storeId: 103,
          workType: 'ROOM_TEMP',
        );

        // then
        expect(result1.attendanceId, 1001);
        expect(result2.attendanceId, 1002);
        expect(result3.attendanceId, 1003);
      });
    });

    group('비동기 지연 시뮬레이션', () {
      test('모든 메서드는 비동기로 동작', () async {
        // when
        final stopwatch = Stopwatch()..start();
        await repository.getStoreList();
        stopwatch.stop();

        // then - 최소 300ms 지연
        expect(stopwatch.elapsedMilliseconds, greaterThanOrEqualTo(200));
      });
    });

    group('Mock 데이터 무결성', () {
      test('PATROL 거래처는 5개의 고유 ID를 가짐', () async {
        // when
        final result = await repository.getStoreList();

        // then
        final storeIds = result.stores.map((s) => s.storeId).toSet();
        expect(storeIds.length, 5);
        expect(storeIds, {101, 102, 103, 104, 105});
      });

      test('모든 거래처는 고유한 storeCode를 가짐', () async {
        // when
        final result = await repository.getStoreList();

        // then
        final storeCodes = result.stores.map((s) => s.storeCode).toSet();
        expect(storeCodes.length, result.stores.length);
      });
    });
  });
}
