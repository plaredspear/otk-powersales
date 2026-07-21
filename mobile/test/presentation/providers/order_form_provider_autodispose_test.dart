import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/data/models/order_form/order_draft_response_model.dart';
import 'package:mobile/domain/usecases/order_form/delete_order_draft.dart';
import 'package:mobile/domain/usecases/order_form/get_loan_inquiry.dart';
import 'package:mobile/domain/usecases/order_form/get_order_draft.dart';
import 'package:mobile/domain/usecases/order_form/save_order_draft.dart';
import 'package:mobile/domain/usecases/order_form/submit_order_request.dart';
import 'package:mobile/domain/usecases/search_products_for_order_usecase.dart';
import 'package:mobile/presentation/providers/add_product_provider.dart';
import 'package:mobile/presentation/providers/order_form_provider.dart';

import '../../helpers/fake_order_form_repository.dart';
import '../../helpers/fake_order_request_repository.dart';

/// 재진입 시 임시저장 팝업 누락 개선 회귀 테스트.
///
/// 화면 진입 트리거를 build 리스너(값 전이 감지)에서 initState 명령형 호출로 바꾸면서,
/// orderFormProvider 를 autoDispose 로 전환했다. 이 테스트는 그 핵심 계약
/// — "구독이 끊기면 provider 가 폐기되어 재진입 시 항상 초기 상태로 시작한다" — 를
/// 검증한다. 이 계약이 성립해야 매 진입마다 initialize() 가 hasDraft 를 다시 세팅하고
/// 명령형 팝업 트리거가 동작한다.
void main() {
  late FakeOrderFormRepository formRepo;

  ProviderContainer makeContainer() {
    return ProviderContainer(
      overrides: [
        getLoanInquiryUseCaseProvider
            .overrideWithValue(GetLoanInquiry(formRepo)),
        getOrderDraftUseCaseProvider.overrideWithValue(GetOrderDraft(formRepo)),
        saveOrderDraftUseCaseProvider
            .overrideWithValue(SaveOrderDraft(formRepo)),
        deleteOrderDraftUseCaseProvider
            .overrideWithValue(DeleteOrderDraft(formRepo)),
        submitOrderRequestUseCaseProvider
            .overrideWithValue(SubmitOrderRequest(formRepo)),
        searchProductsForOrderUseCaseProvider.overrideWithValue(
          SearchProductsForOrder(FakeOrderRequestRepository()),
        ),
      ],
    );
  }

  setUp(() {
    formRepo = FakeOrderFormRepository();
  });

  test('구독이 끊기면 orderFormProvider 상태가 폐기된다 (autoDispose)', () async {
    final container = makeContainer();
    addTearDown(container.dispose);

    // 임시저장이 존재하는 상태로 진입.
    formRepo.orderDraftToReturn = _draftResponse();

    // 첫 진입: 구독 시작 + 초기화 → hasDraft = true.
    final sub1 = container.listen(orderFormProvider, (_, __) {});
    await container.read(orderFormProvider.notifier).initialize();
    expect(container.read(orderFormProvider).hasDraft, isTrue,
        reason: '임시저장이 있으면 첫 진입 시 hasDraft = true');

    // 화면 이탈: 구독 해제 → autoDispose 로 provider 폐기.
    sub1.close();
    // autoDispose 폐기는 microtask 로 처리되므로 한 틱 흘려보낸다.
    await Future<void>.delayed(Duration.zero);

    // 재진입: 새 구독 → 폐기되었으므로 초기 상태(hasDraft = false)로 재생성.
    final sub2 = container.listen(orderFormProvider, (_, __) {});
    addTearDown(sub2.close);
    expect(container.read(orderFormProvider).hasDraft, isFalse,
        reason: 'autoDispose 로 폐기된 뒤 재진입하면 초기 상태여야 한다');
  });

  test('재진입 시 initialize() 가 draft 를 다시 조회해 hasDraft 를 복원한다', () async {
    final container = makeContainer();
    addTearDown(container.dispose);
    formRepo.orderDraftToReturn = _draftResponse();

    // 첫 진입.
    final sub1 = container.listen(orderFormProvider, (_, __) {});
    await container.read(orderFormProvider.notifier).initialize();
    expect(formRepo.getOrderDraftCount, 1);
    expect(container.read(orderFormProvider).hasDraft, isTrue);

    // 이탈.
    sub1.close();
    await Future<void>.delayed(Duration.zero);

    // 재진입 + 재초기화 → draft 재조회 + hasDraft 재세팅.
    final sub2 = container.listen(orderFormProvider, (_, __) {});
    addTearDown(sub2.close);
    await container.read(orderFormProvider.notifier).initialize();
    expect(formRepo.getOrderDraftCount, 2,
        reason: '재진입마다 draft 를 다시 조회해야 팝업 트리거가 매번 성립한다');
    expect(container.read(orderFormProvider).hasDraft, isTrue,
        reason: '재진입 시에도 hasDraft 가 다시 true 가 되어 팝업이 뜬다');
  });
}

OrderDraftResponseModel _draftResponse() => const OrderDraftResponseModel(
      draftId: 99,
      accountId: 5678,
      accountName: '거래처A',
      accountExternalKey: 'EK001',
      deliveryDate: '2026-05-08',
      totalAmount: 1234,
      savedAt: '2026-05-04T10:00:00Z',
      lines: [
        OrderDraftLineModel(
          lineNumber: 10,
          productCode: 'P001',
          productName: '제품A',
          unit: 'BOX',
          quantity: 10,
          quantityPieces: 100,
          quantityBoxes: 10,
          boxSize: 12,
          unitPrice: 12345,
          amount: 1234,
        ),
      ],
    );
