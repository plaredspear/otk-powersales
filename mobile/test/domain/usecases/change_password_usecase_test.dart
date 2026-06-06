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

    test('강제 변경: currentPassword null 허용', () async {
      // 강제 변경 시 currentPassword 미전달 — Repository 호출 시 null 로 전달
      await changePasswordUseCase.call(newPassword: 'newpass1234');
      expect(mockRepository.lastCurrentPassword, isNull);
      expect(mockRepository.lastNewPassword, equals('newpass1234'));
    });

    test('새 비밀번호 3글자 → ArgumentError (길이 위반)', () async {
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'oldpass1234',
          newPassword: 'abc',
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            contains('4자 이상 32자 이하'),
          ),
        ),
      );
    });

    test('새 비밀번호 33자 → ArgumentError (길이 초과)', () async {
      // 33자 + 반복 없음 → 길이 위반
      final tooLong = List.generate(33, (i) => String.fromCharCode(0x61 + (i % 26))).join();
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'oldpass1234',
          newPassword: tooLong,
        ),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('새 비밀번호 "1111" → ArgumentError (4연속 위반)', () async {
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'oldpass1234',
          newPassword: '1111',
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            contains('4번 연속'),
          ),
        ),
      );
    });

    test('새 비밀번호 "aaaa" → ArgumentError (4연속 위반)', () async {
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'oldpass1234',
          newPassword: 'aaaa',
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            contains('4번 연속'),
          ),
        ),
      );
    });

    test('새 비밀번호 "1234" (임시 비번 동일) → ArgumentError', () async {
      expect(
        () => changePasswordUseCase.call(
          currentPassword: 'oldpass1234',
          newPassword: '1234',
        ),
        throwsA(
          isA<ArgumentError>().having(
            (e) => e.message,
            'message',
            contains('임시 비밀번호'),
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
          currentPassword: 'wrongPassword',
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
