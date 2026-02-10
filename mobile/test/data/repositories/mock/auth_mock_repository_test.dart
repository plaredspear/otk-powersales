import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/auth_mock_repository.dart';

void main() {
  group('AuthMockRepository', () {
    late AuthMockRepository repository;

    setUp(() {
      // 각 테스트마다 새로운 repository 인스턴스 생성
      // 주의: static _mockAccounts가 공유되므로,
      // changePassword 테스트는 순서에 민감할 수 있음
      repository = AuthMockRepository();
    });

    group('login', () {
      test('login 성공 - 유효한 사번/비밀번호 → LoginResult with correct User and AuthToken', () async {
        // Arrange
        const employeeId = '20010585';
        const password = 'test1234';

        // Act
        final result = await repository.login(employeeId, password);

        // Assert
        expect(result.user.employeeId, equals(employeeId));
        expect(result.user.name, equals('홍길동'));
        expect(result.user.department, equals('영업1팀'));
        expect(result.user.branchName, equals('부산1지점'));
        expect(result.user.role, equals('USER'));
        expect(result.token.accessToken, isNotEmpty);
        expect(result.token.refreshToken, isNotEmpty);
        expect(result.token.expiresIn, equals(3600));
        expect(result.requiresPasswordChange, isFalse);
        expect(result.requiresGpsConsent, isFalse);
      });

      test('login 실패 - 잘못된 사번 → Exception 발생', () async {
        // Act & Assert
        expect(
          () => repository.login('99999999', 'test1234'),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('사번 또는 비밀번호가 올바르지 않습니다'),
            ),
          ),
        );
      });

      test('login 실패 - 잘못된 비밀번호 → Exception 발생', () async {
        // Act & Assert
        expect(
          () => repository.login('20010585', 'wrong_password'),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('사번 또는 비밀번호가 올바르지 않습니다'),
            ),
          ),
        );
      });

      test('login - 초기 비밀번호 계정 "20020001"/"otg1" → requiresPasswordChange=true, requiresGpsConsent=true', () async {
        // Arrange
        const employeeId = '20020001';
        const password = 'otg1';

        // Act
        final result = await repository.login(employeeId, password);

        // Assert
        expect(result.user.employeeId, equals(employeeId));
        expect(result.user.name, equals('김영업'));
        expect(result.user.department, equals('영업2팀'));
        expect(result.user.branchName, equals('서울2지점'));
        expect(result.user.role, equals('USER'));
        expect(result.requiresPasswordChange, isTrue);
        expect(result.requiresGpsConsent, isTrue);
      });

      test('login - 일반 계정 "20010585"/"test1234" → requiresPasswordChange=false', () async {
        // Arrange
        const employeeId = '20010585';
        const password = 'test1234';

        // Act
        final result = await repository.login(employeeId, password);

        // Assert
        expect(result.requiresPasswordChange, isFalse);
        expect(result.requiresGpsConsent, isFalse);
      });

      test('login - 리더 계정 "20030117"/"test1234" → role=LEADER', () async {
        // Arrange
        const employeeId = '20030117';
        const password = 'test1234';

        // Act
        final result = await repository.login(employeeId, password);

        // Assert
        expect(result.user.employeeId, equals(employeeId));
        expect(result.user.name, equals('김조장'));
        expect(result.user.department, equals('영업1팀'));
        expect(result.user.branchName, equals('부산1지점'));
        expect(result.user.role, equals('LEADER'));
        expect(result.requiresPasswordChange, isFalse);
        expect(result.requiresGpsConsent, isFalse);
      });
    });

    group('refreshToken', () {
      test('refreshToken 성공 - "mock_" prefix token → new AuthToken', () async {
        // Arrange
        const refreshToken = 'mock_refresh_token_test';

        // Act
        final result = await repository.refreshToken(refreshToken);

        // Assert
        expect(result.accessToken, contains('mock_access_token'));
        expect(result.accessToken, contains('refreshed'));
        expect(result.refreshToken, equals(refreshToken));
        expect(result.expiresIn, equals(3600));
      });

      test('refreshToken 실패 - invalid token → Exception', () async {
        // Act & Assert
        expect(
          () => repository.refreshToken('invalid_token'),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('세션이 만료되었습니다'),
            ),
          ),
        );
      });

      test('refreshToken - 빈 문자열 → Exception', () async {
        // Act & Assert
        expect(
          () => repository.refreshToken(''),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('세션이 만료되었습니다'),
            ),
          ),
        );
      });
    });

    group('changePassword', () {
      test('changePassword 성공 - 로그인 후 올바른 현재 비밀번호', () async {
        // Arrange
        const employeeId = '20030117';
        const currentPassword = 'test1234';
        const newPassword = 'new_password_1234';

        // 먼저 로그인하여 _currentEmployeeId 설정
        await repository.login(employeeId, currentPassword);

        // Act
        await repository.changePassword(currentPassword, newPassword);

        // Assert - 예외가 발생하지 않으면 성공
        expect(true, isTrue);
      });

      test('changePassword 실패 - 잘못된 현재 비밀번호 → Exception', () async {
        // Arrange
        const employeeId = '20020001';
        const correctPassword = 'otg1';
        const wrongPassword = 'wrong_password';
        const newPassword = 'new_password_1234';

        // 먼저 로그인
        await repository.login(employeeId, correctPassword);

        // Act & Assert
        expect(
          () => repository.changePassword(wrongPassword, newPassword),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('현재 비밀번호가 올바르지 않습니다'),
            ),
          ),
        );
      });

      test('changePassword 실패 - 로그인하지 않은 상태 → Exception', () async {
        // Act & Assert
        expect(
          () => repository.changePassword('any_password', 'new_password'),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('인증이 필요합니다'),
            ),
          ),
        );
      });

    });

    group('logout', () {
      test('logout - 정상 완료', () async {
        // Arrange
        await repository.login('20010585', 'test1234');

        // Act
        await repository.logout();

        // Assert - 예외가 발생하지 않으면 성공
        expect(true, isTrue);
      });

      test('logout 후 changePassword 시도 → Exception', () async {
        // Arrange
        await repository.login('20010585', 'test1234');
        await repository.logout();

        // Act & Assert
        expect(
          () => repository.changePassword('test1234', 'new_password'),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('인증이 필요합니다'),
            ),
          ),
        );
      });
    });

    group('recordGpsConsent', () {
      test('recordGpsConsent - 정상 완료', () async {
        // Arrange
        await repository.login('20020001', 'otg1');

        // Act
        await repository.recordGpsConsent();

        // Assert - 예외가 발생하지 않으면 성공
        expect(true, isTrue);
      });

      test('recordGpsConsent - 로그인하지 않은 상태에서도 동작', () async {
        // Act
        await repository.recordGpsConsent();

        // Assert - 예외가 발생하지 않으면 성공
        expect(true, isTrue);
      });
    });

    group('통합 시나리오', () {
      test('로그인 → 토큰 갱신 → 로그아웃', () async {
        // 1. 로그인
        final loginResult = await repository.login('20010585', 'test1234');
        expect(loginResult.user.name, equals('홍길동'));

        // 2. 토큰 갱신
        final newToken = await repository.refreshToken(loginResult.token.refreshToken);
        expect(newToken.refreshToken, equals(loginResult.token.refreshToken));
        expect(newToken.accessToken, isNot(equals(loginResult.token.accessToken)));

        // 3. 로그아웃
        await repository.logout();
      });
    });
  });
}
