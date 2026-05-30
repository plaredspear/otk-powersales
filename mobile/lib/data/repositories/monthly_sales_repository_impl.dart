import '../../domain/entities/monthly_sales.dart';
import '../../domain/repositories/monthly_sales_repository.dart';
import '../datasources/monthly_sales_remote_datasource.dart';

/// MonthlySales Repository 구현체
///
/// Remote DataSource에서 데이터를 가져와 Domain Entity로 변환한다.
class MonthlySalesRepositoryImpl implements MonthlySalesRepository {
  final MonthlySalesRemoteDataSource _remoteDataSource;

  MonthlySalesRepositoryImpl({
    required MonthlySalesRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<MonthlySales> getMonthlySales({
    String? customerId,
    required String yearMonth,
  }) async {
    final model = await _remoteDataSource.getMonthlySales(
      customerId: customerId,
      yearMonth: yearMonth,
    );
    return model.toEntity();
  }
}
