import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/password_validation.dart';
import '../../domain/usecases/change_password_usecase.dart';
import 'auth_provider.dart'; // auth_provider에서 changePasswordUseCaseProvider 재사용

// --- Password Validation Provider ---

/// 비밀번호 유효성 검증 Provider
///
/// 입력된 비밀번호의 유효성을 실시간으로 검증합니다.
/// - 4글자 이상
/// - 동일 문자 반복 불가 (예: 1111, aaaa)
final passwordValidationProvider =
    Provider.family<PasswordValidation, String>((ref, password) {
  return PasswordValidation.fromPassword(password);
});

// --- Password Change Notifier ---

/// 비밀번호 변경 상태 Notifier
///
/// POST /api/v1/mobile/auth/change-password API를 호출하여
/// 비밀번호를 변경합니다.
class PasswordChangeNotifier extends StateNotifier<AsyncValue<void>> {
  final ChangePasswordUseCase _changePasswordUseCase;

  PasswordChangeNotifier({
    required ChangePasswordUseCase changePasswordUseCase,
  })  : _changePasswordUseCase = changePasswordUseCase,
        super(const AsyncValue.data(null));

  /// 비밀번호 변경
  ///
  /// [currentPassword]: 현재 비밀번호
  /// [newPassword]: 새 비밀번호
  ///
  /// Throws: Exception - 현재 비밀번호 불일치, 유효성 검증 실패 등
  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    state = const AsyncValue.loading();

    try {
      await _changePasswordUseCase(
        currentPassword: currentPassword,
        newPassword: newPassword,
      );
      state = const AsyncValue.data(null);
    } catch (error, stackTrace) {
      state = AsyncValue.error(error, stackTrace);
      rethrow;
    }
  }

  /// 상태 초기화
  void reset() {
    state = const AsyncValue.data(null);
  }
}

/// PasswordChangeNotifier Provider
///
/// auth_provider의 changePasswordUseCaseProvider를 재사용합니다.
final passwordChangeProvider =
    StateNotifierProvider<PasswordChangeNotifier, AsyncValue<void>>((ref) {
  final changePasswordUseCase = ref.watch(changePasswordUseCaseProvider); // from auth_provider
  return PasswordChangeNotifier(changePasswordUseCase: changePasswordUseCase);
});
