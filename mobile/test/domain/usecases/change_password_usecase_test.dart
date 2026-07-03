import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/auth_token.dart';
import 'package:mobile/domain/entities/user.dart';
import 'package:mobile/domain/repositories/auth_repository.dart';
import 'package:mobile/domain/usecases/change_password_usecase.dart';

/// Mock AuthRepository for testing
class MockAuthRepository implements AuthRepository {
  LoginResult? loginResult;
  AuthToken? refreshResult;
  Exception? exceptionToThrow;

  String? lastLoginEmployeeNumber;
  String? lastLoginPassword;
  String? lastRefreshToken;
  String? lastCurrentPassword;
  String? lastNewPassword;
  bool logoutCalled = false;
  bool recordGpsConsentCalled = false;

  @override
  Future<LoginResult> login(String employeeCode, String password) async {
    lastLoginEmployeeNumber = employeeCode;
    lastLoginPassword = password;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return loginResult!;
  }

  @override
  Future<User> getMe() async => throw UnimplementedError();

  @override
  Future<AuthToken> refreshToken(String refreshToken) async {
    lastRefreshToken = refreshToken;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return refreshResult!;
  }

  @override
  Future<AuthToken> changePassword({String? currentPassword, required String newPassword}) async {
    lastCurrentPassword = currentPassword;
    lastNewPassword = newPassword;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return const AuthToken(accessToken: "new-access", refreshToken: "new-refresh", expiresIn: 3600);
  }

  @override
  Future<void> logout() async {
    logoutCalled = true;
    if (exceptionToThrow != null) throw exceptionToThrow!;
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
    recordGpsConsentCalled = true;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return const GpsConsentRecordResult(accessToken: 'mockToken', expiresIn: 3600);
  }
}

void main() {
  group('ChangePasswordUseCase', () {
    late ChangePasswordUseCase changePasswordUseCase;
    late MockAuthRepository mockRepository;

    setUp(() {
      mockRepository = MockAuthRepository();
      changePasswordUseCase = ChangePasswordUseCase(mockRepository);
    });

    test('정상 비밀번호 변경 (8자 + 3종)', () async {
      // Arrange
      const currentPassword = 'Oldpass12!';
      const newPassword = 'Newpass56!';

      // Act
      await changePasswordUseCase.call(
        currentPassword: currentPassword,
        newPassword: newPassword,
      );

      // Assert
      expect(mockRepository.lastCurrentPassword, equals(currentPassword));
      expect(mockRepository.lastNewPassword, equals(newPassword));
    });

    test('강제 변경: currentPassword null 허용', () async {
      // 강제 변경 시 currentPassword 미전달 — Repository 호출 시 null 로 전달
      await changePasswordUseCase.call(newPassword: 'Newpass12!');
      expect(mockRepository.lastCurrentPassword, isNull);
      expect(mockRepository.lastNewPassword, equals('Newpass12!'));
    });

    test('임시 비밀번호 "pwrs1234!" (9자, 3종) → 성공', () async {
      await changePasswordUseCase.call(
        currentPassword: 'Oldpass12!',
        newPassword: 'pwrs1234!',
      );
      expect(mockRepository.lastNewPassword, equals('pwrs1234!'));
    });

    test('새 비밀번호 7글자 → ArgumentError (길이 위반)', () async {
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'Oldpass12!',
          newPassword: 'Abc12!x',
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            contains('8자 이상'),
          ),
        ),
      );
    });

    test('새 비밀번호 소문자만 8자 (1종) → ArgumentError (종류 위반)', () async {
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'Oldpass12!',
          newPassword: 'abcdefgh',
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            contains('3종 이상'),
          ),
        ),
      );
    });

    test('새 비밀번호 소문자+숫자 8자 (2종) → ArgumentError (종류 위반)', () async {
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'Oldpass12!',
          newPassword: 'abcd1234',
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            contains('3종 이상'),
          ),
        ),
      );
    });

    test('동일 문자 반복이어도 8자 + 3종이면 성공 (반복금지 제거됨)', () async {
      const newPassword = 'aaaaA1!x';
      await changePasswordUseCase.call(
        currentPassword: 'Oldpass12!',
        newPassword: newPassword,
      );
      expect(mockRepository.lastNewPassword, equals(newPassword));
    });

    test('Repository 예외 전파', () async {
      // Arrange
      mockRepository.exceptionToThrow = Exception('현재 비밀번호가 올바르지 않습니다');

      // Act & Assert
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'wrongPassword',
          newPassword: 'Newpass56!',
        ),
        throwsA(
          isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('현재 비밀번호가 올바르지 않습니다'),
          ),
        ),
      );
    });

    test('Repository에 올바른 파라미터가 전달되는지 확인', () async {
      // Arrange
      const currentPassword = 'Current12!';
      const newPassword = 'Newpass45!';

      // Act
      await changePasswordUseCase.call(
        currentPassword: currentPassword,
        newPassword: newPassword,
      );

      // Assert
      expect(mockRepository.lastCurrentPassword, equals(currentPassword));
      expect(mockRepository.lastNewPassword, equals(newPassword));
    });
  });
}
