import '../../domain/entities/staff_evaluation.dart';
import '../../domain/repositories/staff_evaluation_repository.dart';
import '../datasources/staff_evaluation_remote_datasource.dart';

/// StaffEvaluation Repository 구현체
class StaffEvaluationRepositoryImpl implements StaffEvaluationRepository {
  final StaffEvaluationRemoteDataSource _remoteDataSource;

  StaffEvaluationRepositoryImpl({
    required StaffEvaluationRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<StaffEvaluation> getStaffEvaluation({String? yearMonth}) async {
    final model =
        await _remoteDataSource.getStaffEvaluation(yearMonth: yearMonth);
    return model.toEntity();
  }
}
