import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/order_form/order_draft_request_model.dart';
import 'package:mobile/data/models/order_form/order_request_payload_model.dart';
import 'package:mobile/domain/usecases/order_form/delete_order_draft.dart';
import 'package:mobile/domain/usecases/order_form/get_loan_inquiry.dart';
import 'package:mobile/domain/usecases/order_form/get_order_draft.dart';
import 'package:mobile/domain/usecases/order_form/save_order_draft.dart';
import 'package:mobile/domain/usecases/order_form/submit_order_request.dart';

import '../../../helpers/fake_order_form_repository.dart';

void main() {
  late FakeOrderFormRepository fake;

  setUp(() {
    fake = FakeOrderFormRepository();
  });

  group('GetLoanInquiry UseCase (#594)', () {
    test('externalKey 전달 시 Repository 호출 후 응답 패스스루', () async {
      final useCase = GetLoanInquiry(fake);

      final result = await useCase(externalKey: 'EK001');

      expect(fake.lastExternalKey, 'EK001');
      expect(result.creditBalance, 2500000);
      expect(result.currency, 'KRW');
    });

    test('빈 externalKey 는 ArgumentError', () async {
      final useCase = GetLoanInquiry(fake);
      expect(() => useCase(externalKey: ''), throwsA(isA<ArgumentError>()));
    });
  });

  group('GetOrderDraft UseCase (#596 GET)', () {
    test('임시저장 없을 때 null 반환 (예외 아님)', () async {
      fake.orderDraftToReturn = null;
      final useCase = GetOrderDraft(fake);

      final result = await useCase();

      expect(result, isNull);
      expect(fake.getOrderDraftCount, 1);
    });
  });

  group('SaveOrderDraft UseCase (#596 POST)', () {
    test('Repository 호출 + 응답 패스스루', () async {
      final useCase = SaveOrderDraft(fake);

      final request = OrderDraftRequestModel(
        accountId: 5678,
        deliveryDate: '2026-05-08',
        totalAmount: 1234567,
        lines: const [
          OrderDraftRequestLineModel(
            lineNumber: 10,
            productCode: 'P001',
            unit: 'BOX',
            quantity: 10,
          ),
        ],
      );
      final result = await useCase(request: request);

      expect(fake.lastSavedDraftRequest, equals(request));
      expect(result.draftId, 99);
    });
  });

  group('DeleteOrderDraft UseCase (#596 DELETE)', () {
    test('Repository 호출 검증', () async {
      final useCase = DeleteOrderDraft(fake);

      await useCase();

      expect(fake.deleteOrderDraftCount, 1);
    });
  });

  group('SubmitOrderRequest UseCase (#592)', () {
    test('payload 패스스루 + clientRequestId 헤더 송신 (DataSource 책임 검증은 별도)', () async {
      final useCase = SubmitOrderRequest(fake);

      final payload = OrderRequestPayloadModel(
        clientRequestId: '11111111-1111-4111-8111-111111111111',
        accountId: 5678,
        deliveryDate: '2026-05-08',
        totalAmount: 1234567,
        lines: const [
          OrderRequestLineModel(
            lineNumber: 10,
            productCode: 'P001',
            quantity: 10,
            unit: 'BOX',
            quantityPieces: 100,
            quantityBoxes: 10,
          ),
        ],
      );

      final result = await useCase(payload: payload);

      expect(fake.lastSubmittedPayload, equals(payload));
      expect(result.status, 'SENT');
    });
  });
}
