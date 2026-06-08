import 'package:dio/dio.dart';

import '../../domain/entities/pos_sales.dart';
import '../models/pos_sales_model.dart';

/// POS 매출 API 데이터소스.
///
/// Dio 로 백엔드 `MonthlySalesController` 의 POS 매출 엔드포인트와 통신한다.
class PosSalesApiDataSource {
  final Dio _dio;

  PosSalesApiDataSource(this._dio);

  /// 거래처 1곳 + 연월 기준 제품별 POS 매출 조회.
  Future<List<PosSales>> getPosSales({
    required int customerId,
    required String yearMonth,
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/sales/pos',
      queryParameters: <String, dynamic>{
        'customerId': customerId,
        'yearMonth': yearMonth,
      },
    );

    return PosSalesModel.listFromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
