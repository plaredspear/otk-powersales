import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/expiry_alert.dart';
import 'package:mobile/domain/entities/notice.dart';
import 'package:mobile/domain/entities/schedule.dart';
import 'package:mobile/domain/repositories/home_repository.dart';
import 'package:mobile/domain/usecases/get_home_data.dart';

/// 테스트용 Mock HomeRepository
class MockHomeRepository implements HomeRepository {
  HomeData? homeData;
  Exception? exceptionToThrow;
  int callCount = 0;

  @override
  Future<HomeData> getHomeData() async {
    callCount++;
    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return homeData!;
  }
}

void main() {
  group('GetHomeData', () {
    late GetHomeData useCase;
    late MockHomeRepository mockRepository;

    final testHomeData = HomeData(
      todaySchedules: const [
        Schedule(
          id: 1,
          storeName: '이마트 부산점',
          startTime: '09:00',
          endTime: '12:00',
          type: '순회',
        ),
      ],
      expiryAlert: const ExpiryAlert(
        branchName: '부산1지점',
        employeeName: '최금주',
        employeeId: '20030117',
        expiryCount: 1,
      ),
      notices: [
        Notice(
          id: 1,
          title: '2월 영업 목표 달성 현황',
          type: 'BRANCH',
          createdAt: DateTime.parse('2026-02-05T10:00:00.000Z'),
        ),
      ],
      currentDate: '2026-02-07',
    );

    setUp(() {
      mockRepository = MockHomeRepository();
      useCase = GetHomeData(mockRepository);
    });

    test('정상 조회 시 HomeData를 반환한다', () async {
      mockRepository.homeData = testHomeData;

      final result = await useCase();

      expect(result, testHomeData);
      expect(result.todaySchedules.length, 1);
      expect(result.expiryAlert, isNotNull);
      expect(result.notices.length, 1);
      expect(result.currentDate, '2026-02-07');
    });

    test('Repository를 정확히 1회 호출한다', () async {
      mockRepository.homeData = testHomeData;

      await useCase();

      expect(mockRepository.callCount, 1);
    });

    test('일정이 빈 배열인 HomeData를 반환할 수 있다', () async {
      mockRepository.homeData = const HomeData(
        todaySchedules: [],
        expiryAlert: null,
        notices: [],
        currentDate: '2026-02-07',
      );

      final result = await useCase();

      expect(result.todaySchedules, isEmpty);
      expect(result.expiryAlert, isNull);
      expect(result.notices, isEmpty);
    });

    test('expiryAlert가 null인 HomeData를 반환할 수 있다', () async {
      mockRepository.homeData = HomeData(
        todaySchedules: const [
          Schedule(
            id: 1,
            storeName: '이마트 부산점',
            startTime: '09:00',
            endTime: '12:00',
            type: '순회',
          ),
        ],
        expiryAlert: null,
        notices: [
          Notice(
            id: 1,
            title: '테스트 공지',
            type: 'ALL',
            createdAt: DateTime.parse('2026-02-05T10:00:00.000Z'),
          ),
        ],
        currentDate: '2026-02-07',
      );

      final result = await useCase();

      expect(result.expiryAlert, isNull);
      expect(result.todaySchedules, isNotEmpty);
      expect(result.notices, isNotEmpty);
    });

    test('Repository에서 Exception 발생 시 그대로 전파한다', () async {
      mockRepository.exceptionToThrow = Exception('네트워크 오류');

      expect(
        () => useCase(),
        throwsA(
          isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('네트워크 오류'),
          ),
        ),
      );
    });

    test('Repository에서 서버 오류 발생 시 그대로 전파한다', () async {
      mockRepository.exceptionToThrow = Exception('서비스 일시 장애');

      expect(
        () => useCase(),
        throwsA(
          isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('서비스 일시 장애'),
          ),
        ),
      );
    });
  });
}
