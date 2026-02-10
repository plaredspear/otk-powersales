import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/order_draft.dart';
import '../../domain/entities/validation_error.dart';
import '../../domain/usecases/delete_draft_order_usecase.dart';
import '../../domain/usecases/get_credit_balance_usecase.dart';
import '../../domain/usecases/load_draft_order_usecase.dart';
import '../../domain/usecases/save_draft_order_usecase.dart';
import '../../domain/usecases/submit_order_usecase.dart';
import '../../domain/usecases/update_order_usecase.dart';
import '../../domain/usecases/validate_order_usecase.dart';
import 'order_form_state.dart';
import 'order_list_provider.dart';

// --- Dependency Providers ---

/// GetCreditBalance UseCase Provider
final getCreditBalanceUseCaseProvider = Provider<GetCreditBalance>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return GetCreditBalance(repository);
});

/// LoadDraftOrder UseCase Provider
final loadDraftOrderUseCaseProvider = Provider<LoadDraftOrder>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return LoadDraftOrder(repository);
});

/// SaveDraftOrder UseCase Provider
final saveDraftOrderUseCaseProvider = Provider<SaveDraftOrder>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return SaveDraftOrder(repository);
});

/// DeleteDraftOrder UseCase Provider
final deleteDraftOrderUseCaseProvider = Provider<DeleteDraftOrder>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return DeleteDraftOrder(repository);
});

/// ValidateOrder UseCase Provider
final validateOrderUseCaseProvider = Provider<ValidateOrder>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return ValidateOrder(repository);
});

/// SubmitOrder UseCase Provider
final submitOrderUseCaseProvider = Provider<SubmitOrder>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return SubmitOrder(repository);
});

/// UpdateOrder UseCase Provider
final updateOrderUseCaseProvider = Provider<UpdateOrder>((ref) {
  final repository = ref.watch(orderRepositoryProvider);
  return UpdateOrder(repository);
});

// --- OrderFormNotifier ---

/// 주문서 작성 상태 관리 Notifier
///
/// 주문서 작성, 임시저장, 유효성 검증, 승인요청 기능을 관리합니다.
class OrderFormNotifier extends StateNotifier<OrderFormState> {
  final GetCreditBalance _getCreditBalance;
  final LoadDraftOrder _loadDraftOrder;
  final SaveDraftOrder _saveDraftOrder;
  final DeleteDraftOrder _deleteDraftOrder;
  final ValidateOrder _validateOrder;
  final SubmitOrder _submitOrder;
  final UpdateOrder _updateOrder;

  OrderFormNotifier({
    required GetCreditBalance getCreditBalance,
    required LoadDraftOrder loadDraftOrder,
    required SaveDraftOrder saveDraftOrder,
    required DeleteDraftOrder deleteDraftOrder,
    required ValidateOrder validateOrder,
    required SubmitOrder submitOrder,
    required UpdateOrder updateOrder,
  })  : _getCreditBalance = getCreditBalance,
        _loadDraftOrder = loadDraftOrder,
        _saveDraftOrder = saveDraftOrder,
        _deleteDraftOrder = deleteDraftOrder,
        _validateOrder = validateOrder,
        _submitOrder = submitOrder,
        _updateOrder = updateOrder,
        super(OrderFormState.initial());

  /// 초기화
  ///
  /// [orderId]: 수정할 주문 ID (있으면 수정 모드)
  /// [clients]: 거래처 목록
  Future<void> initialize({
    int? orderId,
    Map<int, String>? clients,
  }) async {
    // 거래처 목록 설정
    if (clients != null) {
      state = state.copyWith(clients: clients);
    }

    // 수정 모드 (향후 구현: 기존 주문 불러오기)
    if (orderId != null) {
      // TODO: 기존 주문 불러오기 로직 추가
      return;
    }

    // 신규 작성 모드: 임시저장 데이터 확인
    try {
      final draft = await _loadDraftOrder.call();
      if (draft != null) {
        state = state.copyWith(hasDraft: true);
      }
    } catch (e) {
      // 임시저장 불러오기 실패는 무시 (초기화 계속 진행)
    }
  }

  /// 임시저장 주문서 불러오기
  Future<void> loadDraftOrder() async {
    state = state.toLoading();

    try {
      final draft = await _loadDraftOrder.call();
      if (draft != null) {
        state = state.copyWith(
          isLoading: false,
          orderDraft: draft,
          hasDraft: false,
          clearError: true,
        );
      } else {
        state = state.toError('불러올 임시저장 데이터가 없습니다.');
      }
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 새 주문서 초기화
  void initializeNewOrder() {
    state = state.copyWith(
      orderDraft: OrderDraft.empty(),
      hasDraft: false,
      clearValidationErrors: true,
      clearError: true,
      clearSuccess: true,
    );
  }

  /// 거래처 선택
  void selectClient(int clientId, String clientName) {
    state = state.copyWith(
      orderDraft: state.orderDraft.copyWith(
        clientId: clientId,
        clientName: clientName,
      ),
    );
    fetchCreditBalance(clientId);
  }

  /// 여신 잔액 조회
  Future<void> fetchCreditBalance(int clientId) async {
    state = state.toLoading();

    try {
      final creditBalance = await _getCreditBalance.call(clientId: clientId);
      state = state.copyWith(
        isLoading: false,
        orderDraft: state.orderDraft.copyWith(creditBalance: creditBalance),
        clearError: true,
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
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
        errorMessage: e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 임시저장
  Future<void> saveDraft() async {
    state = state.copyWith(
      isSubmitting: true,
      clearError: true,
      clearSuccess: true,
    );

    try {
      await _saveDraftOrder.call(orderDraft: state.orderDraft);

      state = state.copyWith(
        isSubmitting: false,
        successMessage: '임시저장되었습니다.',
        clearError: true,
      );
    } catch (e) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 임시저장 데이터 삭제
  Future<void> deleteOrder() async {
    try {
      await _deleteDraftOrder.call();
      state = OrderFormState.initial();
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
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
    getCreditBalance: ref.watch(getCreditBalanceUseCaseProvider),
    loadDraftOrder: ref.watch(loadDraftOrderUseCaseProvider),
    saveDraftOrder: ref.watch(saveDraftOrderUseCaseProvider),
    deleteDraftOrder: ref.watch(deleteDraftOrderUseCaseProvider),
    validateOrder: ref.watch(validateOrderUseCaseProvider),
    submitOrder: ref.watch(submitOrderUseCaseProvider),
    updateOrder: ref.watch(updateOrderUseCaseProvider),
  );
});
