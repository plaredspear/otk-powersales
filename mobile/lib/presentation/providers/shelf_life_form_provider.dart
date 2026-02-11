import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/mock/my_store_mock_repository.dart';
import '../../domain/entities/shelf_life_form.dart';
import '../../domain/entities/shelf_life_item.dart';
import '../../domain/usecases/delete_shelf_life_usecase.dart';
import '../../domain/usecases/register_shelf_life_usecase.dart';
import '../../domain/usecases/update_shelf_life_usecase.dart';
import 'shelf_life_form_state.dart';
import 'shelf_life_list_provider.dart';

// ============================================
// 1. Dependency Providers
// ============================================

/// RegisterShelfLife UseCase Provider
final registerShelfLifeUseCaseProvider = Provider<RegisterShelfLife>((ref) {
  final repository = ref.watch(shelfLifeRepositoryProvider);
  return RegisterShelfLife(repository);
});

/// UpdateShelfLife UseCase Provider
final updateShelfLifeUseCaseProvider = Provider<UpdateShelfLife>((ref) {
  final repository = ref.watch(shelfLifeRepositoryProvider);
  return UpdateShelfLife(repository);
});

/// DeleteShelfLife UseCase Provider (단건 삭제)
final deleteShelfLifeUseCaseProvider = Provider<DeleteShelfLife>((ref) {
  final repository = ref.watch(shelfLifeRepositoryProvider);
  return DeleteShelfLife(repository);
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

/// 유통기한 등록/수정 폼 상태 관리 Notifier
class ShelfLifeFormNotifier extends StateNotifier<ShelfLifeFormState> {
  final RegisterShelfLife _registerShelfLife;
  final UpdateShelfLife _updateShelfLife;
  final DeleteShelfLife _deleteShelfLife;

  ShelfLifeFormNotifier({
    required RegisterShelfLife registerShelfLife,
    required UpdateShelfLife updateShelfLife,
    required DeleteShelfLife deleteShelfLife,
  })  : _registerShelfLife = registerShelfLife,
        _updateShelfLife = updateShelfLife,
        _deleteShelfLife = deleteShelfLife,
        super(ShelfLifeFormState.initial());

  /// 등록 모드 초기화 (거래처 목록 로드)
  Future<void> initializeForRegister() async {
    final mockStoreRepo = MyStoreMockRepository();
    final storeResult = await mockStoreRepo.getMyStores();
    final storesMap = <int, String>{};
    for (final store in storeResult.stores) {
      storesMap[store.storeId] = store.storeName;
    }
    state = ShelfLifeFormState.initial().copyWith(stores: storesMap);
  }

  /// 수정 모드 초기화 (기존 데이터 로드)
  void initializeForEdit(ShelfLifeItem item) {
    state = ShelfLifeFormState(
      isLoading: false,
      selectedStoreId: item.storeId,
      selectedStoreName: item.storeName,
      selectedProductCode: item.productCode,
      selectedProductName: item.productName,
      expiryDate: item.expiryDate,
      alertDate: item.alertDate,
      description: item.description,
      editId: item.id,
    );
  }

  /// 거래처 선택
  void selectStore(int storeId, String storeName) {
    state = state.copyWith(
      selectedStoreId: storeId,
      selectedStoreName: storeName,
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
      final form = ShelfLifeRegisterForm(
        storeId: state.selectedStoreId!,
        productCode: state.selectedProductCode!,
        expiryDate: state.expiryDate,
        alertDate: state.alertDate,
        description: state.description,
      );

      await _registerShelfLife.call(form);

      state = state.copyWith(isLoading: false, isSaved: true);
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 유통기한 수정
  Future<void> update() async {
    if (state.editId == null) return;

    state = state.toLoading();

    try {
      final form = ShelfLifeUpdateForm(
        expiryDate: state.expiryDate,
        alertDate: state.alertDate,
        description: state.description,
      );

      await _updateShelfLife.call(state.editId!, form);

      state = state.copyWith(isLoading: false, isSaved: true);
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 유통기한 단건 삭제 (수정 화면에서)
  Future<void> delete() async {
    if (state.editId == null) return;

    state = state.toLoading();

    try {
      await _deleteShelfLife.call(state.editId!);

      state = state.copyWith(isLoading: false, isDeleted: true);
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
}

// ============================================
// 3. StateNotifier Provider Definition
// ============================================

/// ShelfLifeForm StateNotifier Provider
final shelfLifeFormProvider =
    StateNotifierProvider<ShelfLifeFormNotifier, ShelfLifeFormState>((ref) {
  final registerUseCase = ref.watch(registerShelfLifeUseCaseProvider);
  final updateUseCase = ref.watch(updateShelfLifeUseCaseProvider);
  final deleteUseCase = ref.watch(deleteShelfLifeUseCaseProvider);

  return ShelfLifeFormNotifier(
    registerShelfLife: registerUseCase,
    updateShelfLife: updateUseCase,
    deleteShelfLife: deleteUseCase,
  );
});
