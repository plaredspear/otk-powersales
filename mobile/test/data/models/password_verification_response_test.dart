import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/password_verification_response.dart';

void main() {
  group('PasswordVerificationResponse 모델', () {
    test('fromJson이 올바르게 동작한다 - 일치', () {
      final json = {'is_valid': true};

      final response = PasswordVerificationResponse.fromJson(json);

      expect(response.isValid, true);
    });

    test('fromJson이 올바르게 동작한다 - 불일치', () {
      final json = {'is_valid': false};

      final response = PasswordVerificationResponse.fromJson(json);

      expect(response.isValid, false);
    });

    test('fromJson이 snake_case에서 파싱한다', () {
      final json = {'is_valid': true};

      expect(
        () => PasswordVerificationResponse.fromJson(json),
        returnsNormally,
      );
    });
  });
}
