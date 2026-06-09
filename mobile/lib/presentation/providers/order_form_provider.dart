import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../../core/network/dio_provider.dart';
import '../../data/datasources/order_form_api_datasource.dart';
import '../../data/models/order_form/order_draft_request_model.dart';
import '../../data/models/order_form/order_request_payload_model.dart';
import '../../data/repositories/order_form_repository_impl.dart';
import '../../domain/entities/order_draft.dart';
import '../../domain/entities/product_for_order.dart';
import '../../domain/entities/validation_error.dart';
import '../../domain/repositories/order_form_repository.dart';
import '../../domain/usecases/order_form/delete_order_draft.dart';
import '../../domain/usecases/order_form/get_loan_inquiry.dart';
import '../../domain/usecases/order_form/get_order_draft.dart';
import '../../domain/usecases/order_form/save_order_draft.dart';
import '../../domain/usecases/order_form/submit_order_request.dart';
import '../../domain/usecases/search_products_for_order_usecase.dart';
import 'add_product_provider.dart';
import 'order_form_state.dart';

// --- Spec #598 P1-M: 신규 백엔드 연결 Provider ──────────────────────
// (P2-M / P3-M 가 OrderFormNotifier 메서드에서 활용. P1-M 단독 머지 시점에는
// 정의만 추가하고 Notifier 동작은 mock 그대로 유지)

/// 주문서 작성 화면 API DataSource Provider (#598 P1-M).
final orderFormApiDataSourceProvider = Provider<OrderFormApiDataSource>((ref) {
  return OrderFormApiDataSource(ref.watch(dioProvider));
});

/// 주문서 작성 화면 Repository Provider (#598 P1-M).
final orderFormRepositoryProvider = Provider<OrderFormRepository>((ref) {
  return OrderFormRepositoryImpl(
    dataSource: ref.watch(orderFormApiDataSourceProvider),
  );
});

/// `getLoanInquiry` UseCase Provider (#594 호출).
final getLoanInquiryUseCaseProvider = Provider<GetLoanInquiry>((ref) {
  return GetLoanInquiry(ref.watch(orderFormRepositoryProvider));
});

/// `getOrderDraft` UseCase Provider (#596 GET).
final getOrderDraftUseCaseProvider = Provider<GetOrderDraft>((ref) {
  return GetOrderDraft(ref.watch(orderFormRepositoryProvider));
});

/// `saveOrderDraft` UseCase Provider (#596 POST).
final saveOrderDraftUseCaseProvider = Provider<SaveOrderDraft>((ref) {
  return SaveOrderDraft(ref.watch(orderFormRepositoryProvider));
});

/// `deleteOrderDraft` UseCase Provider (#596 DELETE).
final deleteOrderDraftUseCaseProvider = Provider<DeleteOrderDraft>((ref) {
  return DeleteOrderDraft(ref.watch(orderFormRepositoryProvider));
});

/// `submitOrderRequest` UseCase Provider (#592 POST + clientRequestId).
final submitOrderRequestUseCaseProvider = Provider<SubmitOrderRequest>((ref) {
  return SubmitOrderRequest(ref.watch(orderFormRepositoryProvider));
});

// --- OrderFormNotifier ---

/// 주문서 작성 상태 관리 Notifier
///
/// 주문서 작성, 임시저장, 유효성 검증, 승인요청 기능을 관리합니다.
/// Spec #598 P2-M / P3-M — 모든 흐름이 신규 백엔드 (#594/#596/#592) 실 API 사용.
class OrderFormNotifier extends StateNotifier<OrderFormState> {
  final GetLoanInquiry _getLoanInquiry;
  final GetOrderDraft _getOrderDraft;
  final SaveOrderDraft _saveOrderDraft;
  final DeleteOrderDraft _deleteOrderDraft;
  final SubmitOrderRequest _submitOrderRequest;
  final SearchProductsForOrder _searchProductsForOrder;
  final Uuid _uuid;

