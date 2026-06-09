import 'package:dio/dio.dart';

import '../../domain/entities/pos_sales_result.dart';
import '../models/pos_sales_model.dart';

/// POS 매출 API 데이터소스.
///
/// Dio 로 백엔드 `MonthlySalesController` 의 POS 매출(기간) 엔드포인트와 통신한다.
class PosSalesApiDataSource {
  final Dio _dio;

  PosSalesApiDataSource(this._dio);

  /// 거래처 1곳 + 기간 + 선택 바코드 기준 제품별 POS 매출 조회.
  Future<PosSalesResult> getPosSalesByRange({
    required int customerId,
    required String startDate,
    required String endDate,
    List<String> barcodes = const [],
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/sales/pos/by-range',
      queryParameters: <String, dynamic>{
        'customerId': customerId,
        'startDate': startDate,
        'endDate': endDate,
        // 백엔드는 쉼표 구분 문자열을 받아 목록으로 파싱 (List 바인딩 모호성 회피).
        if (barcodes.isNotEmpty) 'barcodes': barcodes.join(','),
      },
    );

    return PosSalesModel.resultFromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
