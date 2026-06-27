import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/product_expiration_form.dart';
import '../../domain/entities/product_expiration_item.dart';
import '../../domain/usecases/delete_product_expiration_usecase.dart';
import '../../domain/usecases/register_product_expiration_usecase.dart';
import '../../domain/usecases/update_product_expiration_usecase.dart';
import 'product_expiration_form_state.dart';
import 'product_expiration_list_provider.dart';

// ============================================
// 1. Dependency Providers
// ============================================

/// RegisterProductExpiration UseCase Provider
final registerProductExpirationUseCaseProvider = Provider<RegisterProductExpiration>((ref) {
  final repository = ref.watch(productExpirationRepositoryProvider);
  return RegisterProductExpiration(repository);
});

/// UpdateProductExpiration UseCase Provider
final updateProductExpirationUseCaseProvider = Provider<UpdateProductExpiration>((ref) {
  final repository = ref.watch(productExpirationRepositoryProvider);
  return UpdateProductExpiration(repository);
});

/// DeleteProductExpiration UseCase Provider (단건 삭제)
final deleteProductExpirationUseCaseProvider = Provider<DeleteProductExpiration>((ref) {
  final repository = ref.watch(productExpirationRepositoryProvider);
  return DeleteProductExpiration(repository);
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

/// 소비기한 등록/수정 폼 상태 관리 Notifier
class ProductExpirationFormNotifier extends StateNotifier<ProductExpirationFormState> {
  final RegisterProductExpiration _registerProductExpiration;
  final UpdateProductExpiration _updateProductExpiration;
  final DeleteProductExpiration _deleteProductExpiration;

  ProductExpirationFormNotifier({
    required RegisterProductExpiration registerProductExpiration,
    required UpdateProductExpiration updateProductExpiration,
    required DeleteProductExpiration deleteProductExpiration,
  })  : _registerProductExpiration = registerProductExpiration,
        _updateProductExpiration = updateProductExpiration,
        _deleteProductExpiration = deleteProductExpiration,
        super(ProductExpirationFormState.initial());

  /// 등록 모드 초기화
  ///
  /// 공유 Notifier 가 직전 수정 모드의 상태를 들고 있을 수 있으므로 등록 화면
  /// 진입 시 깨끗한 초기 상태로 되돌린다. 거래처는 [AccountSelectorSheet] 가
  /// 직접 조회하므로 여기서 미리 불러오지 않는다.
  void initializeForRegister() {
    state = ProductExpirationFormState.initial();
  }

  /// 수정 모드 초기화 (기존 데이터 로드)
  void initializeForEdit(ProductExpirationItem item) {
    state = ProductExpirationFormState(
      isLoading: false,
      selectedAccountCode: item.accountCode,
      selectedAccountName: item.accountName,
      selectedProductCode: item.productCode,
      selectedProductName: item.productName,
      expiryDate: item.expiryDate,
      alertDate: item.alertDate,
      description: item.description,
      editSeq: item.seq,
    );
  }

  /// 거래처 선택
  void selectAccount(String accountCode, String accountName) {
    state = state.copyWith(
      selectedAccountCode: accountCode,
      selectedAccountName: accountName,
    );
  }

  /// 제품 선택
  void selectProduct(String productCode, String productName) {
    state = state.copyWith(
      selectedProductCode: productCode,
      selectedProductName: productName,
    );
  }

  /// 소비기한 변경 (알림일도 자동 연동: 소비기한 - 1일)
  void updateExpiryDate(DateTime date) {
    final alertDate = date.subtract(const Duration(days: 1));
    state = state.copyWith(
      expiryDate: date,
      alertDate: alertDate,
    );
  }

  /// 알림일 변경
  void updateAlertDate(DateTime date) {
    state = state.copyWith(alertDate: date);
  }

  /// 설명 변경
  void updateDescription(String description) {
    state = state.copyWith(description: description);
  }

  /// 소비기한 등록
  Future<void> register() async {
    if (!state.isValid) return;

    state = state.toLoading();

    try {
      final form = ProductExpirationRegisterForm(
        accountCode: state.selectedAccountCode!,
        accountName: state.selectedAccountName!,
        productCode: state.selectedProductCode!,
        productName: state.selectedProductName!,
        expiryDate: state.expiryDate,
        alertDate: state.alertDate,
        description: state.description,
      );

      await _registerProductExpiration.call(form);

      state = state.copyWith(isLoading: false, isSaved: true);
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 소비기한 수정
  Future<void> update() async {
    if (state.editSeq == null) return;

    state = state.toLoading();

    try {
      final form = ProductExpirationUpdateForm(
        expiryDate: state.expiryDate,
        alertDate: state.alertDate,
        description: state.description,
      );

      await _updateProductExpiration.call(state.editSeq!, form);

      state = state.copyWith(isLoading: false, isSaved: true);
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 소비기한 단건 삭제 (수정 화면에서)
  Future<void> delete() async {
    if (state.editSeq == null) return;

    state = state.toLoading();

    try {
      await _deleteProductExpiration.call(state.editSeq!);

      state = state.copyWith(isLoading: false, isDeleted: true);
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(clearError: true);
  }
}

// ============================================
// 3. StateNotifier Provider Definition
// ============================================

/// ProductExpirationForm StateNotifier Provider
final productExpirationFormProvider =
    StateNotifierProvider<ProductExpirationFormNotifier, ProductExpirationFormState>((ref) {
  final registerUseCase = ref.watch(registerProductExpirationUseCaseProvider);
  final updateUseCase = ref.watch(updateProductExpirationUseCaseProvider);
  final deleteUseCase = ref.watch(deleteProductExpirationUseCaseProvider);

  return ProductExpirationFormNotifier(
    registerProductExpiration: registerUseCase,
    updateProductExpiration: updateUseCase,
    deleteProductExpiration: deleteUseCase,
  );
});