  OrderFormNotifier({
    required GetLoanInquiry getLoanInquiry,
    required GetOrderDraft getOrderDraft,
    required SaveOrderDraft saveOrderDraft,
    required DeleteOrderDraft deleteOrderDraft,
    required SubmitOrderRequest submitOrderRequest,
    required SearchProductsForOrder searchProductsForOrder,
    Uuid? uuid,
  })  : _getLoanInquiry = getLoanInquiry,
        _getOrderDraft = getOrderDraft,
        _saveOrderDraft = saveOrderDraft,
        _deleteOrderDraft = deleteOrderDraft,
        _submitOrderRequest = submitOrderRequest,
        _searchProductsForOrder = searchProductsForOrder,
        _uuid = uuid ?? const Uuid(),
        super(OrderFormState.initial());

  /// 초기화 (Spec #598 P2-M §2.1).
  ///
  /// [orderId]: 수정할 주문 ID (있으면 수정 모드)
  /// [clients]: 거래처 ID → 이름
  /// [clientExternalKeys]: 거래처 ID → SAP externalKey (`getLoanInquiry` 호출용)
  Future<void> initialize({
    int? orderId,
    Map<int, String>? clients,
    Map<int, String>? clientExternalKeys,
  }) async {
    // 신규 폼 — UUID v4 멱등키 발급 (P1-M state 필드)
    final newClientRequestId = orderId == null ? _uuid.v4() : null;

    state = state.copyWith(
      clients: clients ?? state.clients,
      clientExternalKeys: clientExternalKeys ?? state.clientExternalKeys,
      clientRequestId: newClientRequestId,
    );

    // 수정 모드 (향후 구현)
    if (orderId != null) {
      return;
    }

    // 신규 작성 모드: 임시저장 데이터 확인
    try {
      final draft = await _getOrderDraft.call();
      if (draft != null) {
        // 임시 보관 — 다이얼로그 사용자 선택 후 acceptDraft / declineDraft 에서 활용
        state = state.copyWith(
          hasDraft: true,
          pendingDraft: draft,
        );
      } else {
        state = state.copyWith(hasDraft: false, clearPendingDraft: true);
      }
    } catch (e) {
      // 임시저장 조회 실패는 SnackBar 만 노출, 빈 폼으로 진입
      state = state.copyWith(
        hasDraft: false,
        clearPendingDraft: true,
        errorMessage: '임시저장 조회 중 오류가 발생했습니다.',
      );
    }
  }

  /// 임시저장 다이얼로그 [예] — 폼 채움 + 여신 호출 (Spec #598 P2-M §2.2).
  Future<void> acceptDraft() async {
    final pending = state.pendingDraft;
    if (pending == null) return;

    final items = pending.lines
        .map((l) => OrderDraftItem(
              productCode: l.productCode,
              productName: l.productName,
              quantityBoxes: l.quantityBoxes ?? 0,
              quantityPieces: l.quantityPieces ?? 0,
              unitPrice: (l.unitPrice ?? 0).toInt(),
              boxSize: 1,
              totalPrice: (l.amount ?? 0).toInt(),
            ))
        .toList();

    final filledDraft = OrderDraft(
      clientId: pending.accountId,
      clientName: pending.accountName,
      deliveryDate: pending.deliveryDate != null
          ? DateTime.tryParse(pending.deliveryDate!)
          : null,
      items: items,
      totalAmount: pending.totalAmount,
      isDraft: true,
      lastModified: DateTime.now(),
    );

    state = state.copyWith(
      orderDraft: filledDraft,
      hasDraft: false,
      draftId: pending.draftId,
      selectedAccountId: pending.accountId,
      selectedExternalKey: pending.accountExternalKey,
      clearPendingDraft: true,
    );

    if (pending.accountExternalKey.isNotEmpty) {
      await _fetchLoanInquiry(pending.accountExternalKey);
    }
  }

