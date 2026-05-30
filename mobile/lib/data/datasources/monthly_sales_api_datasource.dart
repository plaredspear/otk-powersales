import 'package:dio/dio.dart';

import '../models/monthly_sales_model.dart';
import 'monthly_sales_remote_datasource.dart';

/// 월매출 API 데이터소스 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API(MonthlySalesController)와
/// 통신합니다.
class MonthlySalesApiDataSource implements MonthlySalesRemoteDataSource {
  final Dio _dio;

  MonthlySalesApiDataSource(this._dio);

  @override
  Future<MonthlySalesModel> getMonthlySales({
    String? customerId,
    required String yearMonth,
  }) async {
    final queryParameters = <String, dynamic>{
      'yearMonth': yearMonth,
    };
    if (customerId != null && customerId.isNotEmpty) {
      queryParameters['customerId'] = customerId;
    }

    final response = await _dio.get(
      '/api/v1/mobile/sales/monthly',
      queryParameters: queryParameters,
    );

    return MonthlySalesModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
