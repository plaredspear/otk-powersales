import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/auth_token.dart';
import 'package:mobile/domain/entities/user.dart';
import 'package:mobile/domain/repositories/auth_repository.dart';
import 'package:mobile/domain/usecases/login_usecase.dart';

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
  group('LoginUseCase', () {
    late LoginUseCase loginUseCase;
    late MockAuthRepository mockRepository;

    setUp(() {
      mockRepository = MockAuthRepository();
      loginUseCase = LoginUseCase(mockRepository);
    });

    test('정상 로그인 시 LoginResult 반환', () async {
      // Arrange
      const expectedUser = User(
        id: 1,
        employeeCode: '20010585',
        name: '홍길동',
        orgName: '부산1지점',
        role: 'USER',
      );
      const expectedToken = AuthToken(
        accessToken: 'mockAccessToken',
        refreshToken: 'mockRefreshToken',
        expiresIn: 3600,
      );
      const expectedLoginResult = LoginResult(
        user: expectedUser,
        token: expectedToken,
        passwordChangeRequired: false,
        requiresGpsConsent: false,
      );
      mockRepository.loginResult = expectedLoginResult;

      // Act
      final result = await loginUseCase.call(
        employeeCode: '20010585',
        password: 'test1234',
        rememberEmployeeNumber: false,
        autoLogin: false,
      );

      // Assert
      expect(result, equals(expectedLoginResult));
      expect(result.user, equals(expectedUser));
      expect(result.token, equals(expectedToken));
    });

    test('사번이 빈 문자열이면 ArgumentError 발생', () async {
      // Act & Assert
      expect(
        () => loginUseCase.call(
          employeeCode: '',
          password: 'test1234',
          rememberEmployeeNumber: false,
          autoLogin: false,
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            '사번을 입력해주세요',
          ),
        ),
      );
    });

    test('비밀번호가 빈 문자열이면 ArgumentError 발생', () async {
      // Act & Assert
      expect(
        () => loginUseCase.call(
          employeeCode: '20010585',
          password: '',
          rememberEmployeeNumber: false,
          autoLogin: false,
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            '비밀번호를 입력해주세요',
          ),
        ),
      );
    });

    test('Repository에 올바른 파라미터가 전달되는지 확인', () async {
      // Arrange
      const testEmployeeNumber = '20010585';
      const testPassword = 'test1234';
      const mockLoginResult = LoginResult(
        user: User(
          id: 1,
          employeeCode: testEmployeeNumber,
          name: '홍길동',
          orgName: '부산1지점',
          role: 'USER',
        ),
        token: AuthToken(
          accessToken: 'mockAccessToken',
          refreshToken: 'mockRefreshToken',
          expiresIn: 3600,
        ),
        passwordChangeRequired: false,
        requiresGpsConsent: false,
      );
      mockRepository.loginResult = mockLoginResult;

      // Act
      await loginUseCase.call(
        employeeCode: testEmployeeNumber,
        password: testPassword,
        rememberEmployeeNumber: true,
        autoLogin: true,
      );

      // Assert
      expect(mockRepository.lastLoginEmployeeNumber, equals(testEmployeeNumber));
      expect(mockRepository.lastLoginPassword, equals(testPassword));
    });

    test('Repository에서 Exception 발생 시 그대로 전파', () async {
      // Arrange
      mockRepository.exceptionToThrow = Exception('네트워크 에러');

      // Act & Assert
      expect(
        () => loginUseCase.call(
          employeeCode: '20010585',
          password: 'test1234',
          rememberEmployeeNumber: false,
          autoLogin: false,
        ),
        throwsA(
          isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('네트워크 에러'),
          ),
        ),
      );
    });
  });
}
