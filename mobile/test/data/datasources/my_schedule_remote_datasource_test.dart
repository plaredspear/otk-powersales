import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/my_schedule_remote_datasource.dart';
import 'package:mobile/data/models/daily_schedule_info_model.dart';
import 'package:mobile/data/models/monthly_schedule_day_model.dart';

class _MockMyScheduleRemoteDataSource implements MyScheduleRemoteDataSource {
  List<MonthlyScheduleDayModel>? monthlySchedule;
  DailyScheduleInfoModel? dailySchedule;
  Exception? error;

  @override
  Future<List<MonthlyScheduleDayModel>> getMonthlySchedule(
    int year,
    int month,
  ) async {
    if (error != null) throw error!;
    return monthlySchedule!;
  }

  @override
  Future<DailyScheduleInfoModel> getDailySchedule(String date) async {
    if (error != null) throw error!;
    return dailySchedule!;
  }
}

void main() {
  group('MyScheduleRemoteDataSource', () {
    late _MockMyScheduleRemoteDataSource dataSource;

    setUp(() {
      dataSource = _MockMyScheduleRemoteDataSource();
    });

    group('getMonthlySchedule', () {
      test('월간 일정 조회 메서드가 호출 가능하다', () async {
        // Given
        final expectedSchedule = [
          MonthlyScheduleDayModel(
            date: DateTime(2026, 2, 1),
            hasWork: false,
          ),
          MonthlyScheduleDayModel(
            date: DateTime(2026, 2, 3),
            hasWork: true,
          ),
        ];
        dataSource.monthlySchedule = expectedSchedule;

        // When
        final result = await dataSource.getMonthlySchedule(2026, 2);

        // Then
        expect(result, expectedSchedule);
        expect(result.length, 2);
      });

      test('DataSource에서 에러가 발생하면 전파된다', () async {
        // Given
        dataSource.error = Exception('Network error');

        // When & Then
        expect(
          () => dataSource.getMonthlySchedule(2026, 2),
          throwsException,
        );
      });
    });

    group('getDailySchedule', () {
      test('일간 일정 조회 메서드가 호출 가능하다', () async {
        // Given
        final expectedSchedule = DailyScheduleInfoModel(
          date: '2026-02-04',
          dayOfWeek: '화',
          memberName: '최금주',
          employeeNumber: '20030117',
          reportProgress: const ReportProgressModel(
            completed: 0,
            total: 3,
            workType: '진열',
          ),
          stores: const [],
        );
        dataSource.dailySchedule = expectedSchedule;

        // When
        final result = await dataSource.getDailySchedule('2026-02-04');

        // Then
        expect(result, expectedSchedule);
        expect(result.memberName, '최금주');
      });

      test('DataSource에서 에러가 발생하면 전파된다', () async {
        // Given
        dataSource.error = Exception('Network error');

        // When & Then
        expect(
          () => dataSource.getDailySchedule('2026-02-04'),
          throwsException,
        );
      });
    });
  });
}
