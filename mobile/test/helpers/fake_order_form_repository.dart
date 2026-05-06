import 'package:mobile/data/models/order_form/loan_inquiry_response_model.dart';
import 'package:mobile/data/models/order_form/order_draft_request_model.dart';
import 'package:mobile/data/models/order_form/order_draft_response_model.dart';
import 'package:mobile/data/models/order_form/order_request_payload_model.dart';
import 'package:mobile/data/models/order_form/order_request_response_model.dart';
import 'package:mobile/domain/repositories/order_form_repository.dart';

/// Spec #598 P1-M — `OrderFormRepository` 의 수동 Fake.
///
/// `mocktail`/`mockito` 미사용 (mobile-conventions.md 정합 — Fake 패턴).
class FakeOrderFormRepository implements OrderFormRepository {
  // ─── 응답 stub ───
  LoanInquiryResponseModel? loanInquiryToReturn;
  OrderDraftResponseModel? orderDraftToReturn;
  OrderDraftSavedModel? orderDraftSavedToReturn;
  OrderRequestResponseModel? orderRequestResponseToReturn;
  Object? exceptionToThrow;

  // ─── 호출 캡처 ───
  String? lastExternalKey;
  OrderDraftRequestModel? lastSavedDraftRequest;
  OrderRequestPayloadModel? lastSubmittedPayload;
  int getOrderDraftCount = 0;
  int deleteOrderDraftCount = 0;
  int submitOrderRequestCount = 0;

  @override
  Future<LoanInquiryResponseModel> getLoanInquiry({
    required String externalKey,
  }) async {
    lastExternalKey = externalKey;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return loanInquiryToReturn ??
        const LoanInquiryResponseModel(
          externalKey: 'EK001',
          totalCredit: 10000000,
          creditBalance: 2500000,
          currency: 'KRW',
          dataAsOf: '2026-05-04T03:00:00+09:00',
        );
  }

  @override
  Future<OrderDraftResponseModel?> getOrderDraft() async {
    getOrderDraftCount += 1;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return orderDraftToReturn;
  }

  @override
  Future<OrderDraftSavedModel> saveOrderDraft(
      OrderDraftRequestModel request) async {
    lastSavedDraftRequest = request;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return orderDraftSavedToReturn ??
        const OrderDraftSavedModel(draftId: 99, savedAt: '2026-05-04T10:00:00Z');
  }

  @override
  Future<void> deleteOrderDraft() async {
    deleteOrderDraftCount += 1;
    if (exceptionToThrow != null) throw exceptionToThrow!;
  }

  @override
  Future<OrderRequestResponseModel> submitOrderRequest(
    OrderRequestPayloadModel payload,
  ) async {
    submitOrderRequestCount += 1;
    lastSubmittedPayload = payload;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return orderRequestResponseToReturn ??
        const OrderRequestResponseModel(
          orderRequestId: 12345,
          orderRequestNumber: 'ORD-20260504-12345',
          status: 'SENT',
          totalAmount: 1234567,
        );
  }
}
