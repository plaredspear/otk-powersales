import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/usecases/get_attendance_status.dart';
import 'package:mobile/domain/usecases/register_attendance.dart';
import 'package:mobile/data/repositories/mock/attendance_mock_repository.dart';

void main() {
  group('GetAttendanceStatus UseCase', () {
    late GetAttendanceStatus useCase;
    late RegisterAttendance registerUseCase;
    late AttendanceMockRepository repository;

    setUp(() {
      repository = AttendanceMockRepository(workerType: 'PATROL');
      useCase = GetAttendanceStatus(repository);
      registerUseCase = RegisterAttendance(repository);
    });

    group('초기 상태', () {
      test('초기 상태에서는 모든 거래처가 PENDING', () async {
        // when
        final result = await useCase.call();

        // then
        expect(result.totalCount, 5);
        expect(result.registeredCount, 0);
        expect(result.statusList.length, 5);
        expect(result.currentDate, isNotEmpty);

        for (final status in result.statusList) {
          expect(status.status, 'PENDING');
          expect(status.isPending, true);
          expect(status.isCompleted, false);
          expect(status.workType, null);
          expect(status.registeredAt, null);
        }
      });

      test('AttendanceStatus는 모든 필수 필드를 포함', () async {
        // when
        final result = await useCase.call();

        // then
        final firstStatus = result.statusList.first;
        expect(firstStatus.storeId, isA<int>());
        expect(firstStatus.storeName, isA<String>());
        expect(firstStatus.status, isA<String>());
        expect(firstStatus.storeId, isPositive);
        expect(firstStatus.storeName, isNotEmpty);
      });
    });

    group('등록 후 상태', () {
      test('1개 등록 후 해당 거래처만 COMPLETED 상태', () async {
        // given
        await registerUseCase.call(storeId: 101, workType: 'ROOM_TEMP');

        // when
        final result = await useCase.call();

        // then
        expect(result.totalCount, 5);
        expect(result.registeredCount, 1);

        final completedStatus =
            result.statusList.firstWhere((s) => s.storeId == 101);
        expect(completedStatus.status, 'COMPLETED');
        expect(completedStatus.isCompleted, true);
        expect(completedStatus.isPending, false);
        expect(completedStatus.workType, 'ROOM_TEMP');
        expect(completedStatus.registeredAt, isNotNull);

        final pendingCount =
            result.statusList.where((s) => s.isPending).length;
        expect(pendingCount, 4);
      });

      test('3개 등록 후 정확한 상태 반영', () async {
        // given
        await registerUseCase.call(storeId: 101, workType: 'ROOM_TEMP');
        await registerUseCase.call(storeId: 102, workType: 'REFRIGERATED');
        await registerUseCase.call(storeId: 103, workType: 'ROOM_TEMP');

        // when
        final result = await useCase.call();

        // then
        expect(result.totalCount, 5);
        expect(result.registeredCount, 3);

        final completedCount =
            result.statusList.where((s) => s.isCompleted).length;
        expect(completedCount, 3);

        final pendingCount =
            result.statusList.where((s) => s.isPending).length;
        expect(pendingCount, 2);

        // 각 등록 거래처의 workType 확인
        final store101 =
            result.statusList.firstWhere((s) => s.storeId == 101);
        expect(store101.workType, 'ROOM_TEMP');

        final store102 =
            result.statusList.firstWhere((s) => s.storeId == 102);
        expect(store102.workType, 'REFRIGERATED');
      });

      test('모든 거래처 등록 완료 시 registeredCount와 totalCount가 동일', () async {
        // given - 5개 모두 등록
        await registerUseCase.call(storeId: 101, workType: 'ROOM_TEMP');
        await registerUseCase.call(storeId: 102, workType: 'REFRIGERATED');
        await registerUseCase.call(storeId: 103, workType: 'ROOM_TEMP');
        await registerUseCase.call(storeId: 104, workType: 'REFRIGERATED');
        await registerUseCase.call(storeId: 105, workType: 'ROOM_TEMP');

        // when
        final result = await useCase.call();

        // then
        expect(result.registeredCount, result.totalCount);
        expect(result.registeredCount, 5);

        for (final status in result.statusList) {
          expect(status.isCompleted, true);
          expect(status.workType, isNotNull);
          expect(status.registeredAt, isNotNull);
        }
      });
    });

    group('totalCount와 registeredCount', () {
      test('totalCount는 거래처 수와 일치', () async {
        // when
        final result = await useCase.call();

        // then
        expect(result.totalCount, result.statusList.length);
      });

      test('registeredCount는 등록된 거래처 수와 일치', () async {
        // given
        await registerUseCase.call(storeId: 101, workType: 'ROOM_TEMP');
        await registerUseCase.call(storeId: 102, workType: 'REFRIGERATED');

        // when
        final result = await useCase.call();

        // then
        final completedCount =
            result.statusList.where((s) => s.isCompleted).length;
        expect(result.registeredCount, completedCount);
        expect(result.registeredCount, 2);
      });
    });

    group('FIXED 근무자', () {
      test('FIXED 근무자는 1개 거래처만 조회', () async {
        // given
        repository = AttendanceMockRepository(workerType: 'FIXED');
        useCase = GetAttendanceStatus(repository);
        registerUseCase = RegisterAttendance(repository);

        // when
        final result = await useCase.call();

        // then
        expect(result.totalCount, 1);
        expect(result.statusList.length, 1);
        expect(result.statusList.first.storeName, '이마트 부산본점');
      });

      test('FIXED 근무자 등록 후 상태 확인', () async {
        // given
        repository = AttendanceMockRepository(workerType: 'FIXED');
        useCase = GetAttendanceStatus(repository);
        registerUseCase = RegisterAttendance(repository);

        await registerUseCase.call(storeId: 201, workType: 'ROOM_TEMP');

        // when
        final result = await useCase.call();

        // then
        expect(result.totalCount, 1);
        expect(result.registeredCount, 1);
        expect(result.statusList.first.isCompleted, true);
      });
    });

    group('AttendanceStatus getter 메서드', () {
      test('isCompleted getter가 정상 동작', () async {
        // given
        await registerUseCase.call(storeId: 101, workType: 'ROOM_TEMP');

        // when
        final result = await useCase.call();

        // then
        final completedStatus =
            result.statusList.firstWhere((s) => s.storeId == 101);
        final pendingStatus =
            result.statusList.firstWhere((s) => s.storeId == 102);

        expect(completedStatus.isCompleted, true);
        expect(pendingStatus.isCompleted, false);
      });

      test('isPending getter가 정상 동작', () async {
        // given
        await registerUseCase.call(storeId: 101, workType: 'ROOM_TEMP');

        // when
        final result = await useCase.call();

        // then
        final completedStatus =
            result.statusList.firstWhere((s) => s.storeId == 101);
        final pendingStatus =
            result.statusList.firstWhere((s) => s.storeId == 102);

        expect(completedStatus.isPending, false);
        expect(pendingStatus.isPending, true);
      });
    });
  });
}
