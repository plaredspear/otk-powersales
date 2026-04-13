import '../../domain/entities/claim_detail.dart';

/// 클레임 상세 상태
class ClaimDetailState {
  final bool isLoading;
  final String? errorMessage;
  final ClaimDetail? detail;

  const ClaimDetailState({
    this.isLoading = false,
    this.errorMessage,
    this.detail,
  });

  ClaimDetailState toLoading() => copyWith(isLoading: true, clearErrorMessage: true);

  ClaimDetailState toError(String msg) =>
      copyWith(isLoading: false, errorMessage: msg);

  ClaimDetailState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearErrorMessage = false,
    ClaimDetail? detail,
  }) {
    return ClaimDetailState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearErrorMessage ? null : (errorMessage ?? this.errorMessage),
      detail: detail ?? this.detail,
    );
  }
}
