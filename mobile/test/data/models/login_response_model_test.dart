import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/login_response_model.dart';
import 'package:mobile/data/models/user_model.dart';
import 'package:mobile/data/models/auth_token_model.dart';
import 'package:mobile/domain/repositories/auth_repository.dart';

void main() {
  group('LoginResponseModel', () {
    const testUserModel = UserModel(
      id: 1,
      employeeId: '20010585',
      name: '홍길동',
      department: '영업1팀',
      branchName: '부산1지점',
      role: 'USER',
    );

    const testTokenModel = AuthTokenModel(
      accessToken: 'test_access_token',
      refreshToken: 'test_refresh_token',
      expiresIn: 3600,
    );

    const testModel = LoginResponseModel(
      user: testUserModel,
      token: testTokenModel,
      requiresPasswordChange: true,
      requiresGpsConsent: false,
    );

    final testJson = {
      'user': {
        'id': 1,
        'employee_id': '20010585',
        'name': '홍길동',
        'department': '영업1팀',
        'branch_name': '부산1지점',
        'role': 'USER',
      },
      'token': {
        'access_token': 'test_access_token',
        'refresh_token': 'test_refresh_token',
        'expires_in': 3600,
      },
      'requires_password_change': true,
      'requires_gps_consent': false,
    };

    group('fromJson', () {
      test('중첩된 user와 token을 올바르게 파싱해야 한다', () {
        // Act
        final result = LoginResponseModel.fromJson(testJson);

        // Assert
        expect(result.user.id, 1);
        expect(result.user.employeeId, '20010585');
        expect(result.user.name, '홍길동');
        expect(result.user.department, '영업1팀');
        expect(result.user.branchName, '부산1지점');
        expect(result.user.role, 'USER');

        expect(result.token.accessToken, 'test_access_token');
        expect(result.token.refreshToken, 'test_refresh_token');
        expect(result.token.expiresIn, 3600);

        expect(result.requiresPasswordChange, true);
        expect(result.requiresGpsConsent, false);
      });
    });

    group('toJson', () {
      test('올바른 snake_case 형식으로 직렬화해야 한다', () {
        // Act
        final result = testModel.toJson();

        // Assert
        expect(result['user'], isA<Map<String, dynamic>>());
        expect(result['user']['id'], 1);
        expect(result['user']['employee_id'], '20010585');
        expect(result['user']['name'], '홍길동');
        expect(result['user']['department'], '영업1팀');
        expect(result['user']['branch_name'], '부산1지점');
        expect(result['user']['role'], 'USER');

        expect(result['token'], isA<Map<String, dynamic>>());
        expect(result['token']['access_token'], 'test_access_token');
        expect(result['token']['refresh_token'], 'test_refresh_token');
        expect(result['token']['expires_in'], 3600);

        expect(result['requires_password_change'], true);
        expect(result['requires_gps_consent'], false);
      });
    });

    group('toLoginResult', () {
      test('User와 AuthToken 엔티티를 포함한 올바른 LoginResult를 생성해야 한다', () {
        // Act
        final result = testModel.toLoginResult();

        // Assert
        expect(result, isA<LoginResult>());

        expect(result.user.id, 1);
        expect(result.user.employeeId, '20010585');
        expect(result.user.name, '홍길동');
        expect(result.user.department, '영업1팀');
        expect(result.user.branchName, '부산1지점');
        expect(result.user.role, 'USER');

        expect(result.token.accessToken, 'test_access_token');
        expect(result.token.refreshToken, 'test_refresh_token');
        expect(result.token.expiresIn, 3600);

        expect(result.requiresPasswordChange, true);
        expect(result.requiresGpsConsent, false);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 LoginResponseModel은 동일해야 한다', () {
        // Arrange
        const model1 = LoginResponseModel(
          user: testUserModel,
          token: testTokenModel,
          requiresPasswordChange: true,
          requiresGpsConsent: false,
        );

        const model2 = LoginResponseModel(
          user: testUserModel,
          token: testTokenModel,
          requiresPasswordChange: true,
          requiresGpsConsent: false,
        );

        // Assert
        expect(model1, model2);
      });

      test('다른 값을 가진 두 LoginResponseModel은 동일하지 않아야 한다', () {
        // Arrange
        const model1 = LoginResponseModel(
          user: testUserModel,
          token: testTokenModel,
          requiresPasswordChange: true,
          requiresGpsConsent: false,
        );

        const model2 = LoginResponseModel(
          user: testUserModel,
          token: testTokenModel,
          requiresPasswordChange: false,
          requiresGpsConsent: true,
        );

        // Assert
        expect(model1, isNot(model2));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 LoginResponseModel은 같은 hashCode를 가져야 한다', () {
        // Arrange
        const model1 = LoginResponseModel(
          user: testUserModel,
          token: testTokenModel,
          requiresPasswordChange: true,
          requiresGpsConsent: false,
        );

        const model2 = LoginResponseModel(
          user: testUserModel,
          token: testTokenModel,
          requiresPasswordChange: true,
          requiresGpsConsent: false,
        );

        // Assert
        expect(model1.hashCode, model2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환해야 한다', () {
        // Act
        final result = testModel.toString();

        // Assert
        expect(result, contains('LoginResponseModel'));
        expect(result, contains('user:'));
        expect(result, contains('token:'));
        expect(result, contains('requiresPasswordChange: true'));
        expect(result, contains('requiresGpsConsent: false'));
      });
    });
  });
}
