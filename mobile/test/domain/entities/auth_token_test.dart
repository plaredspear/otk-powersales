import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/auth_token.dart';
import '../../test_helper.dart';

void main() {
  setUpAll(() async {
    await TestHelper.initialize();
  });

  const testToken = AuthToken(
    accessToken: 'test_access_token',
    refreshToken: 'test_refresh_token',
    expiresIn: 3600,
  );

  group('AuthToken Entity 생성 테스트', () {
    test('AuthToken 인스턴스가 올바르게 생성되는지 확인', () {
      expect(testToken.accessToken, 'test_access_token');
      expect(testToken.refreshToken, 'test_refresh_token');
      expect(testToken.expiresIn, 3600);
    });
  });

  group('AuthToken copyWith 테스트', () {
    test('일부 필드만 변경', () {
      final updatedToken = testToken.copyWith(
        accessToken: 'new_access_token',
      );

      expect(updatedToken.accessToken, 'new_access_token');
      expect(updatedToken.refreshToken, testToken.refreshToken);
      expect(updatedToken.expiresIn, testToken.expiresIn);
    });

    test('변경하지 않은 필드 유지', () {
      final updatedToken = testToken.copyWith(expiresIn: 7200);

      expect(updatedToken.accessToken, testToken.accessToken);
      expect(updatedToken.refreshToken, testToken.refreshToken);
      expect(updatedToken.expiresIn, 7200);
    });

    test('모든 필드 변경', () {
      final updatedToken = testToken.copyWith(
        accessToken: 'new_access_token',
        refreshToken: 'new_refresh_token',
        expiresIn: 7200,
      );

      expect(updatedToken.accessToken, 'new_access_token');
      expect(updatedToken.refreshToken, 'new_refresh_token');
      expect(updatedToken.expiresIn, 7200);
    });
  });

  group('AuthToken toJson/fromJson 테스트', () {
    test('toJson 직렬화', () {
      final json = testToken.toJson();

      expect(json['accessToken'], 'test_access_token');
      expect(json['refreshToken'], 'test_refresh_token');
      expect(json['expiresIn'], 3600);
    });

    test('fromJson 역직렬화', () {
      final json = {
        'accessToken': 'test_access_token',
        'refreshToken': 'test_refresh_token',
        'expiresIn': 3600,
      };

      final token = AuthToken.fromJson(json);

      expect(token.accessToken, 'test_access_token');
      expect(token.refreshToken, 'test_refresh_token');
      expect(token.expiresIn, 3600);
    });

    test('toJson/fromJson 라운드트립', () {
      final json = testToken.toJson();
      final token = AuthToken.fromJson(json);

      expect(token, testToken);
    });
  });

  group('AuthToken equality 테스트', () {
    test('같은 값을 가진 AuthToken은 같은 객체', () {
      const token1 = AuthToken(
        accessToken: 'test_access_token',
        refreshToken: 'test_refresh_token',
        expiresIn: 3600,
      );

      const token2 = AuthToken(
        accessToken: 'test_access_token',
        refreshToken: 'test_refresh_token',
        expiresIn: 3600,
      );

      expect(token1, token2);
    });

    test('다른 값을 가진 AuthToken은 다른 객체', () {
      const token1 = AuthToken(
        accessToken: 'test_access_token',
        refreshToken: 'test_refresh_token',
        expiresIn: 3600,
      );

      const token2 = AuthToken(
        accessToken: 'different_access_token',
        refreshToken: 'different_refresh_token',
        expiresIn: 7200,
      );

      expect(token1, isNot(token2));
    });

    test('일부 필드만 다른 AuthToken은 다른 객체', () {
      const token1 = AuthToken(
        accessToken: 'test_access_token',
        refreshToken: 'test_refresh_token',
        expiresIn: 3600,
      );

      const token2 = AuthToken(
        accessToken: 'different_access_token',
        refreshToken: 'test_refresh_token',
        expiresIn: 3600,
      );

      expect(token1, isNot(token2));
    });
  });

  group('AuthToken hashCode 테스트', () {
    test('같은 값을 가진 AuthToken은 같은 hashCode', () {
      const token1 = AuthToken(
        accessToken: 'test_access_token',
        refreshToken: 'test_refresh_token',
        expiresIn: 3600,
      );

      const token2 = AuthToken(
        accessToken: 'test_access_token',
        refreshToken: 'test_refresh_token',
        expiresIn: 3600,
      );

      expect(token1.hashCode, token2.hashCode);
    });

    test('다른 값을 가진 AuthToken은 다른 hashCode', () {
      const token1 = AuthToken(
        accessToken: 'test_access_token',
        refreshToken: 'test_refresh_token',
        expiresIn: 3600,
      );

      const token2 = AuthToken(
        accessToken: 'different_access_token',
        refreshToken: 'different_refresh_token',
        expiresIn: 7200,
      );

      expect(token1.hashCode, isNot(token2.hashCode));
    });
  });

  group('AuthToken toString 테스트', () {
    test('toString 포맷 확인', () {
      final result = testToken.toString();

      expect(result, contains('AuthToken'));
      expect(result, contains('accessToken: test_access_token'));
      expect(result, contains('refreshToken: test_refresh_token'));
      expect(result, contains('expiresIn: 3600'));
    });
  });
}
