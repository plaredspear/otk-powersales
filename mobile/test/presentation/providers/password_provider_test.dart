import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/auth_token.dart';
import 'package:mobile/domain/entities/password_validation.dart';
import 'package:mobile/domain/entities/user.dart';
import 'package:mobile/domain/repositories/auth_repository.dart';
import 'package:mobile/domain/repositories/password_repository.dart';
import 'package:mobile/domain/usecases/change_password_usecase.dart';
import 'package:mobile/domain/usecases/verify_current_password_usecase.dart';
import 'package:mobile/presentation/providers/password_provider.dart';

void main() {
  late MockPasswordRepository mockPasswordRepository;
  late MockAuthRepository mockAuthRepository;

  setUp(() {
    mockPasswordRepository = MockPasswordRepository();
    mockAuthRepository = MockAuthRepository();
  });

  group('PasswordValidation (사용되는 로직)', () {
    test('"abcd123!" (8자, 3종) -> 모두 충족', () {
      final validation = PasswordValidation.fromPassword('abcd123!');

      expect(validation.isLengthValid, true);
      expect(validation.hasEnoughCharacterTypes, true);
      expect(validation.isValid, true);
    });

    test('소문자+숫자 8자 (2종) -> 종류 무효', () {
      final validation = PasswordValidation.fromPassword('abcd1234');

      expect(validation.isLengthValid, true);
      expect(validation.hasEnoughCharacterTypes, false);
      expect(validation.isValid, false);
    });

    test('"Abcd123!" (4종) -> 모두 충족', () {
      final validation = PasswordValidation.fromPassword('Abcd123!');

      expect(validation.isValid, true);
    });

    test('7글자 이하 -> 길이 무효', () {
      final validation = PasswordValidation.fromPassword('Abc12!x');

      expect(validation.isLengthValid, false);
      expect(validation.isValid, false);
    });
  });

  group('PasswordVerificationNotifier', () {
    test('초기 상태는 false', () {
      final notifier = PasswordVerificationNotifier(
        verifyUseCase: VerifyCurrentPasswordUseCase(mockPasswordRepository),
      );

      expect(notifier.state.value, false);
    });

    test('verify 성공 시 true 반환', () async {
      mockPasswordRepository.verifyResult = true;

      final notifier = PasswordVerificationNotifier(
        verifyUseCase: VerifyCurrentPasswordUseCase(mockPasswordRepository),
      );

      final result = await notifier.verify('correct123');

      expect(result, true);
      expect(notifier.state.value, true);
    });

    test('verify 실패 시 false 반환', () async {
      mockPasswordRepository.verifyResult = false;

      final notifier = PasswordVerificationNotifier(
        verifyUseCase: VerifyCurrentPasswordUseCase(mockPasswordRepository),
      );

      final result = await notifier.verify('wrong');

      expect(result, false);
      expect(notifier.state.value, false);
    });

    test('verify 에러 발생 시 false 반환', () async {
      mockPasswordRepository.shouldThrowError = true;

      final notifier = PasswordVerificationNotifier(
        verifyUseCase: VerifyCurrentPasswordUseCase(mockPasswordRepository),
      );

      final result = await notifier.verify('test');

      expect(result, false);
    });

    test('reset 호출 시 초기 상태로 복원', () async {
      mockPasswordRepository.verifyResult = true;

      final notifier = PasswordVerificationNotifier(
        verifyUseCase: VerifyCurrentPasswordUseCase(mockPasswordRepository),
      );

      await notifier.verify('correct123');
      expect(notifier.state.value, true);

      notifier.reset();

      expect(notifier.state.value, false);
    });
  });

  group('PasswordChangeNotifier', () {
    test('changePassword 성공', () async {
      final notifier = PasswordChangeNotifier(
        changePasswordUseCase: ChangePasswordUseCase(mockAuthRepository),
      );

      await notifier.changePassword(
        currentPassword: 'Old12345!',
        newPassword: 'New45678!',
      );

      expect(mockAuthRepository.lastCurrentPassword, 'Old12345!');
      expect(mockAuthRepository.lastNewPassword, 'New45678!');
    });

    test('changePassword 유효성 검증 실패 시 ArgumentError 전파', () async {
      final notifier = PasswordChangeNotifier(
        changePasswordUseCase: ChangePasswordUseCase(mockAuthRepository),
      );

      // 새 비밀번호가 8자 미만 -> ArgumentError
      expect(
        () => notifier.changePassword(
          currentPassword: 'Old12345!',
          newPassword: 'New1!', // 5글자
        ),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('changePassword 네트워크 에러 발생 시 Exception 전파', () async {
      mockAuthRepository.shouldThrowError = true;

      final notifier = PasswordChangeNotifier(
        changePasswordUseCase: ChangePasswordUseCase(mockAuthRepository),
      );

      // 유효한 비밀번호지만 네트워크 에러 -> Exception
      expect(
        () => notifier.changePassword(
          currentPassword: 'Old12345!',
          newPassword: 'New45678!',
        ),
        throwsA(isA<Exception>()),
      );
    });
  });
}

/// Mock Password Repository
class MockPasswordRepository implements PasswordRepository {
  bool verifyResult = true;
  bool shouldThrowError = false;
  String? lastCurrentPassword;
  String? lastNewPassword;

  @override
  Future<bool> verifyCurrentPassword(String currentPassword) async {
    if (shouldThrowError) {
      throw Exception('Network error');
    }
    lastCurrentPassword = currentPassword;
    return verifyResult;
  }

  @override
  Future<AuthToken> changePassword({required String currentPassword, required String newPassword}) async {
    if (shouldThrowError) {
      throw Exception('Network error');
    }
    lastCurrentPassword = currentPassword;
    lastNewPassword = newPassword;
    return const AuthToken(accessToken: "new-access", refreshToken: "new-refresh", expiresIn: 3600);
  }
}

/// Mock Auth Repository
class MockAuthRepository implements AuthRepository {
  bool shouldThrowError = false;
  String? lastCurrentPassword;
  String? lastNewPassword;

  @override
  Future<AuthToken> changePassword({String? currentPassword, required String newPassword}) async {
    if (shouldThrowError) {
      throw Exception('Network error');
    }
    lastCurrentPassword = currentPassword;
    lastNewPassword = newPassword;
    return const AuthToken(accessToken: "new-access", refreshToken: "new-refresh", expiresIn: 3600);
  }

  @override
  Future<LoginResult> login(String employeeCode, String password,
      {bool autoLogin = false}) {
    throw UnimplementedError();
  }

  @override
  Future<User> getMe() {
    throw UnimplementedError();
  }

  @override
  Future<AuthToken> refreshToken(String refreshToken) {
    throw UnimplementedError();
  }

  @override
  Future<void> logout() {
    throw UnimplementedError();
  }

  @override
  Future<GpsConsentTerms> getGpsConsentTerms() async {
    return const GpsConsentTerms(agreementNumber: 'AGR-001', contents: 'test');
  }

  @override
  Future<GpsConsentStatus> getGpsConsentStatus() async {
    return const GpsConsentStatus(requiresGpsConsent: false);
  }

  @override
  Future<GpsConsentRecordResult> recordGpsConsent({String? agreementNumber}) async {
    return const GpsConsentRecordResult(accessToken: 'mockToken', expiresIn: 3600);
  }
}
