import 'dart:ui';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/screens/barcode_scanner_screen.dart';

/// 바코드 스캔 가이드 프레임 좌표 — `MobileScanner.scanWindow` 와 화면 오버레이가
/// 같은 사각형을 공유하므로, 여기서 계산한 Rect 가 곧 실제 인식 영역이다.
///
/// 인접 제품의 바코드가 잘못 선택되는 문제를 막기 위해 도입한 것이라,
/// 프레임이 화면 중앙에 위치하고 레이아웃 밖으로 벗어나지 않는 규약을 고정한다.
void main() {
  group('barcodeGuideRect', () {
    test('충분히 큰 레이아웃에서는 기본 크기로 화면 중앙에 위치한다', () {
      const layout = Size(400, 800);

      final rect = barcodeGuideRect(layout);

      expect(rect.width, kBarcodeGuideWidth);
      expect(rect.height, kBarcodeGuideHeight);
      expect(rect.center, const Offset(200, 400));
    });

    test('레이아웃이 기본 폭보다 좁으면 화면 밖으로 나가지 않는다', () {
      const layout = Size(200, 800);

      final rect = barcodeGuideRect(layout);

      expect(rect.width, 200);
      expect(rect.left, greaterThanOrEqualTo(0));
      expect(rect.right, lessThanOrEqualTo(layout.width));
    });

    test('레이아웃이 기본 높이보다 낮으면 세로도 레이아웃 안에 들어온다', () {
      const layout = Size(400, 120);

      final rect = barcodeGuideRect(layout);

      expect(rect.height, 120);
      expect(rect.top, greaterThanOrEqualTo(0));
      expect(rect.bottom, lessThanOrEqualTo(layout.height));
    });
  });
}
