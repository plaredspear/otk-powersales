import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_result.dart';
import 'package:mobile/domain/repositories/attendance_repository.dart';
import 'package:mobile/domain/usecases/register_attendance.dart';

void main() {
  group('RegisterAttendance UseCase', () {
    late RegisterAttendance useCase;
    late FakeAttendanceRepository repository;

    setUp(() {
      repository = FakeAttendanceRepository();
      useCase = RegisterAttendance(repository);
    });

    group('정상 등록', () {
      test('출근 등록 성공', () async {
        final result = await useCase.call(
          scheduleId: 12345,
          latitude: 35.1696,
          longitude: 129.1318,
          workType: 'ROOM_TEMP',
        );

        expect(result.scheduleId, 12345);
        expect(result.storeName, '이마트 해운대점');
        expect(result.workType, 'ROOM_TEMP');
        expect(result.distanceKm, 0.12);
        expect(result.totalCount, 5);
        expect(result.registeredCount, 1);
      });

      test('workType 없이 출근 등록 성공', () async {
        final result = await useCase.call(
          scheduleId: 12345,
          latitude: 35.1696,
          longitude: 129.1318,
        );

        expect(result.scheduleId, 12345);
        expect(result.storeName, '이마트 해운대점');
      });

      test('AttendanceResult 계산 getter 동작', () async {
        final result = await useCase.call(
          scheduleId: 12345,
          latitude: 35.0,
          longitude: 129.0,
          workType: 'ROOM_TEMP',
        );

        expect(result.isAllRegistered, false);
        expect(result.remainingCount, 4);
      });
    });

    group('입력값 검증', () {
      test('유효하지 않은 scheduleId는 ArgumentError 발생', () {
        expect(
          () => useCase.call(
            scheduleId: 0,
            latitude: 35.0,
            longitude: 129.0,
          ),
          throwsA(isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            '유효하지 않은 거래처입니다',
          )),
        );
      });
    });

    group('에러 처리', () {
      test('Repository 에러 시 Exception 전파', () async {
        repository.exceptionToThrow = Exception('이미 출근 등록된 스케줄입니다');

        expect(
          () => useCase.call(
            scheduleId: 12345,
            latitude: 35.0,
            longitude: 129.0,
          ),
          throwsA(isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('이미 출근 등록된 스케줄입니다'),
          )),
        );
      });
    });
  });
}

// --- Fake ---

class FakeAttendanceRepository implements AttendanceRepository {
  Exception? exceptionToThrow;

  @override
  Future<StoreListResult> getStoreList({String? keyword}) async {
    throw UnimplementedError();
  }

  @override
  Future<AttendanceResult> registerAttendance({
    required int scheduleId,
    required double latitude,
    required double longitude,
    String? workType,
  }) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return AttendanceResult(
      scheduleId: scheduleId,
      storeName: '이마트 해운대점',
      workType: workType ?? '진열',
      distanceKm: 0.12,
      totalCount: 5,
      registeredCount: 1,
    );
  }

  @override
  Future<AttendanceStatusResult> getAttendanceStatus() async {
    throw UnimplementedError();
  }
}