  /// 임시저장 다이얼로그 [아니오] — DELETE + 빈 폼 (Spec #598 P2-M §2.2).
  Future<void> declineDraft() async {
    try {
      await _deleteOrderDraft.call();
      state = state.copyWith(
        hasDraft: false,
        clearPendingDraft: true,
        clearDraftId: true,
      );
    } catch (e) {
      state = state.copyWith(
        errorMessage: '삭제 중 오류가 발생했습니다.',
      );
    }
  }

  /// 새 주문서 초기화 (DraftBanner [새로 작성] — 다이얼로그 [아니오] 와 동일 효과).
  void initializeNewOrder() {
    state = state.copyWith(
      orderDraft: OrderDraft.empty(),
      hasDraft: false,
      clearPendingDraft: true,
      clearValidationErrors: true,
      clearError: true,
      clearSuccess: true,
    );
  }

  /// 거래처 선택 (Spec #598 P2-M §2.3).
  void selectClient(int clientId, String clientName, [String? externalKey]) {
    final resolvedExternalKey = externalKey ?? state.clientExternalKeys[clientId];

    state = state.copyWith(
      orderDraft: state.orderDraft.copyWith(
        clientId: clientId,
        clientName: clientName,
      ),
      selectedAccountId: clientId,
      selectedExternalKey: resolvedExternalKey,
    );

    if (resolvedExternalKey != null && resolvedExternalKey.isNotEmpty) {
      _fetchLoanInquiry(resolvedExternalKey);
    }
  }

  /// 여신 잔액 조회 (Spec #598 P2-M §2.3) — 내부 헬퍼.
  Future<void> _fetchLoanInquiry(String externalKey) async {
    // creditBalance 를 null 로 클리어해 스피너 표시 유도
    state = state.copyWith(
      orderDraft: state.orderDraft.copyWith(creditBalance: null),
      isLoading: true,
      clearError: true,
    );

    try {
      final response = await _getLoanInquiry.call(externalKey: externalKey);
      state = state.copyWith(
        isLoading: false,
        orderDraft: state.orderDraft.copyWith(
          creditBalance: response.creditBalance,
        ),
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: _mapLoanInquiryError(e),
      );
    }
  }

  String _mapLoanInquiryError(Object error) {
    final message = extractErrorMessage(error).toLowerCase();
    if (message.contains('html') ||
        message.contains('unavailable') ||
        message.contains('연결')) {
      return 'SAP 시스템 연결에 실패했습니다.';
    }
    return '여신 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
  }

  /// 납기일 설정
  void setDeliveryDate(DateTime date) {
    state = state.copyWith(
      orderDraft: state.orderDraft.copyWith(deliveryDate: date),
    );
  }

  /// 제품 추가 (Spec #598 P3-M §2.1) — 차단 룰 3종 적용 후 라인 추가.
  ///
  /// Returns true 면 라인 추가됨, false 면 차단 (errorMessage 가 SnackBar 로 표시).
  bool addProductLine(ProductForOrder product) {
    // (1) 전용상품 차단
    if (product.isExclusive) {
      state = state.copyWith(errorMessage: '전용상품은 추가할 수 없습니다.');
      return false;
    }
    // (2) 시식·증정용 차단
    if (product.isTastingGift) {
      state = state.copyWith(errorMessage: '시식/증정용 상품은 추가할 수 없습니다.');
      return false;
    }
    // (3) 중복 차단
    final isDuplicate = state.orderDraft.items
        .any((existing) => existing.productCode == product.productCode);
    if (isDuplicate) {
      state = state.copyWith(errorMessage: '이미 추가된 제품입니다.');
      return false;
    }

    final newItem = OrderDraftItem(
      productCode: product.productCode,
      productName: product.productName,
      quantityBoxes: 0,
      quantityPieces: 0,
      unitPrice: product.unitPrice,
      boxSize: product.boxSize,
      totalPrice: 0,
    );
    final updatedItems = [...state.orderDraft.items, newItem];
    final totalAmount = updatedItems.fold(0, (sum, i) => sum + i.totalPrice);

    state = state.copyWith(
      orderDraft: state.orderDraft.copyWith(
        items: updatedItems,
        totalAmount: totalAmount,
      ),
    );
    return true;
  }

