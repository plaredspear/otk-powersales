import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/mock/home_mock_data.dart';
import 'package:mobile/data/repositories/mock/home_mock_repository.dart';
import 'package:mobile/domain/entities/expiry_alert.dart';
import 'package:mobile/domain/entities/notice.dart';
import 'package:mobile/domain/entities/schedule.dart';

void main() {
  group('HomeMockRepository', () {
    late HomeMockRepository repository;

    setUp(() {
      repository = HomeMockRepository();
    });

    group('getHomeData', () {
      test('기본 Mock 데이터를 올바르게 반환한다', () async {
        final result = await repository.getHomeData();

        expect(result.todaySchedules, isNotEmpty);
        expect(result.todaySchedules.length, HomeMockData.todaySchedules.length);
        expect(result.expiryAlert, isNotNull);
        expect(result.notices, isNotEmpty);
        expect(result.currentDate, HomeMockData.currentDate);
      });

      test('일정 데이터가 올바르게 포함된다', () async {
        final result = await repository.getHomeData();

        final firstSchedule = result.todaySchedules.first;
        expect(firstSchedule.storeName, '이마트 부산점');
        expect(firstSchedule.startTime, '09:00');
        expect(firstSchedule.endTime, '12:00');
        expect(firstSchedule.type, '순회');
      });

      test('유통기한 알림 데이터가 올바르게 포함된다', () async {
        final result = await repository.getHomeData();

        expect(result.expiryAlert!.branchName, '부산1지점');
        expect(result.expiryAlert!.employeeName, '최금주');
        expect(result.expiryAlert!.employeeId, '20030117');
        expect(result.expiryAlert!.expiryCount, 1);
      });

      test('공지사항 데이터가 올바르게 포함된다', () async {
        final result = await repository.getHomeData();

        expect(result.notices.length, 5);
        expect(result.notices.first.title, '2월 영업 목표 달성 현황');
        expect(result.notices.first.type, 'BRANCH');
      });

      test('커스텀 일정 데이터를 사용할 수 있다', () async {
        repository.customSchedules = const [
          Schedule(
            id: 99,
            storeName: '테스트 매장',
            startTime: '10:00',
            endTime: '11:00',
            type: '순회',
          ),
        ];

        final result = await repository.getHomeData();

        expect(result.todaySchedules.length, 1);
        expect(result.todaySchedules.first.storeName, '테스트 매장');
      });

      test('빈 일정 목록을 설정할 수 있다', () async {
        repository.customSchedules = HomeMockData.emptySchedules;

        final result = await repository.getHomeData();

        expect(result.todaySchedules, isEmpty);
      });

      test('유통기한 알림을 null로 설정할 수 있다', () async {
        repository.useNullExpiryAlert = true;

        final result = await repository.getHomeData();

        expect(result.expiryAlert, isNull);
      });

      test('커스텀 유통기한 알림을 설정할 수 있다', () async {
        repository.customExpiryAlert = const ExpiryAlert(
          branchName: '서울2지점',
          employeeName: '홍길동',
          employeeId: '20010585',
          expiryCount: 5,
        );

        final result = await repository.getHomeData();

        expect(result.expiryAlert!.branchName, '서울2지점');
        expect(result.expiryAlert!.expiryCount, 5);
      });

      test('커스텀 공지사항을 설정할 수 있다', () async {
        repository.customNotices = [
          Notice(
            id: 99,
            title: '테스트 공지',
            type: 'ALL',
            createdAt: DateTime.parse('2026-02-07T09:00:00.000Z'),
          ),
        ];

        final result = await repository.getHomeData();

        expect(result.notices.length, 1);
        expect(result.notices.first.title, '테스트 공지');
      });

      test('빈 공지사항 목록을 설정할 수 있다', () async {
        repository.customNotices = HomeMockData.emptyNotices;

        final result = await repository.getHomeData();

        expect(result.notices, isEmpty);
      });

      test('커스텀 날짜를 설정할 수 있다', () async {
        repository.customCurrentDate = '2026-03-01';

        final result = await repository.getHomeData();

        expect(result.currentDate, '2026-03-01');
      });

      test('Exception 설정 시 예외를 발생시킨다', () async {
        repository.exceptionToThrow = Exception('네트워크 오류');

        expect(
          () => repository.getHomeData(),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('네트워크 오류'),
            ),
          ),
        );
      });
    });
  });
}
