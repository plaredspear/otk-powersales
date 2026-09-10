import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/utils/thousands_separator_input_formatter.dart';

void main() {
  const formatter = ThousandsSeparatorInputFormatter();

  TextEditingValue edit(String text, {int? cursor}) => TextEditingValue(
        text: text,
        selection: TextSelection.collapsed(offset: cursor ?? text.length),
      );

  group('format / digitsOf', () {
    test('천단위 콤마 표기', () {
      expect(ThousandsSeparatorInputFormatter.format(null), '');
      expect(ThousandsSeparatorInputFormatter.format(0), '0');
      expect(ThousandsSeparatorInputFormatter.format(999), '999');
      expect(ThousandsSeparatorInputFormatter.format(1000), '1,000');
      expect(ThousandsSeparatorInputFormatter.format(1234567), '1,234,567');
    });

    test('digitsOf 는 콤마/문자를 제거', () {
      expect(ThousandsSeparatorInputFormatter.digitsOf('1,234,567'), '1234567');
      expect(ThousandsSeparatorInputFormatter.digitsOf('12a3'), '123');
      expect(ThousandsSeparatorInputFormatter.digitsOf(''), '');
    });
  });

  group('formatEditUpdate', () {
    test('입력 즉시 콤마가 붙고 커서는 끝에 위치', () {
      final result = formatter.formatEditUpdate(edit('1,00'), edit('1,000'));
      expect(result.text, '1,000');
      expect(result.selection.baseOffset, 5);
    });

    test('숫자 이외 문자는 제거', () {
      final result = formatter.formatEditUpdate(edit(''), edit('12a34'));
      expect(result.text, '1,234');
    });

    test('선행 0 제거', () {
      final result = formatter.formatEditUpdate(edit('0'), edit('05'));
      expect(result.text, '5');
    });

    test('전부 지우면 빈 값', () {
      final result = formatter.formatEditUpdate(edit('1,000'), edit(''));
      expect(result.text, '');
    });

    test('중간 삽입 시 커서가 입력한 숫자 뒤에 유지된다', () {
      // "1,234" 에서 맨 앞에 9 를 입력 → "91,234" (커서는 9 뒤)
      final result = formatter.formatEditUpdate(
        edit('1,234'),
        edit('91,234', cursor: 1),
      );
      expect(result.text, '91,234');
      expect(result.selection.baseOffset, 1);
    });
  });
}
