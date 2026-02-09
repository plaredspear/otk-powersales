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
        employeeId: '20010585',
        name: '홍길동',
        department: '영업1팀',
        branchName: '부산1지점',
        role: 'USER',
      );
      const expectedToken = AuthToken(
        accessToken: 'mock_access_token',
        refreshToken: 'mock_refresh_token',
        expiresIn: 3600,
      );
      const expectedLoginResult = LoginResult(
        user: expectedUser,
        token: expectedToken,
        requiresPasswordChange: false,
        requiresGpsConsent: false,
      );
      mockRepository.loginResult = expectedLoginResult;

      // Act
      final result = await loginUseCase.call(
        employeeId: '20010585',
        password: 'test1234',
        rememberEmployeeId: false,
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
          employeeId: '',
          password: 'test1234',
          rememberEmployeeId: false,
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
          employeeId: '20010585',
          password: '',
          rememberEmployeeId: false,
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
      const testEmployeeId = '20010585';
      const testPassword = 'test1234';
      const mockLoginResult = LoginResult(
        user: User(
          id: 1,
          employeeId: testEmployeeId,
          name: '홍길동',
          department: '영업1팀',
          branchName: '부산1지점',
          role: 'USER',
        ),
        token: AuthToken(
          accessToken: 'mock_access_token',
          refreshToken: 'mock_refresh_token',
          expiresIn: 3600,
        ),
        requiresPasswordChange: false,
        requiresGpsConsent: false,
      );
      mockRepository.loginResult = mockLoginResult;

      // Act
      await loginUseCase.call(
        employeeId: testEmployeeId,
        password: testPassword,
        rememberEmployeeId: true,
        autoLogin: true,
      );

      // Assert
      expect(mockRepository.lastLoginEmployeeId, equals(testEmployeeId));
      expect(mockRepository.lastLoginPassword, equals(testPassword));
    });

    test('Repository에서 Exception 발생 시 그대로 전파', () async {
      // Arrange
      mockRepository.exceptionToThrow = Exception('네트워크 에러');

      // Act & Assert
      expect(
        () => loginUseCase.call(
          employeeId: '20010585',
          password: 'test1234',
          rememberEmployeeId: false,
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
