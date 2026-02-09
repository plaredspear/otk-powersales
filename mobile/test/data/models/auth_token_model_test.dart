import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/auth_token_model.dart';
import 'package:mobile/domain/entities/auth_token.dart';

void main() {
  group('AuthTokenModel', () {
    const testModel = AuthTokenModel(
      accessToken: 'test_access_token',
      refreshToken: 'test_refresh_token',
      expiresIn: 3600,
    );

    final testJson = {
      'access_token': 'test_access_token',
      'refresh_token': 'test_refresh_token',
      'expires_in': 3600,
    };

    final testEntity = AuthToken(
      accessToken: 'test_access_token',
      refreshToken: 'test_refresh_token',
      expiresIn: 3600,
    );

    group('fromJson', () {
      test('snake_case JSON 키를 올바르게 파싱해야 한다', () {
        // Act
        final result = AuthTokenModel.fromJson(testJson);

        // Assert
        expect(result.accessToken, 'test_access_token');
        expect(result.refreshToken, 'test_refresh_token');
        expect(result.expiresIn, 3600);
      });
    });

    group('toJson', () {
      test('snake_case JSON 키로 올바르게 직렬화해야 한다', () {
        // Act
        final result = testModel.toJson();

        // Assert
        expect(result['access_token'], 'test_access_token');
        expect(result['refresh_token'], 'test_refresh_token');
        expect(result['expires_in'], 3600);
      });
    });

    group('toEntity', () {
      test('올바른 AuthToken 엔티티를 생성해야 한다', () {
        // Act
        final result = testModel.toEntity();

        // Assert
        expect(result.accessToken, testModel.accessToken);
        expect(result.refreshToken, testModel.refreshToken);
        expect(result.expiresIn, testModel.expiresIn);
      });
    });

    group('fromEntity', () {
      test('AuthToken 엔티티로부터 올바른 AuthTokenModel을 생성해야 한다', () {
        // Act
        final result = AuthTokenModel.fromEntity(testEntity);

        // Assert
        expect(result.accessToken, testEntity.accessToken);
        expect(result.refreshToken, testEntity.refreshToken);
        expect(result.expiresIn, testEntity.expiresIn);
      });
    });

    group('round trip', () {
      test('fromJson -> toEntity -> fromEntity -> toJson 변환이 일관성 있어야 한다', () {
        // Arrange & Act
        final modelFromJson = AuthTokenModel.fromJson(testJson);
        final entity = modelFromJson.toEntity();
        final modelFromEntity = AuthTokenModel.fromEntity(entity);
        final jsonResult = modelFromEntity.toJson();

        // Assert
        expect(jsonResult, testJson);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 AuthTokenModel은 동일해야 한다', () {
        // Arrange
        const model1 = AuthTokenModel(
          accessToken: 'test_access_token',
          refreshToken: 'test_refresh_token',
          expiresIn: 3600,
        );

        const model2 = AuthTokenModel(
          accessToken: 'test_access_token',
          refreshToken: 'test_refresh_token',
          expiresIn: 3600,
        );

        // Assert
        expect(model1, model2);
      });

      test('다른 값을 가진 두 AuthTokenModel은 동일하지 않아야 한다', () {
        // Arrange
        const model1 = AuthTokenModel(
          accessToken: 'test_access_token',
          refreshToken: 'test_refresh_token',
          expiresIn: 3600,
        );

        const model2 = AuthTokenModel(
          accessToken: 'different_access_token',
          refreshToken: 'different_refresh_token',
          expiresIn: 7200,
        );

        // Assert
        expect(model1, isNot(model2));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 AuthTokenModel은 같은 hashCode를 가져야 한다', () {
        // Arrange
        const model1 = AuthTokenModel(
          accessToken: 'test_access_token',
          refreshToken: 'test_refresh_token',
          expiresIn: 3600,
        );

        const model2 = AuthTokenModel(
          accessToken: 'test_access_token',
          refreshToken: 'test_refresh_token',
          expiresIn: 3600,
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
        expect(result, contains('AuthTokenModel'));
        expect(result, contains('accessToken: test_access_token'));
        expect(result, contains('refreshToken: test_refresh_token'));
        expect(result, contains('expiresIn: 3600'));
      });
    });
  });
}
