import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/usecases/register_attendance.dart';
import 'package:mobile/data/repositories/mock/attendance_mock_repository.dart';

void main() {
  group('RegisterAttendance UseCase', () {
    late RegisterAttendance useCase;
    late AttendanceMockRepository repository;

    setUp(() {
      repository = AttendanceMockRepository(workerType: 'PATROL');
      useCase = RegisterAttendance(repository);
    });

    group('정상 등록', () {
      test('ROOM_TEMP 유형으로 출근 등록 성공', () async {
        // when
        final result = await useCase.call(
          storeId: 101,
          workType: 'ROOM_TEMP',
        );

        // then
        expect(result.attendanceId, isPositive);
        expect(result.storeId, 101);
        expect(result.storeName, '이마트 해운대점');
        expect(result.workType, 'ROOM_TEMP');
        expect(result.registeredAt, isNotNull);
        expect(result.totalCount, 5);
        expect(result.registeredCount, 1);
      });

      test('REFRIGERATED 유형으로 출근 등록 성공', () async {
        // when
        final result = await useCase.call(
          storeId: 102,
          workType: 'REFRIGERATED',
        );

        // then
        expect(result.attendanceId, isPositive);
        expect(result.storeId, 102);
        expect(result.storeName, '홈플러스 서면점');
        expect(result.workType, 'REFRIGERATED');
        expect(result.registeredAt, isNotNull);
      });

      test('AttendanceResult는 모든 필수 필드를 포함', () async {
        // when
        final result = await useCase.call(
          storeId: 103,
          workType: 'ROOM_TEMP',
        );

        // then
        expect(result.attendanceId, isA<int>());
        expect(result.storeId, isA<int>());
        expect(result.storeName, isA<String>());
        expect(result.workType, isA<String>());
        expect(result.registeredAt, isA<DateTime>());
        expect(result.totalCount, isA<int>());
        expect(result.registeredCount, isA<int>());
      });

      test('5개 중 1개 등록 후 isAllRegistered는 false', () async {
        // when
        final result = await useCase.call(
          storeId: 101,
          workType: 'ROOM_TEMP',
        );

        // then
        expect(result.isAllRegistered, false);
        expect(result.registeredCount, 1);
        expect(result.totalCount, 5);
        expect(result.remainingCount, 4);
      });
    });

    group('입력값 검증', () {
      test('storeId가 0 이하인 경우 ArgumentError 발생', () async {
        // when & then
        expect(
          () => useCase.call(storeId: 0, workType: 'ROOM_TEMP'),
          throwsA(isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            '유효하지 않은 거래처입니다',
          )),
        );
      });

      test('storeId가 음수인 경우 ArgumentError 발생', () async {
        // when & then
        expect(
          () => useCase.call(storeId: -1, workType: 'ROOM_TEMP'),
          throwsA(isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            '유효하지 않은 거래처입니다',
          )),
        );
      });

      test('유효하지 않은 workType은 ArgumentError 발생', () async {
        // when & then
        expect(
          () => useCase.call(storeId: 101, workType: 'INVALID_TYPE'),
          throwsA(isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            '유효하지 않은 근무유형입니다',
          )),
        );
      });

      test('빈 문자열 workType은 ArgumentError 발생', () async {
        // when & then
        expect(
          () => useCase.call(storeId: 101, workType: ''),
          throwsA(isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            '유효하지 않은 근무유형입니다',
          )),
        );
      });
    });

    group('중복 등록 방지', () {
      test('이미 등록된 거래처에 재등록 시 Exception 발생', () async {
        // given
        await useCase.call(storeId: 101, workType: 'ROOM_TEMP');

        // when & then
        expect(
          () => useCase.call(storeId: 101, workType: 'REFRIGERATED'),
          throwsA(isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('이미 출근등록된 거래처입니다'),
          )),
        );
      });
    });

    group('IRREGULAR 근무자 제한', () {
      test('IRREGULAR 근무자는 최대 2개 거래처만 등록 가능', () async {
        // given
        repository = AttendanceMockRepository(workerType: 'IRREGULAR');
        useCase = RegisterAttendance(repository);

        // when - 2개 등록 성공
        await useCase.call(storeId: 101, workType: 'ROOM_TEMP');
        await useCase.call(storeId: 102, workType: 'REFRIGERATED');

        // then - 3번째 등록 시 Exception 발생
        expect(
          () => useCase.call(storeId: 103, workType: 'ROOM_TEMP'),
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
        useCase = RegisterAttendance(repository);

        // when
        final result1 = await useCase.call(storeId: 101, workType: 'ROOM_TEMP');
        final result2 =
            await useCase.call(storeId: 102, workType: 'REFRIGERATED');

        // then
        expect(result1.registeredCount, 1);
        expect(result2.registeredCount, 2);
      });
    });

    group('attendanceId 자동 증가', () {
      test('등록마다 attendanceId가 자동 증가', () async {
        // when
        final result1 = await useCase.call(storeId: 101, workType: 'ROOM_TEMP');
        final result2 =
            await useCase.call(storeId: 102, workType: 'REFRIGERATED');
        final result3 = await useCase.call(storeId: 103, workType: 'ROOM_TEMP');

        // then
        expect(result2.attendanceId, greaterThan(result1.attendanceId));
        expect(result3.attendanceId, greaterThan(result2.attendanceId));
      });
    });
  });
}
