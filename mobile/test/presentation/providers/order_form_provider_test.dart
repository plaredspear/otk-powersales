import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/order_form/loan_inquiry_response_model.dart';
import 'package:mobile/data/models/order_form/order_draft_response_model.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/usecases/order_form/delete_order_draft.dart';
import 'package:mobile/domain/usecases/order_form/get_loan_inquiry.dart';
import 'package:mobile/domain/usecases/order_form/get_order_draft.dart';
import 'package:mobile/domain/usecases/order_form/save_order_draft.dart';
import 'package:mobile/domain/usecases/order_form/submit_order_request.dart';
import 'package:mobile/domain/usecases/search_products_for_order_usecase.dart';
import 'package:mobile/presentation/providers/order_form_provider.dart';
import 'package:uuid/data.dart';
import 'package:uuid/uuid.dart';

import '../../helpers/fake_order_form_repository.dart';
import '../../helpers/fake_order_request_repository.dart';

/// 고정 UUID 발급 (테스트 결정성).
class _FixedUuid extends Uuid {
  final String value;
  _FixedUuid(this.value);

  @override
  String v4({Map<String, dynamic>? options, V4Options? config}) => value;
}

void main() {
  group('OrderFormNotifier (Spec #598 P2-M / P3-M)', () {
    late FakeOrderFormRepository formRepo;
    late OrderFormNotifier notifier;

    OrderFormNotifier createNotifier() {
      return OrderFormNotifier(
        getLoanInquiry: GetLoanInquiry(formRepo),
        getOrderDraft: GetOrderDraft(formRepo),
        saveOrderDraft: SaveOrderDraft(formRepo),
        deleteOrderDraft: DeleteOrderDraft(formRepo),
        submitOrderRequest: SubmitOrderRequest(formRepo),
        searchProductsForOrder:
            SearchProductsForOrder(FakeOrderRequestRepository()),
        uuid: _FixedUuid('11111111-1111-4111-8111-111111111111'),
      );
    }

    setUp(() {
      formRepo = FakeOrderFormRepository();
      notifier = createNotifier();
    });

    group('preloadProductByCode (제품검색 → 주문서 프리필)', () {
      test('제품코드로 검색해 주문 라인에 미리 추가한다', () async {
        await notifier.preloadProductByCode('01101123');

        final items = notifier.state.orderDraft.items;
        expect(items, hasLength(1));
        expect(items.first.productCode, '01101123');
      });

      test('일치하는 제품이 없으면 라인을 추가하지 않는다', () async {
        await notifier.preloadProductByCode('NONEXISTENT_CODE');

        expect(notifier.state.orderDraft.items, isEmpty);
      });
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
        // 레거시 selectTempPrdList 정합: 단가·입수는 마스터 재조회값을 쓰고 소계는 재계산.
        // boxSize=12, unitPrice=12345, 총개수=(10박스×12)+100낱개=220 → 소계=220×12345.
        expect(notifier.state.orderDraft.items[0].boxSize, 12);
        expect(notifier.state.orderDraft.items[0].unitPrice, 12345);
        expect(notifier.state.orderDraft.items[0].totalPrice, 220 * 12345);
        expect(notifier.state.totalAmount, 220 * 12345);
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

    // ─── Spec #598 P3-M: addProductLine 차단 룰 3종 ────────────────────

    group('addProductLine (#598 P3-M §2.1)', () {
      test('E9 — 전용상품 차단', () {
        final result = notifier.addProductLine(_product('P001', exclusive: true));
        expect(result, false);
        expect(notifier.state.errorMessage, '전용상품은 주문이 불가능합니다.');
        expect(notifier.state.orderDraft.items, isEmpty);
      });

      test('E9 예외 — 전용상품이라도 20010042(누룽지)는 추가 허용 (레거시 하드코딩 정합)', () {
        final result =
            notifier.addProductLine(_product('20010042', exclusive: true));
        expect(result, true);
        expect(notifier.state.errorMessage, isNull);
        expect(
          notifier.state.orderDraft.items.single.productCode,
          '20010042',
        );
      });

      test('E10 — 시식·증정 차단', () {
        final result = notifier.addProductLine(_product('P002', tasting: true));
        expect(result, false);
        expect(notifier.state.errorMessage, '시식/증정용 상품은 추가할 수 없습니다.');
      });

      test('E11 — 중복 차단', () {
        notifier.addProductLine(_product('P003'));
        final result = notifier.addProductLine(_product('P003'));
        expect(result, false);
        expect(notifier.state.errorMessage, '이미 추가된 제품입니다.');
        expect(notifier.state.orderDraft.items, hasLength(1));
      });

      test('통과 — 라인 추가', () {
        final result = notifier.addProductLine(_product('P004'));
        expect(result, true);
        expect(notifier.state.orderDraft.items, hasLength(1));
        expect(notifier.state.orderDraft.items[0].productCode, 'P004');
      });
    });

    // ─── Spec #598 P3-M: validateAndSubmitOrder 검증 (A)~(I) ───────────

    group('validateAndSubmitOrder (#598 P3-M §2.6)', () {
      void seedValidState() {
        notifier.state = notifier.state.copyWith(
          selectedAccountId: 5678,
          selectedExternalKey: 'EK001',
          orderDraft: notifier.state.orderDraft.copyWith(
            clientId: 5678,
            deliveryDate: DateTime.now().add(const Duration(days: 3)),
            items: [_item('P001', boxes: 5, pieces: 100)],
            totalAmount: 1000,
          ),
        );
      }

      test('E1 (A) — 거래처 미선택 → SnackBar', () async {
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(
            deliveryDate: DateTime.now().add(const Duration(days: 3)),
            items: [_item('P001', boxes: 1, pieces: 20)],
          ),
        );
        await notifier.validateAndSubmitOrder();
        expect(notifier.state.errorMessage, '거래처를 선택해 주세요');
        expect(formRepo.submitOrderRequestCount, 0);
      });

      test('E2 (B) — 납기일 미선택 → SnackBar', () async {
        notifier.state = notifier.state.copyWith(
          selectedAccountId: 5678,
          orderDraft: notifier.state.orderDraft.copyWith(
            items: [_item('P001', boxes: 1, pieces: 20)],
          ),
        );
        await notifier.validateAndSubmitOrder();
        expect(notifier.state.errorMessage, '납기일을 선택해 주세요');
      });

      test('E3 (E) — 라인 0개 → SnackBar', () async {
        notifier.state = notifier.state.copyWith(
          selectedAccountId: 5678,
          orderDraft: notifier.state.orderDraft.copyWith(
            deliveryDate: DateTime.now().add(const Duration(days: 3)),
          ),
        );
        await notifier.validateAndSubmitOrder();
        expect(notifier.state.errorMessage, '주문할 제품을 추가해주세요');
      });

      test('E4 (F) — 라인 101개 → SnackBar', () async {
        notifier.state = notifier.state.copyWith(
          selectedAccountId: 5678,
          orderDraft: notifier.state.orderDraft.copyWith(
            deliveryDate: DateTime.now().add(const Duration(days: 3)),
            items: List.generate(101, (i) => _item('P${i.toString().padLeft(3, '0')}', boxes: 1, pieces: 20)),
          ),
        );
        await notifier.validateAndSubmitOrder();
        expect(notifier.state.errorMessage, '제품은 100개 이하로 추가해주세요');
      });

      test('E6 (G) — 여신 호출 중 (creditBalance null + externalKey 있음)', () async {
        seedValidState();
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(creditBalance: null),
        );
        await notifier.validateAndSubmitOrder();
        expect(notifier.state.errorMessage, '여신 조회 중입니다. 잠시 후 다시 시도해주세요');
      });

      test('E7 (H) — 라인 총EA = 0 → SnackBar', () async {
        notifier.state = notifier.state.copyWith(
          selectedAccountId: 5678,
          selectedExternalKey: 'EK001',
          orderDraft: notifier.state.orderDraft.copyWith(
            clientId: 5678,
            deliveryDate: DateTime.now().add(const Duration(days: 3)),
            creditBalance: 1000000,
            items: [_item('P001', boxes: 0, pieces: 0)],
          ),
        );
        await notifier.validateAndSubmitOrder();
        expect(notifier.state.errorMessage, '수량이 0인 라인이 있습니다.');
      });

      test('E7-b (H) — 박스만 입력(낱개 0)이라도 총EA > 0 이면 통과', () async {
        // 레거시 write.jsp:219 tot-quantity-each(총EA) 기준 — 박스 2 × 입수 20 = 40 > 0.
        notifier.state = notifier.state.copyWith(
          selectedAccountId: 5678,
          selectedExternalKey: 'EK001',
          orderDraft: notifier.state.orderDraft.copyWith(
            clientId: 5678,
            deliveryDate: DateTime.now().add(const Duration(days: 3)),
            creditBalance: 1000000,
            items: [_item('P001', boxes: 2, pieces: 0)],
          ),
        );
        await notifier.validateAndSubmitOrder();
        expect(notifier.state.errorMessage, isNull);
      });

      test('E6-b (G) — 여신 한도 초과 → SnackBar + 미전송 (레거시 write.jsp:188)', () async {
        seedValidState(); // 총 주문금액 1234
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(creditBalance: 1000),
        );
        await notifier.validateAndSubmitOrder();
        expect(notifier.state.errorMessage, '총 주문금액이 여신잔액을 초과했습니다.');
        expect(formRepo.submitOrderRequestCount, 0);
        expect(notifier.state.isLoanExceeded, true);
      });

      test('I — 납기일 +10일 → requiresDeliveryDateConfirm = true', () async {
        notifier.state = notifier.state.copyWith(
          selectedAccountId: 5678,
          selectedExternalKey: 'EK001',
          orderDraft: notifier.state.orderDraft.copyWith(
            clientId: 5678,
            deliveryDate: DateTime.now().add(const Duration(days: 11)),
            creditBalance: 1000000,
            items: [_item('P001', boxes: 1, pieces: 20)],
            totalAmount: 1000,
          ),
        );
        await notifier.validateAndSubmitOrder();
        // 검증 통과 → 승인요청 확인 다이얼로그 트리거, 아직 미전송.
        expect(notifier.state.requiresSubmitConfirm, true);
        expect(formRepo.submitOrderRequestCount, 0);
        // [예] → +10일 케이스라 납기일 확인창으로 이어짐.
        await notifier.confirmSubmit();
        expect(notifier.state.requiresDeliveryDateConfirm, true);
        expect(formRepo.submitOrderRequestCount, 0);
      });

      test('I-b — 납기일 정확히 +10일 → 확인창 없이 통과 (레거시 +11일부터 확인)', () async {
        seedValidState(); // 총 주문금액 1234
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(
            deliveryDate: DateTime.now().add(const Duration(days: 10)),
            creditBalance: 1000000,
          ),
        );
        await notifier.validateAndSubmitOrder();
        await notifier.confirmSubmit();
        expect(notifier.state.requiresDeliveryDateConfirm, false);
        expect(formRepo.submitOrderRequestCount, 1);
      });

      test('승인요청 확인 — 검증 통과 시 requiresSubmitConfirm = true + 미전송', () async {
        seedValidState();
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(creditBalance: 1000000),
        );

        await notifier.validateAndSubmitOrder();

        expect(notifier.state.requiresSubmitConfirm, true);
        expect(formRepo.submitOrderRequestCount, 0);
      });

      test('cancelSubmitConfirm — [아니오] 시 등록 안 함', () {
        notifier.state = notifier.state.copyWith(requiresSubmitConfirm: true);

        notifier.cancelSubmitConfirm();

        expect(notifier.state.requiresSubmitConfirm, false);
        expect(formRepo.submitOrderRequestCount, 0);
      });

      test('H4 — 검증 통과 → 승인요청 확인 → 등록 200 OK + clientRequestId 폐기',
          () async {
        seedValidState();
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(creditBalance: 1000000),
          clientRequestId: 'idemp-123',
        );

        await notifier.validateAndSubmitOrder();
        await notifier.confirmSubmit();

        expect(notifier.state.requiresSubmitConfirm, false);
        expect(formRepo.submitOrderRequestCount, 1);
        expect(formRepo.lastSubmittedPayload!.clientRequestId, 'idemp-123');
        expect(formRepo.lastSubmittedPayload!.accountId, 5678);
        expect(notifier.state.successMessage, '주문이 접수되었습니다.');
        expect(notifier.state.clientRequestId, isNull);
        expect(notifier.state.submitResult, isNotNull);
      });

      test('confirmDeliveryDateAndSubmit — +10일 다이얼로그 [예] 후 등록', () async {
        notifier.state = notifier.state.copyWith(
          selectedAccountId: 5678,
          selectedExternalKey: 'EK001',
          orderDraft: notifier.state.orderDraft.copyWith(
            clientId: 5678,
            deliveryDate: DateTime.now().add(const Duration(days: 11)),
            creditBalance: 1000000,
            items: [_item('P001', boxes: 1, pieces: 20)],
            totalAmount: 1000,
          ),
          requiresDeliveryDateConfirm: true,
        );

        await notifier.confirmDeliveryDateAndSubmit();

        expect(formRepo.submitOrderRequestCount, 1);
        expect(notifier.state.requiresDeliveryDateConfirm, false);
        expect(notifier.state.successMessage, '주문이 접수되었습니다.');
      });

      test('cancelDeliveryDateConfirm — [아니오] 시 등록 안 함', () {
        notifier.state = notifier.state.copyWith(
          requiresDeliveryDateConfirm: true,
        );

        notifier.cancelDeliveryDateConfirm();

        expect(notifier.state.requiresDeliveryDateConfirm, false);
        expect(formRepo.submitOrderRequestCount, 0);
      });

      test('E12 — 등록 ORD_LOAN_EXCEEDED → API message 패스스루 + clientRequestId 유지', () async {
        seedValidState();
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(creditBalance: 1000000),
          clientRequestId: 'idemp-123',
        );
        formRepo.exceptionToThrow =
            Exception('ORD_LOAN_EXCEEDED: 여신 한도를 초과했습니다 (한도: 1000)');

        await notifier.validateAndSubmitOrder();
        await notifier.confirmSubmit();

        expect(notifier.state.errorMessage, contains('ORD_LOAN_EXCEEDED'));
        expect(notifier.state.clientRequestId, 'idemp-123');
      });

      test('E16 — 403 ORD_ACCOUNT_FORBIDDEN → 한국어 SnackBar', () async {
        seedValidState();
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(creditBalance: 1000000),
        );
        formRepo.exceptionToThrow = Exception('ORD_ACCOUNT_FORBIDDEN');

        await notifier.validateAndSubmitOrder();
        await notifier.confirmSubmit();

        expect(notifier.state.errorMessage, '본인 담당 거래처가 아닙니다.');
      });

      test('등록 INVENTORY_SAP_UNAVAILABLE → 재고 조회 단계 명시 메시지', () async {
        seedValidState();
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(creditBalance: 1000000),
        );
        formRepo.exceptionToThrow =
            _apiError('INVENTORY_SAP_UNAVAILABLE', 'SAP HTTP 500');

        await notifier.validateAndSubmitOrder();
        await notifier.confirmSubmit();

        expect(
          notifier.state.errorMessage,
          '재고 조회 실패(SAP HTTP 500). 잠시 후 다시 시도해주세요.',
        );
      });

      test('등록 INVENTORY_SAP_ERROR → SAP 사유 포함 재고 조회 오류 메시지', () async {
        seedValidState();
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(creditBalance: 1000000),
        );
        formRepo.exceptionToThrow =
            _apiError('INVENTORY_SAP_ERROR', '납기일이 유효하지 않습니다');

        await notifier.validateAndSubmitOrder();
        await notifier.confirmSubmit();

        expect(
          notifier.state.errorMessage,
          '재고 조회 오류: 납기일이 유효하지 않습니다',
        );
      });

      test('등록 LOAN_SAP_UNAVAILABLE → 여신 조회 단계 명시 메시지', () async {
        seedValidState();
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(creditBalance: 1000000),
        );
        formRepo.exceptionToThrow =
            _apiError('LOAN_SAP_UNAVAILABLE', 'SAP 네트워크 오류');

        await notifier.validateAndSubmitOrder();
        await notifier.confirmSubmit();

        expect(
          notifier.state.errorMessage,
          '여신 조회 실패(SAP 네트워크 오류). 잠시 후 다시 시도해주세요.',
        );
      });

      test('등록 코드 있는 서버 메시지에 "SAP" 포함 → 일시 오류로 뭉개지 않고 패스스루', () async {
        seedValidState();
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(creditBalance: 1000000),
        );
        formRepo.exceptionToThrow = _apiError(
          'ORD_INVALID_REQUEST',
          '거래처 SAP 코드(external_key)가 없습니다',
        );

        await notifier.validateAndSubmitOrder();
        await notifier.confirmSubmit();

        expect(
          notifier.state.errorMessage,
          '거래처 SAP 코드(external_key)가 없습니다',
        );
      });
    });
  });
}

