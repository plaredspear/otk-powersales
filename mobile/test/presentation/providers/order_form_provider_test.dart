import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/order_form/loan_inquiry_response_model.dart';
import 'package:mobile/data/models/order_form/order_draft_response_model.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order_request.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/repositories/order_request_repository.dart';
import 'package:mobile/domain/usecases/order_form/delete_order_draft.dart';
import 'package:mobile/domain/usecases/order_form/get_loan_inquiry.dart';
import 'package:mobile/domain/usecases/order_form/get_order_draft.dart';
import 'package:mobile/domain/usecases/order_form/save_order_draft.dart';
import 'package:mobile/domain/usecases/submit_order_usecase.dart';
import 'package:mobile/domain/usecases/update_order_usecase.dart';
import 'package:mobile/domain/usecases/validate_order_usecase.dart';
import 'package:mobile/presentation/providers/order_form_provider.dart';
import 'package:uuid/data.dart';
import 'package:uuid/uuid.dart';

import '../../helpers/fake_order_form_repository.dart';

/// 고정 UUID 발급 (테스트 결정성).
class _FixedUuid extends Uuid {
  final String value;
  _FixedUuid(this.value);

  @override
  String v4({Map<String, dynamic>? options, V4Options? config}) => value;
}

void main() {
  group('OrderFormNotifier (Spec #598 P2-M)', () {
    late FakeOrderRequestRepository legacyRepo;
    late FakeOrderFormRepository formRepo;
    late OrderFormNotifier notifier;

    OrderFormNotifier createNotifier() {
      return OrderFormNotifier(
        getLoanInquiry: GetLoanInquiry(formRepo),
        getOrderDraft: GetOrderDraft(formRepo),
        saveOrderDraft: SaveOrderDraft(formRepo),
        deleteOrderDraft: DeleteOrderDraft(formRepo),
        validateOrder: ValidateOrder(legacyRepo),
        submitOrder: SubmitOrder(legacyRepo),
        updateOrder: UpdateOrder(legacyRepo),
        uuid: _FixedUuid('11111111-1111-4111-8111-111111111111'),
      );
    }

    setUp(() {
      legacyRepo = FakeOrderRequestRepository();
      formRepo = FakeOrderFormRepository();
      notifier = createNotifier();
    });

    group('initialize (#598 P2-M §2.1)', () {
      test('H1 — 임시저장 없음 → hasDraft = false + clientRequestId 발급', () async {
        formRepo.orderDraftToReturn = null;

        await notifier.initialize();

        expect(notifier.state.hasDraft, false);
        expect(notifier.state.pendingDraft, isNull);
        expect(
          notifier.state.clientRequestId,
          '11111111-1111-4111-8111-111111111111',
        );
        expect(formRepo.getOrderDraftCount, 1);
      });

      test('H2 — 임시저장 있음 → hasDraft = true + pendingDraft 보관', () async {
        formRepo.orderDraftToReturn = _draftResponse();

        await notifier.initialize();

        expect(notifier.state.hasDraft, true);
        expect(notifier.state.pendingDraft, isNotNull);
        expect(notifier.state.pendingDraft!.draftId, 99);
      });

      test('수정 모드(orderId)에서는 clientRequestId 미발급', () async {
        await notifier.initialize(orderId: 42);

        expect(notifier.state.clientRequestId, isNull);
      });

      test('GET /draft 실패 시 SnackBar 메시지', () async {
        formRepo.exceptionToThrow = Exception('SAP timeout');

        await notifier.initialize();

        expect(notifier.state.hasDraft, false);
        expect(
          notifier.state.errorMessage,
          '임시저장 조회 중 오류가 발생했습니다.',
        );
      });
    });

    group('acceptDraft (#598 P2-M §2.2)', () {
      test('H2 — 폼 채움 + 여신 호출', () async {
        formRepo.orderDraftToReturn = _draftResponse();
        await notifier.initialize();

        await notifier.acceptDraft();

        expect(notifier.state.hasDraft, false);
        expect(notifier.state.pendingDraft, isNull);
        expect(notifier.state.draftId, 99);
        expect(notifier.state.selectedAccountId, 5678);
        expect(notifier.state.selectedExternalKey, 'EK001');
        expect(notifier.state.orderDraft.items, hasLength(1));
        expect(notifier.state.orderDraft.items[0].productCode, 'P001');
        // 여신 호출 검증
        expect(formRepo.lastExternalKey, 'EK001');
        expect(notifier.state.creditBalance, 2500000);
      });

      test('pendingDraft 없으면 no-op', () async {
        await notifier.acceptDraft();
        expect(notifier.state.orderDraft.items, isEmpty);
      });
    });

    group('declineDraft (#598 P2-M §2.2)', () {
      test('H3 — DELETE /draft 호출 + 빈 폼', () async {
        formRepo.orderDraftToReturn = _draftResponse();
        await notifier.initialize();

        await notifier.declineDraft();

        expect(notifier.state.hasDraft, false);
        expect(notifier.state.pendingDraft, isNull);
        expect(formRepo.deleteOrderDraftCount, 1);
      });
    });

    group('selectClient (#598 P2-M §2.3)', () {
      test('H4 — 거래처 선택 → 여신 호출 → creditBalance 표시', () async {
        formRepo.loanInquiryToReturn = const LoanInquiryResponseModel(
          externalKey: 'EK001',
          totalCredit: 5000000,
          creditBalance: 1500000,
          currency: 'KRW',
          dataAsOf: '2026-05-04T03:00:00+09:00',
        );

        notifier.selectClient(5678, '거래처A', 'EK001');
        await Future<void>.delayed(Duration.zero);

        expect(notifier.state.orderDraft.clientId, 5678);
        expect(notifier.state.orderDraft.clientName, '거래처A');
        expect(notifier.state.selectedAccountId, 5678);
        expect(notifier.state.selectedExternalKey, 'EK001');
        expect(notifier.state.creditBalance, 1500000);
        expect(formRepo.lastExternalKey, 'EK001');
      });

      test('E2 — 여신 조회 5xx → 한국어 SnackBar', () async {
        formRepo.exceptionToThrow = Exception('SAP error');

        notifier.selectClient(5678, '거래처A', 'EK001');
        await Future<void>.delayed(Duration.zero);

        expect(notifier.state.creditBalance, isNull);
        expect(notifier.state.errorMessage, contains('여신 조회 중 오류'));
      });

      test('externalKey 없으면 여신 호출 안 함', () async {
        notifier.selectClient(5678, '거래처A', '');
        await Future<void>.delayed(Duration.zero);

        expect(formRepo.lastExternalKey, isNull);
      });
    });

    group('saveDraft (#598 P2-M §2.4)', () {
      test('H5 — 거래처 + 라인 있을 때 POST /draft → 성공 메시지', () async {
        formRepo.orderDraftSavedToReturn = const OrderDraftSavedModel(
          draftId: 88,
          savedAt: '2026-05-04T10:00:00Z',
        );
        notifier.state = notifier.state.copyWith(
          selectedAccountId: 5678,
          selectedExternalKey: 'EK001',
          orderDraft: notifier.state.orderDraft.copyWith(
            clientId: 5678,
            items: [_item('P001', total: 1234)],
            totalAmount: 1234,
          ),
        );

        await notifier.saveDraft();

        expect(notifier.state.successMessage, '임시저장이 완료되었습니다.');
        expect(notifier.state.draftId, 88);
        expect(formRepo.lastSavedDraftRequest, isNotNull);
        expect(formRepo.lastSavedDraftRequest!.accountId, 5678);
        expect(formRepo.lastSavedDraftRequest!.lines, hasLength(1));
      });

      test('거래처 미선택 시 거부', () async {
        await notifier.saveDraft();
        expect(notifier.state.errorMessage, '거래처를 선택해주세요.');
      });

      test('E5 — 403 ORD_DRAFT_ACCOUNT_FORBIDDEN → 한국어 SnackBar', () async {
        formRepo.exceptionToThrow = Exception('ORD_DRAFT_ACCOUNT_FORBIDDEN');
        notifier.state = notifier.state.copyWith(
          selectedAccountId: 5678,
          orderDraft: notifier.state.orderDraft.copyWith(
            clientId: 5678,
            items: [_item('P001')],
          ),
        );

        await notifier.saveDraft();

        expect(notifier.state.errorMessage, '본인 담당 거래처가 아닙니다.');
      });
    });

    group('deleteDraft (#598 P2-M §2.5)', () {
      test('H6 — DELETE /draft → 빈 폼 + 새 clientRequestId + 성공 메시지', () async {
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(
            items: [_item('P001')],
          ),
          hasDraft: true,
        );

        await notifier.deleteDraft();

        expect(notifier.state.orderDraft.items, isEmpty);
        expect(notifier.state.successMessage, '삭제되었습니다.');
        expect(formRepo.deleteOrderDraftCount, 1);
        expect(notifier.state.clientRequestId, isNotNull);
      });
    });

    group('discardForm (#598 P2-M §2.6)', () {
      test('초기 상태로 리셋', () {
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(
            items: [_item('P001')],
          ),
          hasDraft: true,
          selectedAccountId: 1,
        );

        notifier.discardForm();

        expect(notifier.state.orderDraft.items, isEmpty);
        expect(notifier.state.selectedAccountId, isNull);
      });
    });

    // ─── 변경되지 않은 메서드 회귀 테스트 ─────────────────────────

    group('addProductToOrder', () {
      test('제품 추가 + 중복 차단', () {
        final item = _item('P001');
        notifier.addProductToOrder(item);
        notifier.addProductToOrder(item);

        expect(notifier.state.orderDraft.items, hasLength(1));
      });
    });

    group('validateAndSubmitOrder (mock 유지)', () {
      test('유효성 통과 시 submitResult 갱신', () async {
        legacyRepo.validationResultToReturn =
            const ValidationResult(isValid: true);
        legacyRepo.submitResultToReturn = const OrderSubmitResult(
          orderId: 1,
          orderRequestNumber: 'OP-1',
          status: 'PENDING',
        );
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(
            clientId: 100,
            deliveryDate: DateTime(2026, 5, 8),
            items: [_item('P001')],
          ),
        );

        await notifier.validateAndSubmitOrder();

        expect(notifier.state.submitResult, isNotNull);
      });
    });
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
          unitPrice: 12345,
          amount: 1234,
        ),
      ],
    );