  /// 제품검색 결과의 "주문서 등록"에서 진입 시, 제품코드로 제품을 찾아
  /// 주문 라인에 미리 추가한다 (레거시 `/order/write?productcode=` 정합).
  /// 검색 실패/미일치는 조용히 무시하고 빈 주문서로 진입한다.
  Future<void> preloadProductByCode(String productCode) async {
    try {
      final results = await _searchProductsForOrder.call(query: productCode);
      ProductForOrder? match;
      for (final p in results) {
        if (p.productCode == productCode) {
          match = p;
          break;
        }
      }
      if (match != null) {
        addProductLine(match);
      }
    } catch (_) {
      // 프리필 실패는 무시 (빈 주문서로 진입)
    }
  }

  /// 바코드 스캔으로 제품을 조회해 주문 라인에 추가한다.
  ///
  /// 스캐너에서 받은 [barcode] 로 제품 검색을 수행하고, 바코드/제품코드가 일치하는 제품
  /// (없으면 첫 결과)을 [addProductLine] 으로 추가한다. 성공/차단/조회실패는
  /// successMessage / errorMessage 로 표시되며 페이지 listener 가 SnackBar 로 노출한다.
  Future<void> addProductByBarcode(String barcode) async {
    final code = barcode.trim();
    if (code.isEmpty) return;
    try {
      final results = await _searchProductsForOrder.call(query: code);
      if (results.isEmpty) {
        state = state.copyWith(errorMessage: '바코드에 해당하는 제품이 없습니다.');
        return;
      }
      final match = results.firstWhere(
        (p) => p.barcode == code || p.productCode == code,
        orElse: () => results.first,
      );
      if (addProductLine(match)) {
        state = state.copyWith(successMessage: '${match.productName} 추가됨');
      }
    } catch (e) {
      state = state.copyWith(errorMessage: extractErrorMessage(e));
    }
  }

  /// 제품 추가 (Legacy — OrderDraftItem 직접 전달, 회귀 호환).
  void addProductToOrder(OrderDraftItem item) {
    final isDuplicate = state.orderDraft.items
        .any((existing) => existing.productCode == item.productCode);
    if (isDuplicate) {
      return;
    }

    final updatedItems = [...state.orderDraft.items, item];
    final totalAmount = updatedItems.fold(0, (sum, i) => sum + i.totalPrice);

    state = state.copyWith(
      orderDraft: state.orderDraft.copyWith(
        items: updatedItems,
        totalAmount: totalAmount,
      ),
    );
  }

  /// 선택된 제품 삭제
  void removeSelectedProducts() {
    // 선택되지 않은 제품만 남김
    final remainingItems = state.orderDraft.items
        .where((item) => !item.isSelected)
        .toList();
    final totalAmount =
        remainingItems.fold(0, (sum, item) => sum + item.totalPrice);

    // 삭제된 제품의 유효성 에러 제거
    final updatedValidationErrors =
        Map<String, ValidationError>.from(state.validationErrors);
    for (final item in state.orderDraft.items) {
      if (item.isSelected) {
        updatedValidationErrors.remove(item.productCode);
      }
    }

    state = state.copyWith(
      orderDraft: state.orderDraft.copyWith(
        items: remainingItems,
        totalAmount: totalAmount,
      ),
      validationErrors: updatedValidationErrors,
    );
  }

