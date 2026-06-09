import '../../domain/entities/pos_sales_result.dart';
import '../../domain/repositories/pos_sales_repository.dart';
import '../datasources/pos_sales_api_datasource.dart';

/// POS 매출 Repository 실 API 구현체.
class PosSalesRepositoryImpl implements PosSalesRepository {
  final PosSalesApiDataSource _remoteDataSource;

  PosSalesRepositoryImpl({
    required PosSalesApiDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<PosSalesResult> getPosSalesByRange({
    required int customerId,
    required String startDate,
    required String endDate,
    List<String> barcodes = const [],
  }) {
    return _remoteDataSource.getPosSalesByRange(
      customerId: customerId,
      startDate: startDate,
      endDate: endDate,
      barcodes: barcodes,
    );
  }
}
