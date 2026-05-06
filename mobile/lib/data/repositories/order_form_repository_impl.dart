import '../../domain/repositories/order_form_repository.dart';
import '../datasources/order_form_api_datasource.dart';
import '../models/order_form/loan_inquiry_response_model.dart';
import '../models/order_form/order_draft_request_model.dart';
import '../models/order_form/order_draft_response_model.dart';
import '../models/order_form/order_request_payload_model.dart';
import '../models/order_form/order_request_response_model.dart';

/// [OrderFormRepository] 의 Dio 기반 구현 (Spec #598 P1-M).
class OrderFormRepositoryImpl implements OrderFormRepository {
  final OrderFormApiDataSource _dataSource;

  OrderFormRepositoryImpl({required OrderFormApiDataSource dataSource})
      : _dataSource = dataSource;

  @override
  Future<LoanInquiryResponseModel> getLoanInquiry({
    required String externalKey,
  }) {
    return _dataSource.getLoanInquiry(externalKey: externalKey);
  }

  @override
  Future<OrderDraftResponseModel?> getOrderDraft() {
    return _dataSource.getOrderDraft();
  }

  @override
  Future<OrderDraftSavedModel> saveOrderDraft(OrderDraftRequestModel request) {
    return _dataSource.saveOrderDraft(request);
  }

  @override
  Future<void> deleteOrderDraft() {
    return _dataSource.deleteOrderDraft();
  }

  @override
  Future<OrderRequestResponseModel> submitOrderRequest(
    OrderRequestPayloadModel payload,
  ) {
    return _dataSource.submitOrderRequest(payload);
  }
}
