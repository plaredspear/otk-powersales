import 'package:dio/dio.dart';

import '../../domain/entities/electronic_sales.dart';
import '../models/electronic_sales_model.dart';

/// 전산매출(ABC) API 데이터소스.
///
/// Dio 로 백엔드 `MonthlySalesController` 의 전산매출 엔드포인트와 통신한다.
class ElectronicSalesApiDataSource {
  final Dio _dio;

  ElectronicSalesApiDataSource(this._dio);

  /// 거래처 1곳 + 기간 + 매출 조회 제품(바코드) 기준 전산매출 조회.
  ///
  /// [barcodes] 가 비어 있으면 합계금액만(레거시 `abcSumAmount`), 있으면 해당 제품별 실적을
  /// 조회한다(레거시 `abcAmount`).
  Future<ElectronicSalesResult> getElectronicSales({
    required int customerId,
    required String startDate,
    required String endDate,
    List<String> barcodes = const [],
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/sales/electronic',
      queryParameters: <String, dynamic>{
        'customerId': customerId,
        'startDate': startDate,
        'endDate': endDate,
        if (barcodes.isNotEmpty) 'barcodes': barcodes,
      },
    );

    return ElectronicSalesModel.resultFromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
