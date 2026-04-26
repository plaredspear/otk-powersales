import '../../domain/entities/safety_check_category.dart';
import '../../domain/entities/safety_check_today_status.dart';
import '../../domain/entities/safety_check_submit_result.dart';
import '../../domain/repositories/safety_check_repository.dart';
import '../datasources/safety_check_remote_datasource.dart';

/// 안전점검 Repository 구현체
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
  Future<SafetyCheckSubmitResult> submit({
    required DateTime startTime,
    required DateTime completeTime,
    required List<EquipmentAnswer> equipments,
    List<String>? precautions,
  }) async {
    final model = await _remoteDataSource.submit(
      startTime: startTime,
      completeTime: completeTime,
      equipments: equipments.map((e) => e.toJson()).toList(),
      precautions: precautions,
    );
    return model.toEntity();
  }
}
