import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/auth_token.dart';
import 'package:mobile/domain/entities/password_validation.dart';
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
    test('4글자 이상이고 반복 아닌 비밀번호는 유효', () {
      final validation = PasswordValidation.fromPassword('1234');

      expect(validation.isLengthValid, true);
      expect(validation.isNotRepeating, true); // 서로 다른 문자
      expect(validation.isValid, true);
    });

    test('동일 문자 반복 비밀번호는 무효', () {
      final validation = PasswordValidation.fromPassword('1111');

      expect(validation.isLengthValid, true);
      expect(validation.isNotRepeating, false); // 모든 문자가 같음
      expect(validation.isValid, false);
    });

    test('반복 문자가 아닌 비밀번호는 유효', () {
      final validation = PasswordValidation.fromPassword('abcd');

      expect(validation.isLengthValid, true);
      expect(validation.isNotRepeating, true);
      expect(validation.isValid, true);
    });

    test('3글자 이하 비밀번호는 길이 무효', () {
      final validation = PasswordValidation.fromPassword('123');

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
        currentPassword: 'old123',
        newPassword: 'new456',
      );

      expect(mockAuthRepository.lastCurrentPassword, 'old123');
      expect(mockAuthRepository.lastNewPassword, 'new456');
    });

    test('changePassword 유효성 검증 실패 시 ArgumentError 전파', () async {
      final notifier = PasswordChangeNotifier(
        changePasswordUseCase: ChangePasswordUseCase(mockAuthRepository),
      );

      // 새 비밀번호가 3글자 이하 -> ArgumentError
      expect(
        () => notifier.changePassword(
          currentPassword: 'old1',
          newPassword: 'new', // 3글자
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
          currentPassword: 'old123',
          newPassword: 'new456',
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
  Future<void> changePassword(String currentPassword, String newPassword) async {
    if (shouldThrowError) {
      throw Exception('Network error');
    }
    lastCurrentPassword = currentPassword;
    lastNewPassword = newPassword;
  }
}

/// Mock Auth Repository
class MockAuthRepository implements AuthRepository {
  bool shouldThrowError = false;
  String? lastCurrentPassword;
  String? lastNewPassword;

  @override
  Future<void> changePassword(String currentPassword, String newPassword) async {
    if (shouldThrowError) {
      throw Exception('Network error');
    }
    lastCurrentPassword = currentPassword;
    lastNewPassword = newPassword;
  }

  @override
  Future<LoginResult> login(String employeeId, String password) {
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
  Future<void> recordGpsConsent() {
    throw UnimplementedError();
  }
}
