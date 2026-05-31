import 'package:dio/dio.dart';

import '../../domain/entities/logistics_sales.dart';
import '../models/logistics_sales_model.dart';

/// 물류매출 API 데이터소스.
///
/// Dio 로 백엔드 `MonthlySalesController` 의 물류매출 엔드포인트와 통신한다.
class LogisticsSalesApiDataSource {
  final Dio _dio;

  LogisticsSalesApiDataSource(this._dio);

  /// 거래처 1곳 + 연월 기준 온도대별 물류매출 조회.
  Future<List<LogisticsSales>> getLogisticsSales({
    required int customerId,
    required String yearMonth,
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/sales/logistics',
      queryParameters: <String, dynamic>{
        'customerId': customerId,
        'yearMonth': yearMonth,
      },
    );

    return LogisticsSalesModel.listFromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