OrderDraftItem _item(String productCode, {int total = 1234}) {
  return OrderDraftItem(
    productCode: productCode,
    productName: 'P-$productCode',
    quantityBoxes: 1,
    quantityPieces: 0,
    unitPrice: 1234,
    boxSize: 1,
    totalPrice: total,
  );
}

// --- Fake Repository (kept from previous version, used for validate/submit/update only) ---

class FakeOrderRequestRepository implements OrderRequestRepository {
  ValidationResult validationResultToReturn = const ValidationResult(isValid: true);
  OrderSubmitResult submitResultToReturn = const OrderSubmitResult(
    orderId: 1,
    orderRequestNumber: 'OP00000001',
    status: 'PENDING',
  );
  bool shouldThrow = false;
  bool shouldThrowOnSubmit = false;
  String errorMessage = '테스트 에러';

  bool submitOrderCalled = false;
  bool updateOrderCalled = false;
  int? lastUpdatedOrderId;

  @override
  Future<int> getCreditBalance({required int clientId}) async => throw UnimplementedError();

  @override
  Future<OrderDraft?> loadDraftOrder() async => throw UnimplementedError();

  @override
  Future<void> saveDraftOrder({required OrderDraft orderDraft}) async => throw UnimplementedError();

  @override
  Future<void> deleteDraftOrder() async {
    // SubmitOrder 유스케이스가 호출하므로 no-op (테스트 외 흐름).
  }

