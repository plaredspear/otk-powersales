import '../../domain/entities/safety_check_category.dart';
import '../../domain/entities/safety_check_today_status.dart';
import '../../domain/entities/safety_check_submit_result.dart';
import '../../domain/repositories/safety_check_repository.dart';
import '../datasources/safety_check_remote_datasource.dart';

/// 안전점검 Repository 구현체
///
/// 실제 API DataSource를 사용하여 안전점검 기능을 구현합니다.
/// Backend API 연동 시점에 SafetyCheckMockRepository를 대체합니다.
class SafetyCheckRepositoryImpl implements SafetyCheckRepository {
  final SafetyCheckRemoteDataSource _remoteDataSource;

  SafetyCheckRepositoryImpl({
    required SafetyCheckRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<List<SafetyCheckCategory>> getItems() async {
    final models = await _remoteDataSource.getItems();
    return models.map((model) => model.toEntity()).toList();
  }

  @override
  Future<SafetyCheckTodayStatus> getTodayStatus() async {
    final model = await _remoteDataSource.getTodayStatus();
    return model.toEntity();
  }

  @override
  Future<SafetyCheckSubmitResult> submit(List<int> checkedItemIds) async {
    final model = await _remoteDataSource.submit(checkedItemIds);
    return model.toEntity();
  }
}
