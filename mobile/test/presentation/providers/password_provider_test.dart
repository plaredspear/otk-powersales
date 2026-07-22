import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/auth_token.dart';
import 'package:mobile/domain/entities/password_validation.dart';
import 'package:mobile/domain/entities/user.dart';
import 'package:mobile/domain/repositories/auth_repository.dart';
import 'package:mobile/domain/usecases/change_password_usecase.dart';
import 'package:mobile/presentation/providers/password_provider.dart';

void main() {
  late MockAuthRepository mockAuthRepository;

  setUp(() {
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