  /// 제품 수량 변경
  void updateProductQuantity(
    String productCode,
    double boxes,
    int pieces,
  ) {
    final updatedItems = state.orderDraft.items.map((item) {
      if (item.productCode == productCode) {
        // 총액 재계산
        final totalPieces = (boxes * item.boxSize).round() + pieces;
        final totalPrice = (totalPieces * item.unitPrice / item.boxSize).round();

        return item.copyWith(
          quantityBoxes: boxes,
          quantityPieces: pieces,
          totalPrice: totalPrice,
        ).clearValidationError();
      }
      return item;
    }).toList();

    final totalAmount = updatedItems.fold(0, (sum, item) => sum + item.totalPrice);

    // 해당 제품의 유효성 에러 제거
    final updatedValidationErrors =
        Map<String, ValidationError>.from(state.validationErrors);
    updatedValidationErrors.remove(productCode);

    state = state.copyWith(
      orderDraft: state.orderDraft.copyWith(
        items: updatedItems,
        totalAmount: totalAmount,
      ),
      validationErrors: updatedValidationErrors,
    );
  }

  /// 제품 선택 토글
  void toggleProductSelection(String productCode) {
    final updatedItems = state.orderDraft.items.map((item) {
      if (item.productCode == productCode) {
        return item.copyWith(isSelected: !item.isSelected);
      }
      return item;
    }).toList();

    state = state.copyWith(
      orderDraft: state.orderDraft.copyWith(items: updatedItems),
    );
  }

  /// 전체 제품 선택 토글
  void toggleSelectAllProducts() {
    final allSelected = state.orderDraft.allItemsSelected;
    final updatedItems = state.orderDraft.items.map((item) {
      return item.copyWith(isSelected: !allSelected);
    }).toList();

    state = state.copyWith(
      orderDraft: state.orderDraft.copyWith(items: updatedItems),
    );
  }

  /// 유효성 검증 (A)~(I) + 주문서 전송 (Spec #598 P3-M §2.6 / §2.7).
  ///
  /// (I) 납기일 +10일 케이스에서는 [requiresDeliveryDateConfirm] 가 true 가 되어
  /// 사용자가 다이얼로그 [예] 클릭 시 [confirmDeliveryDateAndSubmit] 호출하여 진행.
  Future<void> validateAndSubmitOrder() async {
    final blocker = _runBlockingValidations();
    if (blocker != null) {
      state = state.copyWith(errorMessage: blocker);
      return;
    }

    // (I) 납기일 +10일
    if (_isDeliveryFarOff()) {
      state = state.copyWith(requiresDeliveryDateConfirm: true);
      return;
    }

    await _submitOrderInternal();
  }

  /// (I) +10일 다이얼로그에서 [예] 선택 시 호출.
  Future<void> confirmDeliveryDateAndSubmit() async {
    state = state.copyWith(clearRequiresDeliveryDateConfirm: true);
    await _submitOrderInternal();
  }

  /// (I) +10일 다이얼로그에서 [아니오] 선택 시 호출 (혹은 닫힘).
  void cancelDeliveryDateConfirm() {
    state = state.copyWith(clearRequiresDeliveryDateConfirm: true);
  }

  /// 검증 (A)~(H) — 차단되면 SnackBar 메시지를 반환.
  String? _runBlockingValidations() {
    // (A) 거래처 미선택
    if (state.selectedAccountId == null) {
      return '거래처를 선택해 주세요';
    }
    // (B) 납기일 미선택
    if (state.deliveryDate == null) {
      return '납기일을 선택해 주세요';
    }
    // (C) 라인 productCode 중복 (방어)
    final codes = state.orderDraft.items.map((e) => e.productCode).toList();
    if (codes.toSet().length != codes.length) {
      return '중복된 제품이 있습니다. 라인을 정리해주세요';
    }
    // (D) 납기일 < 오늘 (방어)
    final today = DateTime.now();
    final todayDate = DateTime(today.year, today.month, today.day);
    if (state.deliveryDate!.isBefore(todayDate)) {
      return '납기일은 오늘 이후여야 합니다.';
    }
    // (E) 라인 0개
    if (state.orderDraft.items.isEmpty) {
      return '주문할 제품을 추가해주세요';
    }
    // (F) 라인 100개 초과
    if (state.orderDraft.items.length > 100) {
      return '제품은 100개 이하로 추가해주세요';
    }
    // (G) 여신 한도 초과 / 조회 중
    final creditBalance = state.creditBalance;
    if (creditBalance == null && state.selectedExternalKey != null) {
      return '여신 조회 중입니다. 잠시 후 다시 시도해주세요';
    }
    if (creditBalance != null && state.totalAmount > creditBalance) {
      return null; // 버튼 disabled 로 도달 자체 차단 — SnackBar 표시 안 함
    }
    // (H) 라인 총EA <= 0
    final zeroLine = state.orderDraft.items.firstWhere(
      (e) => e.quantityPieces <= 0,
      orElse: () => const OrderDraftItem(
        productCode: '',
        productName: '',
        quantityBoxes: 0,
        quantityPieces: 1,
        unitPrice: 0,
        boxSize: 1,
        totalPrice: 0,
      ),
    );
    if (zeroLine.productCode.isNotEmpty) {
      return '수량이 0인 라인이 있습니다.';
    }
    return null;
  }

