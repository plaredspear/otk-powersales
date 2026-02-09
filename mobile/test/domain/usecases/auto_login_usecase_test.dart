import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/auth_token.dart';
import 'package:mobile/domain/entities/user.dart';
import 'package:mobile/domain/repositories/auth_repository.dart';
import 'package:mobile/domain/usecases/auto_login_usecase.dart';

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
  group('AutoLoginUseCase', () {
    late AutoLoginUseCase autoLoginUseCase;
    late MockAuthRepository mockRepository;

    setUp(() {
      mockRepository = MockAuthRepository();
      autoLoginUseCase = AutoLoginUseCase(mockRepository);
    });

    test('유효한 refreshToken으로 AuthToken 반환', () async {
      // Arrange
      const testRefreshToken = 'mock_refresh_token_123';
      const expectedToken = AuthToken(
        accessToken: 'new_access_token',
        refreshToken: testRefreshToken,
        expiresIn: 3600,
      );
      mockRepository.refreshResult = expectedToken;

      // Act
      final result = await autoLoginUseCase.call(
        refreshToken: testRefreshToken,
      );

      // Assert
      expect(result, equals(expectedToken));
      expect(result.accessToken, equals('new_access_token'));
      expect(result.refreshToken, equals(testRefreshToken));
      expect(mockRepository.lastRefreshToken, equals(testRefreshToken));
    });

    test('빈 refreshToken이면 ArgumentError 발생', () async {
      // Act & Assert
      expect(
        () => autoLoginUseCase.call(refreshToken: ''),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            'Refresh Token이 필요합니다',
          ),
        ),
      );
    });

    test('Repository 예외가 전파되는지', () async {
      // Arrange
      mockRepository.exceptionToThrow = Exception('세션이 만료되었습니다');

      // Act & Assert
      expect(
        () => autoLoginUseCase.call(refreshToken: 'invalid_token'),
        throwsA(
          isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('세션이 만료되었습니다'),
          ),
        ),
      );
    });

    test('Repository에 올바른 파라미터가 전달되는지 확인', () async {
      // Arrange
      const testRefreshToken = 'test_refresh_token_xyz';
      const expectedToken = AuthToken(
        accessToken: 'new_access_token',
        refreshToken: testRefreshToken,
        expiresIn: 3600,
      );
      mockRepository.refreshResult = expectedToken;

      // Act
      await autoLoginUseCase.call(refreshToken: testRefreshToken);

      // Assert
      expect(mockRepository.lastRefreshToken, equals(testRefreshToken));
    });
  });
}
