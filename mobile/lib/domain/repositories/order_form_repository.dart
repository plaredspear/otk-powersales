import '../../data/models/order_form/loan_inquiry_response_model.dart';
import '../../data/models/order_form/order_draft_request_model.dart';
import '../../data/models/order_form/order_draft_response_model.dart';
import '../../data/models/order_form/order_request_payload_model.dart';
import '../../data/models/order_form/order_request_response_model.dart';

/// 주문서 작성 화면 Repository (Spec #598 P1-M).
///
/// 5개 메서드로 신규 백엔드 (#594 / #596 / #592) 와 통신한다.
/// 화면 흐름은 P2-M (임시저장 + 거래처/여신) / P3-M (제품 + 작성 완료) 책임.
abstract class OrderFormRepository {
  /// 거래처 여신 한도 조회 (#594).
  Future<LoanInquiryResponseModel> getLoanInquiry({required String externalKey});

  /// 임시저장 조회 (#596). 없으면 `null`.
  Future<OrderDraftResponseModel?> getOrderDraft();

  /// 임시저장 등록 / 갱신 (#596).
  Future<OrderDraftSavedModel> saveOrderDraft(OrderDraftRequestModel request);

  /// 임시저장 삭제 (#596). 없어도 204 (멱등).
  Future<void> deleteOrderDraft();

  /// 주문 등록 (#592). `clientRequestId` 는 헤더 `Idempotency-Key` 로 송신.
  Future<OrderRequestResponseModel> submitOrderRequest(
    OrderRequestPayloadModel payload,
  );
}
