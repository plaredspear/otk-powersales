import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/auth_api_datasource.dart';
import '../../data/repositories/password_repository_impl.dart';
import '../../domain/entities/password_validation.dart';
import '../../domain/repositories/password_repository.dart';
import '../../domain/usecases/change_password_usecase.dart';
import '../../domain/usecases/verify_current_password_usecase.dart';
import 'auth_provider.dart'; // auth_provider에서 changePasswordUseCaseProvider 재사용

// --- Repository & DataSource Providers ---

/// Dio HTTP Client Provider (임시)
/// TODO: 실제 API URL로 변경
final dioProvider = Provider<Dio>((ref) {
  return Dio(BaseOptions(
    baseUrl: 'https://api.example.com',
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 10),
  ));
});

/// Password Repository Provider (비밀번호 검증용)
final passwordRepositoryProvider = Provider<PasswordRepository>((ref) {
  final dio = ref.watch(dioProvider);
  final authRemoteDataSource = AuthApiDataSource(dio);
  return PasswordRepositoryImpl(remoteDataSource: authRemoteDataSource);
});

/// VerifyCurrentPasswordUseCase Provider
final verifyCurrentPasswordUseCaseProvider =
    Provider<VerifyCurrentPasswordUseCase>((ref) {
  final repository = ref.watch(passwordRepositoryProvider);
  return VerifyCurrentPasswordUseCase(repository);
});

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

// --- Password Verification Notifier ---

/// 현재 비밀번호 검증 상태 Notifier
///
/// POST /api/v1/auth/verify-password API를 호출하여
/// 현재 비밀번호가 일치하는지 확인합니다.
class PasswordVerificationNotifier
    extends StateNotifier<AsyncValue<bool>> {
  final VerifyCurrentPasswordUseCase _verifyUseCase;

  PasswordVerificationNotifier({
    required VerifyCurrentPasswordUseCase verifyUseCase,
  })  : _verifyUseCase = verifyUseCase,
        super(const AsyncValue.data(false));

  /// 현재 비밀번호 검증
  ///
  /// [currentPassword]: 확인할 현재 비밀번호
  ///
  /// Returns: true (일치), false (불일치)
  Future<bool> verify(String currentPassword) async {
    state = const AsyncValue.loading();

    try {
      final isValid = await _verifyUseCase(currentPassword);
      state = AsyncValue.data(isValid);
      return isValid;
    } catch (error, stackTrace) {
      state = AsyncValue.error(error, stackTrace);
      return false;
    }
  }

  /// 상태 초기화
  void reset() {
    state = const AsyncValue.data(false);
  }
}

/// PasswordVerificationNotifier Provider
final passwordVerificationProvider = StateNotifierProvider<
    PasswordVerificationNotifier, AsyncValue<bool>>((ref) {
  final verifyUseCase = ref.watch(verifyCurrentPasswordUseCaseProvider);
  return PasswordVerificationNotifier(verifyUseCase: verifyUseCase);
});

// --- Password Change Notifier ---

/// 비밀번호 변경 상태 Notifier
///
/// POST /api/v1/auth/change-password API를 호출하여
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
