import '../../domain/entities/electronic_sales.dart';
import '../../domain/repositories/electronic_sales_repository.dart';
import '../datasources/electronic_sales_api_datasource.dart';

/// 전산매출(ABC) Repository 실 API 구현체.
class ElectronicSalesRepositoryImpl implements ElectronicSalesRepository {
  final ElectronicSalesApiDataSource _remoteDataSource;

  ElectronicSalesRepositoryImpl({
    required ElectronicSalesApiDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<ElectronicSalesResult> getElectronicSales({
    required int customerId,
    required String startDate,
    required String endDate,
    List<String> barcodes = const [],
  }) {
    return _remoteDataSource.getElectronicSales(
      customerId: customerId,
      startDate: startDate,
      endDate: endDate,
      barcodes: barcodes,
    );
  }
}
