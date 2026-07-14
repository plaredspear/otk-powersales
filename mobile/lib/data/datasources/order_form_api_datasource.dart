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
      options: Options(
        headers: headers.isEmpty ? null : headers,
        // 주문 등록은 백엔드에서 SAP 동기 호출을 순차 수행한다 — 재고조회(SD03070,
        // productCode 50개당 1회) + 여신조회(SD03040, 1회). 각 호출의 백엔드
        // read-timeout 이 30초라, 제품 100개(재고 chunk 2회 + 여신 1회 = 3회)
        // 최악의 경우 백엔드 SAP 처리에만 90초가 걸린다. 여기에 백엔드 DB/검증 +
        // 모바일↔백엔드 네트워크 왕복 여유를 더해 100초로 잡는다. 전역 Dio
        // receiveTimeout(35초)을 그대로 쓰면 서버가 정상 처리 중인데도 클라이언트가
        // 먼저 끊는 false timeout 이 발생하므로, 이 요청에만 per-request 상향한다.
        receiveTimeout: const Duration(seconds: 100),
      ),
    );
    return OrderRequestResponseModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
