import '../../domain/entities/expiry_alert.dart';
import '../../domain/repositories/home_repository.dart';
import '../datasources/home_remote_datasource.dart';

/// Home Repository 구현체
///
/// Remote DataSource에서 데이터를 가져와 Domain Entity로 변환한다.
class HomeRepositoryImpl implements HomeRepository {
  final HomeRemoteDataSource _remoteDataSource;

  HomeRepositoryImpl({
    required HomeRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<HomeData> getHomeData() async {
    final response = await _remoteDataSource.getHomeData();

    final todaySchedules = response.todaySchedules
        .map((model) => model.toEntity())
        .toList();

    final ExpiryAlert? expiryAlert = response.expiryAlert?.toEntity();

    final notices = response.notices
        .map((model) => model.toEntity())
        .toList();

    return HomeData(
      todaySchedules: todaySchedules,
      expiryAlert: expiryAlert,
      notices: notices,
      currentDate: response.currentDate,
    );
  }
}
