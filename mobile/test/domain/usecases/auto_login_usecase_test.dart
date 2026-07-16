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

  String? lastLoginEmployeeNumber;
  String? lastLoginPassword;
  String? lastRefreshToken;
  String? lastCurrentPassword;
  String? lastNewPassword;
  bool logoutCalled = false;
  bool recordGpsConsentCalled = false;

  @override
  Future<LoginResult> login(String employeeCode, String password,
      {bool autoLogin = false}) async {
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
  group('AutoLoginUseCase', () {
    late AutoLoginUseCase autoLoginUseCase;
    late MockAuthRepository mockRepository;

    setUp(() {
      mockRepository = MockAuthRepository();
      autoLoginUseCase = AutoLoginUseCase(mockRepository);
    });

    test('유효한 refreshToken으로 AuthToken 반환', () async {
      // Arrange
      const testRefreshToken = 'mockRefreshToken123';
      const expectedToken = AuthToken(
        accessToken: 'newAccessToken',
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
      expect(result.accessToken, equals('newAccessToken'));
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
        () => autoLoginUseCase.call(refreshToken: 'invalidToken'),
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
      const testRefreshToken = 'testRefreshTokenXyz';
      const expectedToken = AuthToken(
        accessToken: 'newAccessToken',
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
