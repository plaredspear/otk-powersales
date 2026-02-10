import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/auth_token.dart';
import 'package:mobile/domain/repositories/auth_repository.dart';
import 'package:mobile/domain/usecases/change_password_usecase.dart';

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
  group('ChangePasswordUseCase', () {
    late ChangePasswordUseCase changePasswordUseCase;
    late MockAuthRepository mockRepository;

    setUp(() {
      mockRepository = MockAuthRepository();
      changePasswordUseCase = ChangePasswordUseCase(mockRepository);
    });

    test('정상 비밀번호 변경', () async {
      // Arrange
      const currentPassword = 'oldpass1234';
      const newPassword = 'newpass5678';

      // Act
      await changePasswordUseCase.call(
        currentPassword: currentPassword,
        newPassword: newPassword,
      );

      // Assert
      expect(mockRepository.lastCurrentPassword, equals(currentPassword));
      expect(mockRepository.lastNewPassword, equals(newPassword));
    });

    test('현재 비밀번호 빈 문자열 → 에러', () async {
      // Act & Assert
      expect(
        () => changePasswordUseCase.call(
          currentPassword: '',
          newPassword: 'newpass1234',
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            '현재 비밀번호를 입력해주세요',
          ),
        ),
      );
    });

    test('새 비밀번호 3글자 → 에러', () async {
      // Act & Assert
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'oldpass1234',
          newPassword: 'abc',
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            '비밀번호는 4글자 이상이어야 합니다',
          ),
        ),
      );
    });

    test('새 비밀번호 "1111" → 에러', () async {
      // Act & Assert
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'oldpass1234',
          newPassword: '1111',
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            '동일한 문자의 반복은 사용할 수 없습니다',
          ),
        ),
      );
    });

    test('새 비밀번호 "aaaa" → 에러', () async {
      // Act & Assert
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'oldpass1234',
          newPassword: 'aaaa',
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            '동일한 문자의 반복은 사용할 수 없습니다',
          ),
        ),
      );
    });

    test('정상 4글자 비밀번호 "ab12" → 성공', () async {
      // Arrange
      const currentPassword = 'oldpass1234';
      const newPassword = 'ab12';

      // Act
      await changePasswordUseCase.call(
        currentPassword: currentPassword,
        newPassword: newPassword,
      );

      // Assert
      expect(mockRepository.lastCurrentPassword, equals(currentPassword));
      expect(mockRepository.lastNewPassword, equals(newPassword));
    });

    test('Repository 예외 전파', () async {
      // Arrange
      mockRepository.exceptionToThrow = Exception('현재 비밀번호가 올바르지 않습니다');

      // Act & Assert
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'wrong_password',
          newPassword: 'newpass5678',
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
      const currentPassword = 'current123';
      const newPassword = 'new4567';

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
