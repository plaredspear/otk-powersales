import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/order_mock_repository.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/usecases/submit_order_usecase.dart';

void main() {
  late OrderMockRepository repository;
  late SubmitOrder submitOrder;

  setUp(() {
    repository = OrderMockRepository();
    submitOrder = SubmitOrder(repository);
  });

  group('SubmitOrder UseCase', () {
    test('주문서 전송 성공 시 결과 반환', () async {
      // Given: 전송할 주문서 준비
      final draft = OrderDraft(
        clientId: 1,
        clientName: '천사푸드',
        deliveryDate: DateTime(2026, 2, 15),
        items: const [
          OrderDraftItem(
            productCode: '01101123',
            productName: '갈릭 아이올리소스 240g',
            quantityBoxes: 5,
            quantityPieces: 0,
            unitPrice: 3500,
            boxSize: 20,
            totalPrice: 350000,
          ),
        ],
        totalAmount: 350000,
        isDraft: false,
        lastModified: DateTime(2026, 2, 10),
      );

      // When: 주문서 전송
      final result = await submitOrder(orderDraft: draft);

      // Then: 전송 결과 반환
      expect(result, isA<OrderSubmitResult>());
      expect(result.orderId, isNotNull);
      expect(result.orderRequestNumber, isNotEmpty);
      expect(result.orderRequestNumber.startsWith('OP'), true);
      expect(result.status, 'PENDING');
    });

    test('전송 성공 후 임시저장 삭제 확인', () async {
      // Given: 임시저장된 주문서가 있는 상태
      final draft = OrderDraft(
        clientId: 1,
        clientName: '천사푸드',
        deliveryDate: DateTime(2026, 2, 15),
        items: const [
          OrderDraftItem(
            productCode: '01101123',
            productName: '갈릭 아이올리소스 240g',
            quantityBoxes: 5,
            quantityPieces: 0,
            unitPrice: 3500,
            boxSize: 20,
            totalPrice: 350000,
          ),
        ],
        totalAmount: 350000,
        isDraft: true,
        lastModified: DateTime(2026, 2, 10),
      );

      // 먼저 임시저장
      await repository.saveDraftOrder(orderDraft: draft);
      final savedDraft = await repository.loadDraftOrder();
      expect(savedDraft, isNotNull);

      // When: 주문서 전송 (내부적으로 임시저장 삭제)
      await submitOrder(orderDraft: draft);

      // Then: 임시저장된 데이터가 삭제됨
      final afterSubmit = await repository.loadDraftOrder();
      expect(afterSubmit, isNull);
    });
  });
}
