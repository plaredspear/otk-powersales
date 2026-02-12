import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/change_password_request.dart';

void main() {
  group('ChangePasswordRequest 모델', () {
    test('toJson이 올바르게 동작한다', () {
      const request = ChangePasswordRequest(
        currentPassword: '1234',
        newPassword: '5678',
      );

      final json = request.toJson();

      expect(json, {
        'current_password': '1234',
        'new_password': '5678',
      });
    });

    test('toJson이 snake_case를 사용한다', () {
      const request = ChangePasswordRequest(
        currentPassword: 'old',
        newPassword: 'new',
      );

      final json = request.toJson();

      expect(json.containsKey('current_password'), true);
      expect(json.containsKey('new_password'), true);
      expect(json.containsKey('currentPassword'), false);
      expect(json.containsKey('newPassword'), false);
    });
  });
}
