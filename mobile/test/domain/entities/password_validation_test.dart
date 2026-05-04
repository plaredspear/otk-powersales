import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/password_validation.dart';

void main() {
  group('PasswordValidation 엔티티 (Spec #584)', () {
    test('모든 규칙 충족 -> isValid=true', () {
      const validation = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: true,
        isNotTemporary: true,
      );

      expect(validation.isValid, true);
    });

    test('길이 미충족 -> isValid=false', () {
      const validation = PasswordValidation(
        isLengthValid: false,
        isNotRepeating: true,
        isNotTemporary: true,
      );

      expect(validation.isValid, false);
    });

    test('반복 문자 위반 -> isValid=false', () {
      const validation = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: false,
        isNotTemporary: true,
      );

      expect(validation.isValid, false);
    });

    test('임시 비밀번호 동일 -> isValid=false', () {
      const validation = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: true,
        isNotTemporary: false,
      );

      expect(validation.isValid, false);
    });

    group('fromPassword factory', () {
      test('"abcd" -> 모두 충족', () {
        final v = PasswordValidation.fromPassword('abcd');
        expect(v.isLengthValid, true);
        expect(v.isNotRepeating, true);
        expect(v.isNotTemporary, true);
        expect(v.isValid, true);
      });

      test('"abc" -> 길이 부족', () {
        final v = PasswordValidation.fromPassword('abc');
        expect(v.isLengthValid, false);
        expect(v.isValid, false);
      });

      test('33자 -> 길이 초과', () {
        final v = PasswordValidation.fromPassword('a' * 33);
        expect(v.isLengthValid, false);
      });

      test('32자 (반복 없음) -> 길이 충족', () {
        final v = PasswordValidation.fromPassword(
            List.generate(32, (i) => String.fromCharCode(0x61 + (i % 26))).join());
        expect(v.isLengthValid, true);
      });

      test('"aaaa" -> 동일 문자 4연속 위반', () {
        final v = PasswordValidation.fromPassword('aaaa');
        expect(v.isNotRepeating, false);
      });

      test('"가가가가" -> 한글 4연속 위반', () {
        final v = PasswordValidation.fromPassword('가가가가');
        expect(v.isNotRepeating, false);
      });

      test('"!!!!" -> 특수문자 4연속 위반', () {
        final v = PasswordValidation.fromPassword('!!!!');
        expect(v.isNotRepeating, false);
      });

      test('"1234" -> 임시 비밀번호 동일', () {
        final v = PasswordValidation.fromPassword('1234');
        expect(v.isNotTemporary, false);
        expect(v.isValid, false);
      });

      test('빈 문자열 -> 길이 부족', () {
        final v = PasswordValidation.fromPassword('');
        expect(v.isLengthValid, false);
      });

      test('한글/특수문자 혼합 -> 모두 충족', () {
        final v = PasswordValidation.fromPassword('abcd1234!@한');
        expect(v.isValid, true);
      });
    });

    test('copyWith 동작', () {
      const original = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: false,
        isNotTemporary: true,
      );
      final copied = original.copyWith(isNotRepeating: true);

      expect(copied.isLengthValid, true);
      expect(copied.isNotRepeating, true);
      expect(copied.isNotTemporary, true);
      expect(copied.isValid, true);
    });

    test('동등성 비교', () {
      const a = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: true,
        isNotTemporary: true,
      );
      const b = PasswordValidation(
        isLengthValid: true,
        isNotRepeating: true,
        isNotTemporary: true,
      );
      const c = PasswordValidation(
        isLengthValid: false,
        isNotRepeating: true,
        isNotTemporary: true,
      );

      expect(a, b);
      expect(a.hashCode, b.hashCode);
      expect(a, isNot(c));
    });
  });
}
