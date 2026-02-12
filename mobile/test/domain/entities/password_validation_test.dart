import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/password_validation.dart';

void main() {
  group('PasswordValidation 엔티티', () {
    test('올바른 비밀번호로 유효한 객체가 생성된다', () {
      const validation = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: true,
      );

      expect(validation.isLengthValid, true);
      expect(validation.isNotRepeating, true);
      expect(validation.isValid, true);
    });

    test('길이가 부족하면 isValid가 false', () {
      const validation = PasswordValidation(
        isLengthValid: false,
        isNotRepeating: true,
      );

      expect(validation.isValid, false);
    });

    test('동일 문자 반복이면 isValid가 false', () {
      const validation = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: false,
      );

      expect(validation.isValid, false);
    });

    test('fromPassword factory: 4글자 이상 비밀번호는 유효', () {
      final validation = PasswordValidation.fromPassword('1234');

      expect(validation.isLengthValid, true);
      expect(validation.isNotRepeating, true);
      expect(validation.isValid, true);
    });

    test('fromPassword factory: 3글자 이하 비밀번호는 무효', () {
      final validation = PasswordValidation.fromPassword('123');

      expect(validation.isLengthValid, false);
      expect(validation.isValid, false);
    });

    test('fromPassword factory: 동일 문자 반복 비밀번호는 무효', () {
      final validation = PasswordValidation.fromPassword('1111');

      expect(validation.isNotRepeating, false);
      expect(validation.isValid, false);
    });

    test('fromPassword factory: 문자 동일 반복 비밀번호는 무효', () {
      final validation = PasswordValidation.fromPassword('aaaa');

      expect(validation.isNotRepeating, false);
      expect(validation.isValid, false);
    });

    test('fromPassword factory: 빈 문자열은 무효', () {
      final validation = PasswordValidation.fromPassword('');

      expect(validation.isLengthValid, false);
      expect(validation.isValid, false);
    });

    test('copyWith 메서드가 올바르게 동작한다', () {
      const original = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: false,
      );

      final copied = original.copyWith(isNotRepeating: true);

      expect(copied.isLengthValid, true);
      expect(copied.isNotRepeating, true);
      expect(copied.isValid, true);
    });

    test('같은 값을 가진 객체는 동일하게 비교된다', () {
      const validation1 = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: true,
      );

      const validation2 = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: true,
      );

      expect(validation1, validation2);
      expect(validation1.hashCode, validation2.hashCode);
    });

    test('다른 값을 가진 객체는 다르게 비교된다', () {
      const validation1 = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: true,
      );

      const validation2 = PasswordValidation(
        isLengthValid: false,
        isNotRepeating: true,
      );

      expect(validation1, isNot(validation2));
    });

    test('toString 메서드가 올바르게 동작한다', () {
      const validation = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: true,
      );

      final stringValue = validation.toString();

      expect(stringValue, contains('PasswordValidation'));
      expect(stringValue, contains('isLengthValid: true'));
      expect(stringValue, contains('isNotRepeating: true'));
      expect(stringValue, contains('isValid: true'));
    });
  });
}
