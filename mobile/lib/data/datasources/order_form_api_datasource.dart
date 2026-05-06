import 'package:dio/dio.dart';

import '../models/order_form/loan_inquiry_response_model.dart';
import '../models/order_form/order_draft_request_model.dart';
import '../models/order_form/order_draft_response_model.dart';
import '../models/order_form/order_request_payload_model.dart';
import '../models/order_form/order_request_response_model.dart';

/// 주문서 작성 화면 API 데이터소스 (Spec #598 P1-M).
///
/// 의존 백엔드 스펙:
///  - #594 `GET /api/v1/mobile/clients/{external_key}/loan-inquiry`
///  - #592 `POST /api/v1/mobile/order-requests`
///  - #596 `GET / POST / DELETE /api/v1/mobile/orders/draft`
///
/// 모든 응답은 글로벌 ApiResponse 계약 (`{ data, message, ... }`) 을 따른다.
/// `data` 필드를 언래핑하여 모델로 매핑한다.
class OrderFormApiDataSource {
  final Dio _dio;

  OrderFormApiDataSource(this._dio);

  /// #594 — 거래처 여신 한도 조회.
  Future<LoanInquiryResponseModel> getLoanInquiry({
    required String externalKey,
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/clients/$externalKey/loan-inquiry',
    );
    return LoanInquiryResponseModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  /// #596 — 임시저장 조회. 없으면 `null`.
  Future<OrderDraftResponseModel?> getOrderDraft() async {
    final response = await _dio.get('/api/v1/mobile/orders/draft');
    final data = response.data['data'];
    if (data == null) {
      return null;
    }
    return OrderDraftResponseModel.fromJson(data as Map<String, dynamic>);
  }

  /// #596 — 임시저장 등록 / 갱신.
  Future<OrderDraftSavedModel> saveOrderDraft(
    OrderDraftRequestModel request,
  ) async {
    final response = await _dio.post(
      '/api/v1/mobile/orders/draft',
      data: request.toJson(),
    );
    return OrderDraftSavedModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  /// #596 — 임시저장 삭제. 없어도 204 (멱등).
  Future<void> deleteOrderDraft() async {
    await _dio.delete('/api/v1/mobile/orders/draft');
  }

  /// #592 — 주문 등록. `clientRequestId` 는 헤더 `Idempotency-Key` 로 전송 (서버 멱등).
  Future<OrderRequestResponseModel> submitOrderRequest(
    OrderRequestPayloadModel payload,
  ) async {
    final headers = <String, dynamic>{};
    if (payload.clientRequestId != null) {
      headers['Idempotency-Key'] = payload.clientRequestId;
    }

    final response = await _dio.post(
      '/api/v1/mobile/order-requests',
      data: payload.toJson(),
      options: Options(headers: headers.isEmpty ? null : headers),
    );
    return OrderRequestResponseModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