/// 서버 표준 에러 응답(`{"error": {"code", "message"}}`) 형태의 DioException 생성.
DioException _apiError(String code, String message) {
  final options = RequestOptions(path: '/api/v1/mobile/order-requests');
  return DioException(
    requestOptions: options,
    type: DioExceptionType.badResponse,
    response: Response(
      requestOptions: options,
      statusCode: 500,
      data: {
        'error': {'code': code, 'message': message},
      },
    ),
  );
}

ProductForOrder _product(
  String code, {
  bool exclusive = false,
  bool tasting = false,
}) {
  return ProductForOrder(
    productCode: code,
    productName: 'P-$code',
    barcode: '8800000$code',
    storageType: '상온',
    shelfLife: '12개월',
    unitPrice: 1000,
    boxSize: 20,
    isFavorite: false,
    productType: exclusive ? 'EXCLUSIVE' : null,
    tasteGiftType: tasting ? 'TASTING_GIFT' : null,
  );
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

OrderDraftItem _item(
  String productCode, {
  int total = 1234,
  double boxes = 1,
  int pieces = 0,
}) {
  return OrderDraftItem(
    productCode: productCode,
    productName: 'P-$productCode',
    quantityBoxes: boxes,
    quantityPieces: pieces,
    unitPrice: 1234,
    boxSize: 20,
    totalPrice: total,
  );
}

