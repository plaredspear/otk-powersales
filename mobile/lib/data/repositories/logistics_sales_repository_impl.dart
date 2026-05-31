import '../../domain/entities/logistics_sales.dart';
import '../../domain/repositories/logistics_sales_repository.dart';
import '../datasources/logistics_sales_api_datasource.dart';

/// 물류매출 Repository 실 API 구현체.
class LogisticsSalesRepositoryImpl implements LogisticsSalesRepository {
  final LogisticsSalesApiDataSource _remoteDataSource;

  LogisticsSalesRepositoryImpl({
    required LogisticsSalesApiDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<List<LogisticsSales>> getLogisticsSales({
    required int customerId,
    required String yearMonth,
  }) {
    return _remoteDataSource.getLogisticsSales(
      customerId: customerId,
      yearMonth: yearMonth,
    );
  }
}
