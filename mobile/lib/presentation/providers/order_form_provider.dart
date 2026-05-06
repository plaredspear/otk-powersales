import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../../core/network/dio_provider.dart';
import '../../data/datasources/order_form_api_datasource.dart';
import '../../data/models/order_form/order_draft_request_model.dart';
import '../../data/repositories/order_form_repository_impl.dart';
import '../../domain/entities/order_draft.dart';
import '../../domain/entities/validation_error.dart';
import '../../domain/repositories/order_form_repository.dart';
import '../../domain/usecases/order_form/delete_order_draft.dart';
import '../../domain/usecases/order_form/get_loan_inquiry.dart';
import '../../domain/usecases/order_form/get_order_draft.dart';
import '../../domain/usecases/order_form/save_order_draft.dart';
import '../../domain/usecases/order_form/submit_order_request.dart';
import '../../domain/usecases/submit_order_usecase.dart';
import '../../domain/usecases/update_order_usecase.dart';
import '../../domain/usecases/validate_order_usecase.dart';
import 'order_form_state.dart';
import 'order_request_list_provider.dart';

// --- Dependency Providers ---

/// ValidateOrder UseCase Provider
final validateOrderUseCaseProvider = Provider<ValidateOrder>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
  return ValidateOrder(repository);
});

/// SubmitOrder UseCase Provider
final submitOrderUseCaseProvider = Provider<SubmitOrder>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
  return SubmitOrder(repository);
});

/// UpdateOrder UseCase Provider
final updateOrderUseCaseProvider = Provider<UpdateOrder>((ref) {
  final repository = ref.watch(orderRequestRepositoryProvider);
  return UpdateOrder(repository);
});

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
/// Spec #598 P2-M — 임시저장/거래처/여신 흐름은 신규 백엔드 (#594/#596) 실 API 사용.
/// 제품 추가/검증/등록 흐름은 기존 mock 유지 (P3-M 에서 전환 예정).
class OrderFormNotifier extends StateNotifier<OrderFormState> {
  final GetLoanInquiry _getLoanInquiry;
  final GetOrderDraft _getOrderDraft;
  final SaveOrderDraft _saveOrderDraft;
  final DeleteOrderDraft _deleteOrderDraft;
  final ValidateOrder _validateOrder;
  final SubmitOrder _submitOrder;
  final UpdateOrder _updateOrder;
  final Uuid _uuid;

  OrderFormNotifier({
    required GetLoanInquiry getLoanInquiry,
    required GetOrderDraft getOrderDraft,
    required SaveOrderDraft saveOrderDraft,
    required DeleteOrderDraft deleteOrderDraft,
    required ValidateOrder validateOrder,
    required SubmitOrder submitOrder,
    required UpdateOrder updateOrder,
    Uuid? uuid,
  })  : _getLoanInquiry = getLoanInquiry,
        _getOrderDraft = getOrderDraft,
        _saveOrderDraft = saveOrderDraft,
        _deleteOrderDraft = deleteOrderDraft,
        _validateOrder = validateOrder,
        _submitOrder = submitOrder,
        _updateOrder = updateOrder,
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

  /// 제품 추가
  void addProductToOrder(OrderDraftItem item) {
    // 중복 체크 (productCode 기준)
    final isDuplicate = state.orderDraft.items
        .any((existing) => existing.productCode == item.productCode);
    if (isDuplicate) {
      return;
    }

    // 제품 추가
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

  /// 유효성 검증 및 주문서 전송
  Future<void> validateAndSubmitOrder() async {
    state = state.copyWith(
      isSubmitting: true,
      clearError: true,
      clearSuccess: true,
    );

    try {
      // 유효성 검증
      final validationResult = await _validateOrder.call(
        orderDraft: state.orderDraft,
      );

      if (!validationResult.isValid) {
        // 유효성 에러 적용
        final updatedItems = state.orderDraft.items.map((item) {
          final error = validationResult.errors[item.productCode];
          if (error != null) {
            return item.copyWith(validationError: error);
          }
          return item;
        }).toList();

        // 폼 레벨 에러가 있으면 에러 메시지 설정
        final formError = validationResult.errors['_form'];
        final errorMessage = formError?.message ?? '유효성 검증 실패';

        state = state.copyWith(
          isSubmitting: false,
          orderDraft: state.orderDraft.copyWith(items: updatedItems),
          validationErrors: validationResult.errors,
          errorMessage: errorMessage,
        );
        return;
      }

      // 주문서 전송 (수정 모드 or 신규 작성)
      final result = state.isEditMode
          ? await _updateOrder.call(
              orderId: state.orderDraft.id!,
              orderDraft: state.orderDraft,
            )
          : await _submitOrder.call(orderDraft: state.orderDraft);

      state = state.copyWith(
        isSubmitting: false,
        submitResult: result,
        successMessage: '주문서가 성공적으로 전송되었습니다.',
        clearError: true,
      );
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: extractErrorMessage(e),
      );
    }
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
    validateOrder: ref.watch(validateOrderUseCaseProvider),
    submitOrder: ref.watch(submitOrderUseCaseProvider),
    updateOrder: ref.watch(updateOrderUseCaseProvider),
  );
});
