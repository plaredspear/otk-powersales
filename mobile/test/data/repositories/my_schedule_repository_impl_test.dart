import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/my_schedule_remote_datasource.dart';
import 'package:mobile/data/models/daily_schedule_info_model.dart';
import 'package:mobile/data/models/monthly_schedule_day_model.dart';
import 'package:mobile/data/repositories/my_schedule_repository_impl.dart';

void main() {
  late MyScheduleRepositoryImpl repository;
  late FakeMyScheduleRemoteDataSource fakeRemoteDataSource;

  setUp(() {
    fakeRemoteDataSource = FakeMyScheduleRemoteDataSource();
    repository = MyScheduleRepositoryImpl(
      remoteDataSource: fakeRemoteDataSource,
    );
  });

  group('MyScheduleRepositoryImpl', () {
    group('getMonthlySchedule', () {
      test('월간 일정을 remote datasource에서 가져와 Entity로 변환한다', () async {
        // Given
        final mockModels = [
          MonthlyScheduleDayModel(
            date: DateTime(2026, 2, 1),
            hasWork: false,
          ),
          MonthlyScheduleDayModel(
            date: DateTime(2026, 2, 3),
            hasWork: true,
          ),
        ];
        fakeRemoteDataSource.monthlyScheduleResult = mockModels;

        // When
        final result = await repository.getMonthlySchedule(2026, 2);

        // Then
        expect(fakeRemoteDataSource.getMonthlyScheduleCalls, 1);
        expect(fakeRemoteDataSource.lastYear, 2026);
        expect(fakeRemoteDataSource.lastMonth, 2);
        expect(result.length, 2);
        expect(result[0].date, DateTime(2026, 2, 1));
        expect(result[0].hasWork, false);
        expect(result[1].date, DateTime(2026, 2, 3));
        expect(result[1].hasWork, true);
      });

      test('빈 목록을 올바르게 처리한다', () async {
        // Given
        fakeRemoteDataSource.monthlyScheduleResult = [];

        // When
        final result = await repository.getMonthlySchedule(2026, 2);

        // Then
        expect(result, isEmpty);
      });

      test('DataSource 예외를 전파한다', () async {
        // Given
        fakeRemoteDataSource.monthlyScheduleException =
            Exception('Network error');

        // When & Then
        expect(
          () => repository.getMonthlySchedule(2026, 2),
          throwsException,
        );
      });
    });

    group('getDailySchedule', () {
      test('일간 일정을 remote datasource에서 가져와 Entity로 변환한다', () async {
        // Given
        final mockModel = DailyScheduleInfoModel(
          date: '2026-02-04',
          dayOfWeek: '화',
          memberName: '최금주',
          employeeNumber: '20030117',
          reportProgress: const ReportProgressModel(
            completed: 0,
            total: 3,
            workType: '진열',
          ),
          stores: const [
            ScheduleStoreDetailModel(
              storeId: 1,
              storeName: '이마트',
              workType1: '진열',
              workType2: '전담',
              workType3: '순회',
              isRegistered: false,
            ),
          ],
        );
        fakeRemoteDataSource.dailyScheduleResult = mockModel;

        // When
        final result = await repository.getDailySchedule(DateTime(2026, 2, 4));

        // Then
        expect(fakeRemoteDataSource.getDailyScheduleCalls, 1);
        expect(fakeRemoteDataSource.lastDateString, '2026-02-04');
        expect(result.date, '2026년 02월 04일(화)');
        expect(result.memberName, '최금주');
        expect(result.employeeNumber, '20030117');
        expect(result.stores.length, 1);
        expect(result.stores[0].storeName, '이마트');
      });

      test('DateTime을 YYYY-MM-DD 형식 문자열로 변환한다', () async {
        // Given
        final mockModel = DailyScheduleInfoModel(
          date: '2026-12-25',
          dayOfWeek: '금',
          memberName: '홍길동',
          employeeNumber: '12345678',
          reportProgress: const ReportProgressModel(
            completed: 0,
            total: 0,
            workType: '순회',
          ),
          stores: const [],
        );
        fakeRemoteDataSource.dailyScheduleResult = mockModel;

        // When
        await repository.getDailySchedule(DateTime(2026, 12, 25));

        // Then
        expect(fakeRemoteDataSource.lastDateString, '2026-12-25');
      });

      test('한 자리 월/일도 올바르게 포맷한다', () async {
        // Given
        final mockModel = DailyScheduleInfoModel(
          date: '2026-01-05',
          dayOfWeek: '월',
          memberName: '홍길동',
          employeeNumber: '12345678',
          reportProgress: const ReportProgressModel(
            completed: 0,
            total: 0,
            workType: '순회',
          ),
          stores: const [],
        );
        fakeRemoteDataSource.dailyScheduleResult = mockModel;

        // When
        await repository.getDailySchedule(DateTime(2026, 1, 5));

        // Then
        expect(fakeRemoteDataSource.lastDateString, '2026-01-05');
      });

      test('DataSource 예외를 전파한다', () async {
        // Given
        fakeRemoteDataSource.dailyScheduleException =
            Exception('Network error');

        // When & Then
        expect(
          () => repository.getDailySchedule(DateTime(2026, 2, 4)),
          throwsException,
        );
      });
    });
  });
}

/// MyScheduleRemoteDataSource Fake 구현
///
/// 테스트를 위해 네트워크 호출을 시뮬레이션합니다.
class FakeMyScheduleRemoteDataSource implements MyScheduleRemoteDataSource {
  // getMonthlySchedule
  int getMonthlyScheduleCalls = 0;
  int? lastYear;
  int? lastMonth;
  List<MonthlyScheduleDayModel>? monthlyScheduleResult;
  Exception? monthlyScheduleException;

  // getDailySchedule
  int getDailyScheduleCalls = 0;
  String? lastDateString;
  DailyScheduleInfoModel? dailyScheduleResult;
  Exception? dailyScheduleException;

  @override
  Future<List<MonthlyScheduleDayModel>> getMonthlySchedule(
    int year,
    int month,
  ) async {
    getMonthlyScheduleCalls++;
    lastYear = year;
    lastMonth = month;

    if (monthlyScheduleException != null) {
      throw monthlyScheduleException!;
    }

    return monthlyScheduleResult!;
  }

  @override
  Future<DailyScheduleInfoModel> getDailySchedule(String date) async {
    getDailyScheduleCalls++;
    lastDateString = date;

    if (dailyScheduleException != null) {
      throw dailyScheduleException!;
    }

    return dailyScheduleResult!;
  }
}
