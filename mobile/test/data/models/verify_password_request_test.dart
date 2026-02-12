import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/verify_password_request.dart';

void main() {
  group('VerifyPasswordRequest 모델', () {
    test('toJson이 올바르게 동작한다', () {
      const request = VerifyPasswordRequest(currentPassword: '1234');

      final json = request.toJson();

      expect(json, {
        'current_password': '1234',
      });
    });

    test('toJson이 snake_case를 사용한다', () {
      const request = VerifyPasswordRequest(currentPassword: 'test123');

      final json = request.toJson();

      expect(json.containsKey('current_password'), true);
      expect(json.containsKey('currentPassword'), false);
    });
  });
}
