import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../data/datasources/claim_api_datasource.dart';
import '../../data/repositories/claim_repository_impl.dart';
import '../../data/datasources/claim_remote_datasource.dart';
import '../../domain/entities/claim_code.dart';
import '../../domain/entities/claim_draft.dart';
import '../../domain/entities/claim_form.dart';
import '../../domain/repositories/claim_repository.dart';
import '../../domain/usecases/get_claim_form_data_usecase.dart';
import '../../domain/usecases/register_claim_usecase.dart';
import 'claim_register_state.dart';

// ============================================
// 1. Dependency Providers (실 API)
// ============================================

/// ClaimRemoteDataSource Provider (실 API — ClaimController/ClaimQueryController)
final claimRemoteDataSourceProvider = Provider<ClaimRemoteDataSource>((ref) {
  return ClaimApiDataSource(ref.watch(dioProvider));
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
  final ClaimRepository _claimRepository;

  ClaimRegisterNotifier({
    required RegisterClaimUseCase registerClaimUseCase,
    required GetClaimFormDataUseCase getClaimFormDataUseCase,
    required ClaimRepository claimRepository,
  })  : _registerClaimUseCase = registerClaimUseCase,
        _getClaimFormDataUseCase = getClaimFormDataUseCase,
        _claimRepository = claimRepository,
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
  void selectAccount(int accountId, String accountName) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(
        accountId: accountId,
        accountName: accountName,
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
  void selectCategory(String categoryId, String categoryName) {
    final currentForm = state.form ?? _createInitialForm();
    state = state.copyWith(
      form: currentForm.copyWith(
        categoryId: categoryId,
        categoryName: categoryName,
        subcategoryId: '', // 종류2 초기화
        subcategoryName: '',
      ),
    );
  }

  /// 클레임 종류2 선택
  void selectSubcategory(String subcategoryId, String subcategoryName) {
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
  // 임시저장 (draft)
  // ──────────────────────────────────────────────────────────────────

  /// 임시저장 — 현재 폼 상태를 검증 없이 서버에 upsert.
  Future<bool> saveDraft() async {
    try {
      await _claimRepository.saveDraft(state.form);
      return true;
    } catch (e) {
      state = state.toError(e.toString());
      return false;
    }
  }

  /// 임시저장 조회 — 진입 시 이어쓰기 여부를 묻기 위해 호출. 없으면 null.
  Future<ClaimDraft?> loadDraft() async {
    try {
      return await _claimRepository.getDraft();
    } catch (_) {
      return null;
    }
  }

  /// 임시저장 폐기.
  Future<void> discardDraft() async {
    try {
      await _claimRepository.deleteDraft();
    } catch (_) {
      // 폐기 실패는 무시 (다음 저장/등록 시 덮어쓰기/삭제됨)
    }
  }

  /// 임시저장 내용을 폼에 반영(이어쓰기). 이름은 form-data 로 해석한다.
  void applyDraft(ClaimDraft draft) {
    final base = _createInitialForm();

    final categoryId = draft.claimType1 ?? '';
    final subcategoryId = draft.claimType2 ?? '';

    final form = ClaimRegisterForm(
      accountId: draft.accountId ?? 0,
      accountName: draft.accountName ?? '',
      productCode: draft.productCode ?? '',
      productName: draft.productName ?? '',
      dateType: draft.dateType != null
          ? ClaimDateType.fromJson(draft.dateType!)
          : base.dateType,
      date: _parseDraftDate(draft.date) ?? base.date,
      categoryId: categoryId,
      categoryName: _resolveCategoryName(categoryId),
      subcategoryId: subcategoryId,
      subcategoryName: _resolveSubcategoryName(categoryId, subcategoryId),
      defectDescription: draft.defectDescription ?? '',
      defectQuantity: draft.defectQuantity ?? 0,
      defectPhoto: draft.defectPhoto ?? File(''),
      labelPhoto: draft.labelPhoto ?? File(''),
      purchaseAmount: draft.purchaseAmount,
      purchaseMethodCode: draft.purchaseMethodCode,
      purchaseMethodName: _resolvePurchaseMethodName(draft.purchaseMethodCode),
      receiptPhoto: draft.receiptPhoto,
      requestTypeCode: draft.requestTypeCode,
      requestTypeName: _resolveRequestTypeName(draft.requestTypeCode),
    );

    state = state.copyWith(form: form, clearError: true);
  }

  DateTime? _parseDraftDate(String? raw) {
    if (raw == null || raw.isEmpty) return null;
    return DateTime.tryParse(raw);
  }

  /// 종류1 코드 → 이름 (form-data 기준, 미해석 시 코드 그대로 사용해 비어있지 않게 한다)
  String _resolveCategoryName(String categoryId) {
    if (categoryId.isEmpty) return '';
    final categories = state.formData?.categories ?? [];
    for (final c in categories) {
      if (c.id == categoryId) return c.name;
    }
    return categoryId;
  }

  String _resolveSubcategoryName(String categoryId, String subcategoryId) {
    if (subcategoryId.isEmpty) return '';
    final categories = state.formData?.categories ?? [];
    for (final c in categories) {
      if (c.id != categoryId) continue;
      for (final s in c.subcategories) {
        if (s.id == subcategoryId) return s.name;
      }
    }
    return subcategoryId;
  }

  String? _resolvePurchaseMethodName(String? code) {
    if (code == null || code.isEmpty) return null;
    final methods = state.formData?.purchaseMethods ?? [];
    for (final m in methods) {
      if (m.code == code) return m.name;
    }
    return code;
  }

  String? _resolveRequestTypeName(String? code) {
    if (code == null || code.isEmpty) return null;
    final firstCode = code.split(';').first.trim();
    final types = state.formData?.requestTypes ?? [];
    for (final t in types) {
      if (t.code == firstCode) return t.name;
    }
    return firstCode;
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
      accountId: 0,
      accountName: '',
      productCode: '',
      productName: '',
      dateType: ClaimDateType.expiryDate,
      date: DateTime.now(),
      categoryId: '',
      categoryName: '',
      subcategoryId: '',
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
    claimRepository: ref.watch(claimRepositoryProvider),
  );
});
