import 'package:dio/dio.dart';
import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
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

/// 유통기한 등록/수정 폼 상태 관리 Notifier
class ProductExpirationFormNotifier extends StateNotifier<ProductExpirationFormState> {
  final RegisterProductExpiration _registerProductExpiration;
  final UpdateProductExpiration _updateProductExpiration;
  final DeleteProductExpiration _deleteProductExpiration;
  final Dio _dio;

  ProductExpirationFormNotifier({
    required RegisterProductExpiration registerProductExpiration,
    required UpdateProductExpiration updateProductExpiration,
    required DeleteProductExpiration deleteProductExpiration,
    required Dio dio,
  })  : _registerProductExpiration = registerProductExpiration,
        _updateProductExpiration = updateProductExpiration,
        _deleteProductExpiration = deleteProductExpiration,
        _dio = dio,
        super(ProductExpirationFormState.initial());

  /// 등록 모드 초기화 (거래처 목록 로드)
  Future<void> initializeForRegister() async {
    try {
      final response = await _dio.get('/api/v1/mobile/accounts/my');
      final data = response.data['data'] as Map<String, dynamic>;
      final accountsList = data['accounts'] as List<dynamic>;
      final accountsMap = <String, String>{};
      for (final account in accountsList) {
        final accountMap = account as Map<String, dynamic>;
        final code = accountMap['accountCode'] as String;
        final name = accountMap['accountName'] as String;
        accountsMap[code] = name;
      }
      state = ProductExpirationFormState.initial().copyWith(accounts: accountsMap);
    } catch (e) {
      // 거래처 로딩 실패 시 빈 목록으로 초기화
      state = ProductExpirationFormState.initial();
    }
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

  /// 유통기한 변경 (알림일도 자동 연동: 유통기한 - 1일)
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

  /// 유통기한 등록
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

  /// 유통기한 수정
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

  /// 유통기한 단건 삭제 (수정 화면에서)
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
  final dio = ref.watch(dioProvider);

  return ProductExpirationFormNotifier(
    registerProductExpiration: registerUseCase,
    updateProductExpiration: updateUseCase,
    deleteProductExpiration: deleteUseCase,
    dio: dio,
  );
});
