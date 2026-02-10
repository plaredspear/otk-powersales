import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/auth_token.dart';
import 'package:mobile/domain/repositories/auth_repository.dart';
import 'package:mobile/domain/usecases/logout_usecase.dart';

/// Mock AuthRepository for testing
class MockAuthRepository implements AuthRepository {
  LoginResult? loginResult;
  AuthToken? refreshResult;
  Exception? exceptionToThrow;

  String? lastLoginEmployeeId;
  String? lastLoginPassword;
  String? lastRefreshToken;
  String? lastCurrentPassword;
  String? lastNewPassword;
  bool logoutCalled = false;
  bool recordGpsConsentCalled = false;

  @override
  Future<LoginResult> login(String employeeId, String password) async {
    lastLoginEmployeeId = employeeId;
    lastLoginPassword = password;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return loginResult!;
  }

  @override
  Future<AuthToken> refreshToken(String refreshToken) async {
    lastRefreshToken = refreshToken;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return refreshResult!;
  }

  @override
  Future<void> changePassword(String currentPassword, String newPassword) async {
    lastCurrentPassword = currentPassword;
    lastNewPassword = newPassword;
    if (exceptionToThrow != null) throw exceptionToThrow!;
  }

  @override
  Future<void> logout() async {
    logoutCalled = true;
    if (exceptionToThrow != null) throw exceptionToThrow!;
  }

  @override
  Future<void> recordGpsConsent() async {
    recordGpsConsentCalled = true;
    if (exceptionToThrow != null) throw exceptionToThrow!;
  }
}

void main() {
  group('LogoutUseCase', () {
    late LogoutUseCase logoutUseCase;
    late MockAuthRepository mockRepository;

    setUp(() {
      mockRepository = MockAuthRepository();
      logoutUseCase = LogoutUseCase(mockRepository);
    });

    test('정상 로그아웃 호출', () async {
      // Act
      await logoutUseCase.call();

      // Assert
      expect(mockRepository.logoutCalled, isTrue);
    });

    test('Repository logout() 호출 확인', () async {
      // Arrange
      expect(mockRepository.logoutCalled, isFalse);

      // Act
      await logoutUseCase.call();

      // Assert
      expect(mockRepository.logoutCalled, isTrue);
    });

    test('Repository 예외 전파', () async {
      // Arrange
      mockRepository.exceptionToThrow = Exception('네트워크 에러');

      // Act & Assert
      expect(
        () => logoutUseCase.call(),
        throwsA(
          isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('네트워크 에러'),
          ),
        ),
      );
    });

    test('여러 번 호출해도 정상 동작', () async {
      // Act
      await logoutUseCase.call();
      await logoutUseCase.call();
      await logoutUseCase.call();

      // Assert
      expect(mockRepository.logoutCalled, isTrue);
    });
  });
}