  /// (I) 납기일 ≥ 주문일+10일 룰.
  bool _isDeliveryFarOff() {
    final delivery = state.deliveryDate;
    if (delivery == null) return false;
    final today = DateTime.now();
    final todayDate = DateTime(today.year, today.month, today.day);
    return delivery.difference(todayDate).inDays >= 10;
  }

  Future<void> _submitOrderInternal() async {
    state = state.copyWith(
      isSubmitting: true,
      clearError: true,
      clearSuccess: true,
    );

    final payload = OrderRequestPayloadModel(
      clientRequestId: state.clientRequestId,
      accountId: state.selectedAccountId!,
      deliveryDate:
          state.deliveryDate!.toIso8601String().substring(0, 10),
      totalAmount: state.totalAmount,
      lines: state.orderDraft.items.asMap().entries.map((entry) {
        final index = entry.key;
        final item = entry.value;
        final unit = item.quantityBoxes > 0 ? 'BOX' : 'EA';
        return OrderRequestLineModel(
          lineNumber: index + 1,
          productCode: item.productCode,
          quantity: unit == 'BOX'
              ? item.quantityBoxes
              : item.quantityPieces.toDouble(),
          unit: unit,
          quantityPieces: item.quantityPieces,
          quantityBoxes: item.quantityBoxes,
        );
      }).toList(),
    );

    try {
      final response = await _submitOrderRequest.call(payload: payload);
      state = state.copyWith(
        isSubmitting: false,
        submitResult: OrderSubmitResult(
          orderId: response.orderRequestId,
          orderRequestNumber: response.orderRequestNumber,
          status: response.status,
        ),
        successMessage: '주문이 접수되었습니다.',
        clearError: true,
        clearClientRequestId: true,
      );
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: _mapSubmitError(e),
      );
    }
  }

  String _mapSubmitError(Object error) {
    final raw = extractErrorMessage(error);
    if (raw.contains('ORD_LOAN_EXCEEDED') || raw.contains('여신 한도')) {
      return raw;
    }
    if (raw.contains('ORD_PRODUCT_RESTRICTED') || raw.contains('공급제한')) {
      return raw;
    }
    if (raw.contains('ORD_INVALID_UNIT')) {
      return raw;
    }
    if (raw.contains('ORD_INVALID_REQUEST')) {
      return raw;
    }
    if (raw.contains('ORD_ACCOUNT_FORBIDDEN') || raw.contains('FORBIDDEN')) {
      return '본인 담당 거래처가 아닙니다.';
    }
    if (raw.contains('LOAN_SAP_UNAVAILABLE') || raw.contains('UNAVAILABLE')) {
      return 'SAP 연결 실패. 잠시 후 다시 시도해주세요.';
    }
    if (raw.contains('LOAN_SAP_ERROR') || raw.contains('SAP')) {
      return 'SAP 일시 오류. 잠시 후 다시 시도해주세요.';
    }
    return '오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
  }

  /// 임시저장 등록 (Spec #598 P2-M §2.4).
  Future<void> saveDraft() async {
    state = state.copyWith(
      isSubmitting: true,
      clearError: true,
      clearSuccess: true,
    );

    final accountId = state.selectedAccountId;
    if (accountId == null) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: '거래처를 선택해주세요.',
      );
      return;
    }

    final request = OrderDraftRequestModel(
      accountId: accountId,
      deliveryDate: state.orderDraft.deliveryDate?.toIso8601String().substring(0, 10),
      totalAmount: state.totalAmount,
      lines: state.orderDraft.items.map((item) {
        return OrderDraftRequestLineModel(
          lineNumber: state.orderDraft.items.indexOf(item) + 1,
          productCode: item.productCode,
          unit: item.quantityBoxes > 0 ? 'BOX' : 'EA',
          quantity: item.quantityBoxes > 0
              ? item.quantityBoxes
              : item.quantityPieces.toDouble(),
          quantityPieces: item.quantityPieces,
          quantityBoxes: item.quantityBoxes,
          unitPrice: item.unitPrice.toDouble(),
          amount: item.totalPrice.toDouble(),
        );
      }).toList(),
    );

    try {
      final saved = await _saveOrderDraft.call(request: request);
      state = state.copyWith(
        isSubmitting: false,
        successMessage: '임시저장이 완료되었습니다.',
        draftId: saved.draftId,
        clearError: true,
      );
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: _mapDraftSaveError(e),
      );
    }
  }

  String _mapDraftSaveError(Object error) {
    final message = extractErrorMessage(error);
    if (message.contains('ORD_DRAFT_ACCOUNT_FORBIDDEN') ||
        message.contains('FORBIDDEN')) {
      return '본인 담당 거래처가 아닙니다.';
    }
    if (message.contains('ORD_DRAFT_INVALID_REQUEST')) {
      return message;
    }
    return '오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
  }

  /// 임시저장 삭제 (Spec #598 P2-M §2.5) — 현 페이지 유지 + 빈 폼 (Q11).
  Future<void> deleteDraft() async {
    try {
      await _deleteOrderDraft.call();
      state = OrderFormState.initial().copyWith(
        clientRequestId: _uuid.v4(),
        clients: state.clients,
        clientExternalKeys: state.clientExternalKeys,
        successMessage: '삭제되었습니다.',
      );
    } catch (e) {
      state = state.copyWith(
        errorMessage: '삭제 중 오류가 발생했습니다.',
      );
    }
  }

  /// 페이지 이탈 시 폼 폐기 (Spec #598 P2-M §2.6 [그냥 나가기]).
  void discardForm() {
    state = OrderFormState.initial();
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(clearError: true);
  }

  /// 성공 메시지 초기화
  void clearSuccess() {
    state = state.copyWith(clearSuccess: true);
  }

  /// 유효성 에러 초기화
  void clearValidationErrors() {
    state = state.copyWith(clearValidationErrors: true);

    // 각 제품의 validationError도 클리어
    final updatedItems = state.orderDraft.items.map((item) {
      return item.clearValidationError();
    }).toList();

    state = state.copyWith(
      orderDraft: state.orderDraft.copyWith(items: updatedItems),
    );
  }
}

/// OrderForm StateNotifier Provider
final orderFormProvider =
    StateNotifierProvider<OrderFormNotifier, OrderFormState>((ref) {
  return OrderFormNotifier(
    getLoanInquiry: ref.watch(getLoanInquiryUseCaseProvider),
    getOrderDraft: ref.watch(getOrderDraftUseCaseProvider),
    saveOrderDraft: ref.watch(saveOrderDraftUseCaseProvider),
    deleteOrderDraft: ref.watch(deleteOrderDraftUseCaseProvider),
    submitOrderRequest: ref.watch(submitOrderRequestUseCaseProvider),
    searchProductsForOrder: ref.watch(searchProductsForOrderUseCaseProvider),
  );
});
