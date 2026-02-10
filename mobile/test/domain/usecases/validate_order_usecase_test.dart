import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/order_mock_repository.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/usecases/validate_order_usecase.dart';

void main() {
  late OrderMockRepository repository;
  late ValidateOrder validateOrder;

  setUp(() {
    repository = OrderMockRepository();
    validateOrder = ValidateOrder(repository);
  });

  group('ValidateOrder UseCase', () {
    test('거래처 미선택 시 유효성 실패', () async {
      // Given: 거래처가 선택되지 않은 주문서
      final draft = OrderDraft(
        clientId: null, // 거래처 미선택
        clientName: null,
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

      // When: 유효성 검증 실행
      final result = await validateOrder(orderDraft: draft);

      // Then: 거래처 선택 에러 반환
      expect(result.isValid, false);
      expect(result.errors.containsKey('_form'), true);
      expect(result.errors['_form']?.message, '거래처를 선택해주세요');
    });

    test('납기일 미선택 시 유효성 실패', () async {
      // Given: 납기일이 선택되지 않은 주문서
      final draft = OrderDraft(
        clientId: 1,
        clientName: '천사푸드',
        deliveryDate: null, // 납기일 미선택
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

      // When: 유효성 검증 실행
      final result = await validateOrder(orderDraft: draft);

      // Then: 납기일 선택 에러 반환
      expect(result.isValid, false);
      expect(result.errors.containsKey('_form'), true);
      expect(result.errors['_form']?.message, '납기일을 선택해주세요');
    });

    test('제품 미추가 시 유효성 실패', () async {
      // Given: 제품이 추가되지 않은 주문서
      final draft = OrderDraft(
        clientId: 1,
        clientName: '천사푸드',
        deliveryDate: DateTime(2026, 2, 15),
        items: const [], // 제품 미추가
        totalAmount: 0,
        isDraft: false,
        lastModified: DateTime(2026, 2, 10),
      );

      // When: 유효성 검증 실행
      final result = await validateOrder(orderDraft: draft);

      // Then: 제품 추가 에러 반환
      expect(result.isValid, false);
      expect(result.errors.containsKey('_form'), true);
      expect(result.errors['_form']?.message, '제품을 최소 1개 이상 추가해주세요');
    });

    test('수량 미입력 제품 유효성 실패', () async {
      // Given: 수량이 입력되지 않은 제품이 포함된 주문서
      final draft = OrderDraft(
        clientId: 1,
        clientName: '천사푸드',
        deliveryDate: DateTime(2026, 2, 15),
        items: const [
          OrderDraftItem(
            productCode: '01101123',
            productName: '갈릭 아이올리소스 240g',
            quantityBoxes: 0, // 박스 수량 0
            quantityPieces: 0, // 낱개 수량 0
            unitPrice: 3500,
            boxSize: 20,
            totalPrice: 0,
          ),
        ],
        totalAmount: 0,
        isDraft: false,
        lastModified: DateTime(2026, 2, 10),
      );

      // When: 유효성 검증 실행
      final result = await validateOrder(orderDraft: draft);

      // Then: 수량 입력 에러 반환
      expect(result.isValid, false);
      expect(result.errors.containsKey('01101123'), true);
      expect(result.errors['01101123']?.message,
          '갈릭 아이올리소스 240g의 수량을 입력해주세요');
    });

    test('모든 필수 입력 완료 + 서버 유효성 통과', () async {
      // Given: 모든 필수 입력이 완료된 주문서
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

      // When: 유효성 검증 실행
      final result = await validateOrder(orderDraft: draft);

      // Then: 유효성 통과
      expect(result.isValid, true);
      expect(result.errors.isEmpty, true);
    });

    test('모든 필수 입력 완료 + 서버 유효성 실패 (제품코드 23010011 박스 < 5)', () async {
      // Given: 제품코드 '23010011'의 박스 수량이 5 미만인 주문서
      final draft = OrderDraft(
        clientId: 1,
        clientName: '천사푸드',
        deliveryDate: DateTime(2026, 2, 15),
        items: const [
          OrderDraftItem(
            productCode: '23010011',
            productName: '오감포차_크림새우180G',
            quantityBoxes: 3, // 5박스 미만
            quantityPieces: 0,
            unitPrice: 5900,
            boxSize: 16,
            totalPrice: 283200,
          ),
        ],
        totalAmount: 283200,
        isDraft: false,
        lastModified: DateTime(2026, 2, 10),
      );

      // When: 유효성 검증 실행
      final result = await validateOrder(orderDraft: draft);

      // Then: 서버 유효성 실패 (최소 주문 수량 5박스)
      expect(result.isValid, false);
      expect(result.errors.containsKey('23010011'), true);
      expect(result.errors['23010011']?.errorType,
          ValidationErrorType.minOrderQuantity);
      expect(result.errors['23010011']?.message, '최소 주문 수량은 5박스입니다');
      expect(result.errors['23010011']?.minOrderQuantity, 5);
    });
  });
}
