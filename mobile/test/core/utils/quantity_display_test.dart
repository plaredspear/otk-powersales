import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/utils/quantity_display.dart';

void main() {
  group('hideZeroQuantity', () {
    test('0 수량은 빈 문자열', () {
      expect(hideZeroQuantity('0 BOX'), '');
      expect(hideZeroQuantity('0 BOX (0 EA)'), '');
      expect(hideZeroQuantity('0.00 BOX'), '');
    });

    test('0 이 아닌 수량은 원본 유지', () {
      expect(hideZeroQuantity('1 BOX'), '1 BOX');
      expect(hideZeroQuantity('10 BOX (300 EA)'), '10 BOX (300 EA)');
      expect(hideZeroQuantity('1,234.5 BOX'), '1,234.5 BOX');
      // BOX 0 이어도 EA 가 남아있으면 표기 유지 (실제 출하량이 있는 케이스).
      expect(hideZeroQuantity('0 BOX (5 EA)'), '0 BOX (5 EA)');
    });

    test('숫자가 없는 문자열은 원본 유지', () {
      expect(hideZeroQuantity(''), '');
      expect(hideZeroQuantity('-'), '-');
    });
  });
}
