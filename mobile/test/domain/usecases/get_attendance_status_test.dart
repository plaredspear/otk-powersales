import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_result.dart';
import 'package:mobile/domain/entities/attendance_status.dart';
import 'package:mobile/domain/repositories/attendance_repository.dart';
import 'package:mobile/domain/usecases/get_attendance_status.dart';

void main() {
  group('GetAttendanceStatus UseCase', () {
    late GetAttendanceStatus useCase;
    late FakeAttendanceRepository repository;

    setUp(() {
      repository = FakeAttendanceRepository();
      useCase = GetAttendanceStatus(repository);
    });

    test('출근등록 현황 조회 성공', () async {
      final result = await useCase.call();

      expect(result.totalCount, 5);
      expect(result.registeredCount, 1);
      expect(result.statusList.length, 5);
      expect(result.currentDate, '2026-03-01');
    });

    test('REGISTERED 상태의 isCompleted는 true', () async {
      final result = await useCase.call();

      final registered =
          result.statusList.firstWhere((s) => s.status == 'REGISTERED');
      expect(registered.isCompleted, true);
      expect(registered.isPending, false);
    });

    test('PENDING 상태의 isPending는 true', () async {
      final result = await useCase.call();

      final pending =
          result.statusList.firstWhere((s) => s.status == 'PENDING');
      expect(pending.isPending, true);
      expect(pending.isCompleted, false);
    });

    test('Repository 에러 시 Exception 전파', () async {
      repository.exceptionToThrow = Exception('서버 오류');

      expect(
        () => useCase.call(),
        throwsA(isA<Exception>()),
      );
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
    required String scheduleSfid,
    required double latitude,
    required double longitude,
    String? workType,
  }) async {
    throw UnimplementedError();
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

const _mockStatusList = [
  AttendanceStatus(
    scheduleSfid: 'a0xXX0000012345',
    storeName: '이마트 해운대점',
    workCategory: '진열',
    status: 'REGISTERED',
    workType: 'ROOM_TEMP',
  ),
  AttendanceStatus(
    scheduleSfid: 'a0xXX0000012346',
    storeName: '홈플러스 서면점',
    workCategory: '순회',
    status: 'PENDING',
  ),
  AttendanceStatus(
    scheduleSfid: 'a0xXX0000012347',
    storeName: '롯데마트 광복점',
    workCategory: '진열',
    status: 'PENDING',
  ),
  AttendanceStatus(
    scheduleSfid: 'a0xXX0000012348',
    storeName: '이마트 사상점',
    workCategory: '순회',
    status: 'PENDING',
  ),
  AttendanceStatus(
    scheduleSfid: 'a0xXX0000012349',
    storeName: '홈플러스 센텀시티점',
    workCategory: '진열',
    status: 'PENDING',
  ),
];
