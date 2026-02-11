import '../../domain/entities/claim_form.dart';
import '../../domain/entities/claim_form_data.dart';

/// 클레임 등록 상태
class ClaimRegisterState {
  /// 등록 폼 데이터
  final ClaimRegisterForm? form;

  /// 폼 초기화 데이터 (categories, purchaseMethods, requestTypes)
  final ClaimFormData? formData;

  /// 로딩 상태
  final bool loading;

  /// 에러 메시지
  final String? error;

  const ClaimRegisterState({
    this.form,
    this.formData,
    this.loading = false,
    this.error,
  });

  /// 초기 상태
  factory ClaimRegisterState.initial() => const ClaimRegisterState();

  /// 로딩 상태로 변경
  ClaimRegisterState toLoading() {
    return copyWith(loading: true, clearError: true);
  }

  /// 에러 상태로 변경
  ClaimRegisterState toError(String message) {
    return copyWith(loading: false, error: message);
  }

  /// copyWith 메서드
  ClaimRegisterState copyWith({
    ClaimRegisterForm? form,
    bool clearForm = false,
    ClaimFormData? formData,
    bool? loading,
    String? error,
    bool clearError = false,
  }) {
    return ClaimRegisterState(
      form: clearForm ? null : (form ?? this.form),
      formData: formData ?? this.formData,
      loading: loading ?? this.loading,
      error: clearError ? null : (error ?? this.error),
    );
  }
}
