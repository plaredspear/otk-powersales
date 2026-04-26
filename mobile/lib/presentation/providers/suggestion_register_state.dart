import 'dart:io';

import '../../domain/entities/suggestion_form.dart';

/// 제안하기 등록 화면 상태
class SuggestionRegisterState {
  /// 로딩 상태
  final bool isLoading;

  /// 제출 중 상태
  final bool isSubmitting;

  /// 에러 메시지
  final String? errorMessage;

  /// 성공 메시지
  final String? successMessage;

  /// 등록 폼 데이터
  final SuggestionRegisterForm form;

  /// 선택된 제품명 (로컬 표시용)
  final String? selectedProductName;

  /// 임시 저장 데이터 존재 여부
  final bool hasDraft;

  const SuggestionRegisterState({
    this.isLoading = false,
    this.isSubmitting = false,
    this.errorMessage,
    this.successMessage,
    required this.form,
    this.selectedProductName,
    this.hasDraft = false,
  });

  /// 초기 상태
  factory SuggestionRegisterState.initial() {
    return SuggestionRegisterState(
      form: SuggestionRegisterForm(
        category: SuggestionCategory.newProduct,
        title: '',
        content: '',
        photos: const [],
      ),
    );
  }

  /// 로딩 상태로 전환
  SuggestionRegisterState toLoading() {
    return copyWith(isLoading: true, errorMessage: null);
  }

  /// 제출 중 상태로 전환
  SuggestionRegisterState toSubmitting() {
    return copyWith(
      isSubmitting: true,
      errorMessage: null,
      successMessage: null,
    );
  }

  /// 에러 상태로 전환
  SuggestionRegisterState toError(String message) {
    return copyWith(
      isLoading: false,
      isSubmitting: false,
      errorMessage: message,
      successMessage: null,
    );
  }

  /// 성공 상태로 전환
  SuggestionRegisterState toSuccess(String message) {
    return copyWith(
      isLoading: false,
      isSubmitting: false,
      errorMessage: null,
      successMessage: message,
    );
  }

  /// 분류
  SuggestionCategory get category => form.category;

  /// 신제품 제안 여부
  bool get isNewProduct => category == SuggestionCategory.newProduct;

  /// 기존제품 제안 여부
  bool get isExistingProduct => category == SuggestionCategory.existingProduct;

  /// 제품 선택 여부
  bool get hasProduct => form.hasProduct;

  /// 사진 첨부 여부
  bool get hasPhotos => form.hasPhotos;

  /// 유효성 검증
  bool get isValid => form.isValid;

  /// copyWith
  SuggestionRegisterState copyWith({
    bool? isLoading,
    bool? isSubmitting,
    String? errorMessage,
    String? successMessage,
    SuggestionRegisterForm? form,
    String? selectedProductName,
    bool? hasDraft,
    bool clearErrorMessage = false,
    bool clearSuccessMessage = false,
    bool clearProductName = false,
  }) {
    return SuggestionRegisterState(
      isLoading: isLoading ?? this.isLoading,
      isSubmitting: isSubmitting ?? this.isSubmitting,
      errorMessage: clearErrorMessage
          ? null
          : (errorMessage ?? this.errorMessage),
      successMessage: clearSuccessMessage
          ? null
          : (successMessage ?? this.successMessage),
      form: form ?? this.form,
      selectedProductName: clearProductName
          ? null
          : (selectedProductName ?? this.selectedProductName),
      hasDraft: hasDraft ?? this.hasDraft,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SuggestionRegisterState &&
        other.isLoading == isLoading &&
        other.isSubmitting == isSubmitting &&
        other.errorMessage == errorMessage &&
        other.successMessage == successMessage &&
        other.form == form &&
        other.selectedProductName == selectedProductName &&
        other.hasDraft == hasDraft;
  }

  @override
  int get hashCode => Object.hash(
        isLoading,
        isSubmitting,
        errorMessage,
        successMessage,
        form,
        selectedProductName,
        hasDraft,
      );
}
