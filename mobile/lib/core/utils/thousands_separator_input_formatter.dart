import 'package:flutter/services.dart';

/// 숫자 입력에 천단위 콤마를 붙이는 입력 포매터.
///
/// 레거시(Heroku) 금액/수량 입력의 `numberWithCommas` 표기 정합용. 숫자 이외 문자는
/// 모두 제거하며, 선행 0 은 제거한다("05" → "5"). 자릿수 제한이 없으므로 int 파싱 대신
/// 문자열 그룹핑으로 처리해 오버플로를 피한다.
///
/// 사용하는 쪽은 상태 값을 [format] 으로 변환해 전달하고, 입력 콜백에서는 [digitsOf]
/// 로 숫자만 뽑아 파싱해야 표시값과 상태값이 어긋나지 않는다.
class ThousandsSeparatorInputFormatter extends TextInputFormatter {
  const ThousandsSeparatorInputFormatter();

  /// 문자열에서 숫자만 추출 (콤마 제거).
  static String digitsOf(String text) => text.replaceAll(RegExp(r'[^0-9]'), '');

  /// 정수를 천단위 콤마 문자열로 변환 (null → 빈 문자열).
  static String format(int? value) =>
      value == null ? '' : _group(value.toString());

  static String _group(String digits) {
    final buffer = StringBuffer();
    for (var i = 0; i < digits.length; i++) {
      if (i > 0 && (digits.length - i) % 3 == 0) buffer.write(',');
      buffer.write(digits[i]);
    }
    return buffer.toString();
  }

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final cursor = newValue.selection.end.clamp(0, newValue.text.length);
    final digitsBeforeCursor = digitsOf(newValue.text.substring(0, cursor)).length;

    var digits = digitsOf(newValue.text);
    digits = digits.replaceFirst(RegExp(r'^0+(?=\d)'), '');
    if (digits.isEmpty) return const TextEditingValue();

    final formatted = _group(digits);

    // 커서는 "앞쪽 숫자 개수" 를 보존하는 위치로 옮긴다(콤마 삽입으로 밀린 만큼 보정).
    var offset = formatted.length;
    if (digitsBeforeCursor == 0) {
      offset = 0;
    } else {
      var seen = 0;
      for (var i = 0; i < formatted.length; i++) {
        if (formatted[i] != ',') seen++;
        if (seen == digitsBeforeCursor) {
          offset = i + 1;
          break;
        }
      }
    }

    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: offset),
    );
  }
}
