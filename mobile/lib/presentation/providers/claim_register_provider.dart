import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/claim_repository_impl.dart';
import '../../data/datasources/claim_remote_datasource.dart';
import '../../domain/entities/claim_code.dart';
import '../../domain/entities/claim_form.dart';
import '../../domain/repositories/claim_repository.dart';
import '../../domain/usecases/get_claim_form_data_usecase.dart';
import '../../domain/usecases/register_claim_usecase.dart';
import 'claim_register_state.dart';

// ============================================
// 1. Dependency Providers (Mock for now)
// ============================================

/// ClaimRemoteDataSource Provider (Mock)
final claimRemoteDataSourceProvider = Provider<ClaimRemoteDataSource>((ref) {
  // TODO: Replace with real implementation when backend is ready
  throw UnimplementedError('ClaimRemoteDataSource not implemented yet');
});

/// ClaimRepository Provider
final claimRepositoryProvider = Provider<ClaimRepository>((ref) {
  final dataSource = ref.watch(claimRemoteDataSourceProvider);
  return ClaimRepositoryImpl(dataSource);
});

/// RegisterClaimUseCase Provider
final registerClaimUseCaseProvider = Provider<RegisterClaimUseCase>((ref) {
  final repository = ref.watch(claimRepositoryProvider);
  return RegisterClaimUseCase(repository);
});

/// GetClaimFormDataUseCase Provider
final getClaimFormDataUseCaseProvider =
    Provider<GetClaimFormDataUseCase>((ref) {
  final repository = ref.watch(claimRepositoryProvider);
  return GetClaimFormDataUseCase(repository);
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

/// 클레임 등록 상태 관리 Notifier
class ClaimRegisterNotifier extends StateNotifier<ClaimRegisterState> {
  final RegisterClaimUseCase _registerClaimUseCase;
  final GetClaimFormDataUseCase _getClaimFormDataUseCase;

  ClaimRegisterNotifier({
    required RegisterClaimUseCase registerClaimUseCase,
    required GetClaimFormDataUseCase getClaimFormDataUseCase,
  })  : _registerClaimUseCase = registerClaimUseCase,
        _getClaimFormDataUseCase = getClaimFormDataUseCase,
        super(ClaimRegisterState.initial());

  // ──────────────────────────────────────────────────────────────────
  // 폼 초기화 데이터 로드
  // ──────────────────────────────────────────────────────────────────

  /// 폼 초기화 데이터 로드 (categories, purchaseMethods, requestTypes)
  Future<void> loadFormData() async {
    state = state.toLoading();

    try {
      final formData = await _getClaimFormDataUseCase();

      state = state.copyWith(
        formData: formData,
        loading: false,
      );
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  // ──────────────────────────────────────────────────────────────────
  // 폼 필드 업데이트
  // ──────────────────────────────────────────────────────────────────

  /// 거래처 선택
  void selectStore(int storeId, String storeName) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(
        storeId: storeId,
        storeName: storeName,
      ),
    );
  }

  /// 제품 선택
  void selectProduct(String productCode, String productName) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(
        productCode: productCode,
        productName: productName,
      ),
    );
  }

  /// 기한 종류 선택
  void selectDateType(ClaimDateType dateType) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(dateType: dateType),
    );
  }

  /// 기한 날짜 선택
  void selectDate(DateTime date) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(date: date),
    );
  }

  /// 클레임 종류1 선택 (종류2 초기화)
  void selectCategory(int categoryId, String categoryName) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(
        categoryId: categoryId,
        categoryName: categoryName,
        subcategoryId: 0, // 종류2 초기화
        subcategoryName: '',
      ),
    );
  }

  /// 클레임 종류2 선택
  void selectSubcategory(int subcategoryId, String subcategoryName) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(
        subcategoryId: subcategoryId,
        subcategoryName: subcategoryName,
      ),
    );
  }

  /// 불량 내역 입력
  void updateDefectDescription(String description) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(defectDescription: description),
    );
  }

  /// 불량 수량 입력
  void updateDefectQuantity(int quantity) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(defectQuantity: quantity),
    );
  }

  /// 불량 사진 첨부
  void attachDefectPhoto(File photo) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(defectPhoto: photo),
    );
  }

  /// 불량 사진 삭제
  void removeDefectPhoto() {
    final currentForm = state.form;
    if (currentForm == null) return;

    // File을 빈 경로로 설정
    state = state.copyWith(
      form: currentForm.copyWith(defectPhoto: File('')),
    );
  }

  /// 일부인 사진 첨부
  void attachLabelPhoto(File photo) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(labelPhoto: photo),
    );
  }

  /// 일부인 사진 삭제
  void removeLabelPhoto() {
    final currentForm = state.form;
    if (currentForm == null) return;

    state = state.copyWith(
      form: currentForm.copyWith(labelPhoto: File('')),
    );
  }

  /// 구매 금액 입력
  void updatePurchaseAmount(int? amount) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(purchaseAmount: amount),
    );
  }

  /// 구매 방법 선택
  void selectPurchaseMethod(String? code, String? name) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(
        purchaseMethodCode: code,
        purchaseMethodName: name,
      ),
    );
  }

  /// 구매 영수증 사진 첨부
  void attachReceiptPhoto(File? photo) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(receiptPhoto: photo),
    );
  }

  /// 구매 영수증 사진 삭제
  void removeReceiptPhoto() {
    final currentForm = state.form;
    if (currentForm == null) return;

    state = state.copyWith(
      form: currentForm.copyWith(receiptPhoto: null),
    );
  }

  /// 요청사항 선택
  void selectRequestType(String? code, String? name) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(
        requestTypeCode: code,
        requestTypeName: name,
      ),
    );
  }

  // ──────────────────────────────────────────────────────────────────
  // 클레임 등록
  // ──────────────────────────────────────────────────────────────────

  /// 클레임 등록
  Future<bool> registerClaim() async {
    final form = state.form;
    if (form == null) {
      state = state.toError('폼 데이터가 없습니다');
      return false;
    }

    // 유효성 검증
    final errors = form.validate();
    if (errors.isNotEmpty) {
      state = state.toError(errors.first);
      return false;
    }

    state = state.toLoading();

    try {
      await _registerClaimUseCase(form);

      state = state.copyWith(loading: false, clearForm: true);

      return true;
    } catch (e) {
      state = state.toError(e.toString());
      return false;
    }
  }

  // ──────────────────────────────────────────────────────────────────
  // 유틸리티
  // ──────────────────────────────────────────────────────────────────

  /// 폼 초기화
  void clearForm() {
    state = state.copyWith(clearForm: true, clearError: true);
  }

  /// 초기 폼 생성 (빈 값)
  ClaimRegisterForm _createInitialForm() {
    return ClaimRegisterForm(
      storeId: 0,
      storeName: '',
      productCode: '',
      productName: '',
      dateType: ClaimDateType.expiryDate,
      date: DateTime.now(),
      categoryId: 0,
      categoryName: '',
      subcategoryId: 0,
      subcategoryName: '',
      defectDescription: '',
      defectQuantity: 0,
      defectPhoto: File(''),
      labelPhoto: File(''),
    );
  }
}

// ============================================
// 3. StateNotifierProvider
// ============================================

/// 클레임 등록 Provider
final claimRegisterProvider =
    StateNotifierProvider<ClaimRegisterNotifier, ClaimRegisterState>((ref) {
  return ClaimRegisterNotifier(
    registerClaimUseCase: ref.watch(registerClaimUseCaseProvider),
    getClaimFormDataUseCase: ref.watch(getClaimFormDataUseCaseProvider),
  );
});
