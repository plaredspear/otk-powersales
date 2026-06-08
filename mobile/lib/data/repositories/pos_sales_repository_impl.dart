import '../../domain/entities/pos_sales.dart';
import '../../domain/repositories/pos_sales_repository.dart';
import '../datasources/pos_sales_api_datasource.dart';

/// POS 매출 Repository 실 API 구현체.
class PosSalesRepositoryImpl implements PosSalesRepository {
  final PosSalesApiDataSource _remoteDataSource;

  PosSalesRepositoryImpl({
    required PosSalesApiDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<List<PosSales>> getPosSales({
    required int customerId,
    required String yearMonth,
  }) {
    return _remoteDataSource.getPosSales(
      customerId: customerId,
      yearMonth: yearMonth,
    );
  }
}
