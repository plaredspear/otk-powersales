import 'dart:io';

import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';

import '../../domain/entities/daily_sales_form.dart';
import '../models/daily_sales_form_model.dart';

/// 일매출 마감/임시저장 API DataSource.
///
/// 대상은 본인 PromotionEmployee 행(promotionEmployeeId)이며 백엔드 경로는
/// `/api/v1/mobile/promotion-employees/{id}/daily-sales`.
class DailySalesApiDataSource {
  final Dio _dio;

  DailySalesApiDataSource(this._dio);

  String _base(int promotionEmployeeId) =>
      '/api/v1/mobile/promotion-employees/$promotionEmployeeId/daily-sales';

  /// 마감 폼 조회 (임시저장 prefill 포함).
  Future<DailySalesForm> getForm(int promotionEmployeeId) async {
    final response = await _dio.get(_base(promotionEmployeeId));
    return DailySalesFormModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  /// 일매출 최종 마감.
  Future<DailySalesCloseResult> close(
    int promotionEmployeeId,
    DailySalesInput input,
    File? photo,
  ) async {
    final formData = await _toFormData(input, photo);
    final response = await _dio.post(
      _base(promotionEmployeeId),
      data: formData,
    );
    return DailySalesCloseResultModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  /// 임시저장 (upsert).
  Future<DailySalesForm> saveDraft(
    int promotionEmployeeId,
    DailySalesInput input,
    File? photo,
  ) async {
    final formData = await _toFormData(input, photo);
    final response = await _dio.post(
      '${_base(promotionEmployeeId)}/draft',
      data: formData,
    );
    return DailySalesFormModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  /// 임시저장 폐기.
  Future<void> deleteDraft(int promotionEmployeeId) async {
    await _dio.delete('${_base(promotionEmployeeId)}/draft');
  }

  Future<FormData> _toFormData(DailySalesInput input, File? photo) async {
    final fields = <String, dynamic>{};
    void put(String key, num? value) {
      if (value != null) fields[key] = value.toString();
    }

    put('primarySalesPrice', input.primarySalesPrice);
    put('primarySalesQuantity', input.primarySalesQuantity);
    put('primaryProductAmount', input.primaryProductAmount);
    put('otherSalesQuantity', input.otherSalesQuantity);
    put('otherSalesAmount', input.otherSalesAmount);
    if (input.description != null && input.description!.isNotEmpty) {
      fields['description'] = input.description!;
    }

    if (photo != null) {
      fields['photo'] = await MultipartFile.fromFile(
        photo.path,
        filename: photo.path.split('/').last,
        contentType: MediaType('image', 'jpeg'),
      );
    }

    return FormData.fromMap(fields);
  }
}
