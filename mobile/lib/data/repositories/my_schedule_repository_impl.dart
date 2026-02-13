import '../../domain/entities/daily_schedule_info.dart';
import '../../domain/entities/monthly_schedule_day.dart';
import '../../domain/repositories/my_schedule_repository.dart';
import '../datasources/my_schedule_remote_datasource.dart';

/// 마이페이지 일정 Repository 구현체
///
/// 실제 API DataSource를 사용하여 일정 기능을 구현합니다.
/// Backend API 연동 시점에 MyScheduleMockRepository를 대체합니다.
class MyScheduleRepositoryImpl implements MyScheduleRepository {
  final MyScheduleRemoteDataSource _remoteDataSource;

  MyScheduleRepositoryImpl({
    required MyScheduleRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<List<MonthlyScheduleDay>> getMonthlySchedule(
    int year,
    int month,
  ) async {
    final models = await _remoteDataSource.getMonthlySchedule(year, month);
    return models.map((model) => model.toEntity()).toList();
  }

  @override
  Future<DailyScheduleInfo> getDailySchedule(DateTime date) async {
    // DateTime을 YYYY-MM-DD 형식 문자열로 변환
    final dateString =
        '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

    final model = await _remoteDataSource.getDailySchedule(dateString);
    return model.toEntity();
  }
}