  @override
  Future<ValidationResult> validateOrder({required OrderDraft orderDraft}) async {
    if (shouldThrow) throw Exception(errorMessage);
    return validationResultToReturn;
  }

  @override
  Future<OrderSubmitResult> submitOrder({required OrderDraft orderDraft}) async {
    if (shouldThrowOnSubmit) throw Exception(errorMessage);
    submitOrderCalled = true;
    return submitResultToReturn;
  }

  @override
  Future<OrderSubmitResult> updateOrder({
    required int orderId,
    required OrderDraft orderDraft,
  }) async {
    if (shouldThrow) throw Exception(errorMessage);
    updateOrderCalled = true;
    lastUpdatedOrderId = orderId;
    return submitResultToReturn;
  }

  // unused stubs
  @override
  Future<OrderRequestListResult> getMyOrderRequests({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
  }) async =>
      throw UnimplementedError();

  @override
  Future<OrderDetail> getOrderRequestDetail({required int orderId}) async =>
      throw UnimplementedError();

  @override
  Future<void> resendOrderRequest({required int orderId}) async =>
      throw UnimplementedError();

  @override
  Future<OrderCancelResult> cancelOrderRequest({
    required int orderId,
    required List<String> productCodes,
  }) async =>
      throw UnimplementedError();

  @override
  Future<List<ProductForOrder>> getFavoriteProducts() async =>
      throw UnimplementedError();

  @override
  Future<List<ProductForOrder>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) async =>
      throw UnimplementedError();

  @override
  Future<ProductForOrder> getProductByBarcode({required String barcode}) async =>
      throw UnimplementedError();

  @override
  Future<void> addToFavorites({required String productCode}) async =>
      throw UnimplementedError();

  @override
  Future<void> removeFromFavorites({required String productCode}) async =>
      throw UnimplementedError();

  @override
  Future<ClientOrderListResult> getClientOrders({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  }) async =>
      throw UnimplementedError();

  @override
  Future<ClientOrderDetail> getClientOrderDetail({
    required String sapOrderNumber,
  }) async =>
      throw UnimplementedError();
}
