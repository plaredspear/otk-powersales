import 'package:dio/dio.dart';

import '../../domain/entities/electronic_sales.dart';
import '../models/electronic_sales_model.dart';

/// 전산매출(ABC) API 데이터소스.
///
/// Dio 로 백엔드 `MonthlySalesController` 의 전산매출 엔드포인트와 통신한다.
class ElectronicSalesApiDataSource {
  final Dio _dio;

  ElectronicSalesApiDataSource(this._dio);

  /// 거래처 1곳 + 연월 기준 제품별 전산매출 조회.
  Future<List<ElectronicSales>> getElectronicSales({
    required int customerId,
    required String yearMonth,
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/sales/electronic',
      queryParameters: <String, dynamic>{
        'customerId': customerId,
        'yearMonth': yearMonth,
      },
    );

    return ElectronicSalesModel.listFromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
