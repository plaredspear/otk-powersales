import '../../../data/models/order_form/loan_inquiry_response_model.dart';
import '../../repositories/order_form_repository.dart';

/// 거래처 여신 한도 조회 UseCase (Spec #598 P1-M, #594).
///
/// P2-M 거래처 선택 직후 호출.
class GetLoanInquiry {
  final OrderFormRepository _repository;

  GetLoanInquiry(this._repository);

  Future<LoanInquiryResponseModel> call({required String externalKey}) {
    if (externalKey.isEmpty) {
      throw ArgumentError('externalKey 는 비어 있을 수 없습니다');
    }
    return _repository.getLoanInquiry(externalKey: externalKey);
  }
}
