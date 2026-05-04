import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/my_schedule_api_datasource.dart';
import 'package:mobile/data/models/monthly_schedule_day_model.dart';

void main() {
  late MyScheduleApiDataSource dataSource;
  late Dio dio;

  setUp(() {
    dio = Dio(BaseOptions(baseUrl: 'https://api.test.com'));
    dataSource = MyScheduleApiDataSource(dio);
  });

  group('MyScheduleApiDataSource', () {
    group('getMonthlySchedule', () {
      test('API 응답의 data.work_days를 정상 파싱한다', () async {
        // Given
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.resolve(Response(
              requestOptions: options,
              statusCode: 200,
              data: {
                'success': true,
                'data': {
                  'year': 2026,
                  'month': 3,
                  'workDays': [
                    {'date': '2026-03-01', 'hasWork': true},
                    {'date': '2026-03-02', 'hasWork': false},
                  ],
                },
                'message': '월간 일정 조회 성공',
              },
            ));
          },
        ));

        // When
        final result = await dataSource.getMonthlySchedule(2026, 3);

        // Then
        expect(result.length, 2);
        expect(result[0], MonthlyScheduleDayModel(
          date: DateTime(2026, 3, 1),
          hasWork: true,
        ));
        expect(result[1], MonthlyScheduleDayModel(
          date: DateTime(2026, 3, 2),
          hasWork: false,
        ));
      });

      test('work_days가 빈 배열이면 빈 리스트를 반환한다', () async {
        // Given
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.resolve(Response(
              requestOptions: options,
              statusCode: 200,
              data: {
                'success': true,
                'data': {
                  'year': 2026,
                  'month': 3,
                  'workDays': [],
                },
              },
            ));
          },
        ));

        // When
        final result = await dataSource.getMonthlySchedule(2026, 3);

        // Then
        expect(result, isEmpty);
      });

      test('work_days가 null이면 빈 리스트를 반환한다', () async {
        // Given
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.resolve(Response(
              requestOptions: options,
              statusCode: 200,
              data: {
                'success': true,
                'data': {
                  'year': 2026,
                  'month': 3,
                  'workDays': null,
                },
              },
            ));
          },
        ));

        // When
        final result = await dataSource.getMonthlySchedule(2026, 3);

        // Then
        expect(result, isEmpty);
      });
    });
  });
}
