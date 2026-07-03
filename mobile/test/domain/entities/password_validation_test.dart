import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/password_validation.dart';

void main() {
  group('PasswordValidation 엔티티 (8자 이상 + 3종 이상 조합)', () {
    test('모든 규칙 충족 -> isValid=true', () {
      const validation = PasswordValidation(
        isLengthValid: true,
        hasEnoughCharacterTypes: true,
      );

      expect(validation.isValid, true);
    });

    test('길이 미충족 -> isValid=false', () {
      const validation = PasswordValidation(
        isLengthValid: false,
        hasEnoughCharacterTypes: true,
      );

      expect(validation.isValid, false);
    });

    test('문자 종류 부족 -> isValid=false', () {
      const validation = PasswordValidation(
        isLengthValid: true,
        hasEnoughCharacterTypes: false,
      );

      expect(validation.isValid, false);
    });

    group('fromPassword factory', () {
      test('"abcd123!" (8자, 소문자/숫자/특수 3종) -> 모두 충족', () {
        final v = PasswordValidation.fromPassword('abcd123!');
        expect(v.isLengthValid, true);
        expect(v.hasEnoughCharacterTypes, true);
        expect(v.isValid, true);
      });

      test('임시 비밀번호 "pwrs1234!" -> 모두 충족', () {
        final v = PasswordValidation.fromPassword('pwrs1234!');
        expect(v.isValid, true);
      });

      test('"Abc12!x" (7자) -> 길이 부족', () {
        final v = PasswordValidation.fromPassword('Abc12!x');
        expect(v.isLengthValid, false);
        expect(v.isValid, false);
      });

      test('상한 없음 - 64자 (3종) -> 길이 충족', () {
        final v = PasswordValidation.fromPassword('Ab1!${'c' * 60}');
        expect(v.isLengthValid, true);
        expect(v.hasEnoughCharacterTypes, true);
      });

      test('소문자만 8자 (1종) -> 종류 부족', () {
        final v = PasswordValidation.fromPassword('abcdefgh');
        expect(v.isLengthValid, true);
        expect(v.hasEnoughCharacterTypes, false);
        expect(v.isValid, false);
      });

      test('소문자+숫자 8자 (2종) -> 종류 부족', () {
        final v = PasswordValidation.fromPassword('abcd1234');
        expect(v.hasEnoughCharacterTypes, false);
      });

      test('한글은 카테고리 아님 - 한글6+숫자2 (숫자 1종) -> 종류 부족', () {
        final v = PasswordValidation.fromPassword('가나다라마바12');
        expect(v.hasEnoughCharacterTypes, false);
      });

      test('대문자+소문자+숫자 (3종) -> 종류 충족', () {
        final v = PasswordValidation.fromPassword('Abcdefg1');
        expect(v.hasEnoughCharacterTypes, true);
        expect(v.isValid, true);
      });

      test('빈 문자열 -> 길이 부족', () {
        final v = PasswordValidation.fromPassword('');
        expect(v.isLengthValid, false);
      });

      test('4종 모두 조합 -> 모두 충족', () {
        final v = PasswordValidation.fromPassword('Abcd123!');
        expect(v.isValid, true);
      });
    });

    test('copyWith 동작', () {
      const original = PasswordValidation(
        isLengthValid: true,
        hasEnoughCharacterTypes: false,
      );
      final copied = original.copyWith(hasEnoughCharacterTypes: true);

      expect(copied.isLengthValid, true);
      expect(copied.hasEnoughCharacterTypes, true);
      expect(copied.isValid, true);
    });

    test('동등성 비교', () {
      const a = PasswordValidation(
        isLengthValid: true,
        hasEnoughCharacterTypes: true,
      );
      const b = PasswordValidation(
        isLengthValid: true,
        hasEnoughCharacterTypes: true,
      );
      const c = PasswordValidation(
        isLengthValid: false,
        hasEnoughCharacterTypes: true,
      );

      expect(a, b);
      expect(a.hashCode, b.hashCode);
      expect(a, isNot(c));
    });
  });
}
