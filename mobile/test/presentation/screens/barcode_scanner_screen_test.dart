import 'dart:ui';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/screens/barcode_scanner_screen.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

/// 바코드 스캔 화면의 두 규약을 고정한다.
///
/// - 가이드 프레임: 겨냥용 안내이므로 화면 폭에 비례하되 화면 밖으로 나가지 않는다.
/// - 바코드 선택: `scanWindow` 로 인식 영역을 자르지 않는 대신(안드로이드에서
///   인식 지연을 유발) 프레임 중앙 = 이미지 중심에 가장 가까운 바코드를 고른다.
void main() {
  group('barcodeGuideRect', () {
    test('충분히 큰 레이아웃에서는 화면 폭에 비례해 중앙에 위치한다', () {
      const layout = Size(400, 800);

      final rect = barcodeGuideRect(layout);

      expect(rect.width, 400 * kBarcodeGuideWidthRatio);
      expect(rect.height, kBarcodeGuideHeight);
      expect(rect.center, const Offset(200, 400));
    });

    test('아주 넓은 레이아웃에서도 상한 폭을 넘지 않는다', () {
      const layout = Size(1200, 800);

      final rect = barcodeGuideRect(layout);

      expect(rect.width, kBarcodeGuideMaxWidth);
      expect(rect.center, const Offset(600, 400));
    });

    test('레이아웃이 좁으면 화면 밖으로 나가지 않는다', () {
      const layout = Size(200, 800);

      final rect = barcodeGuideRect(layout);

      expect(rect.width, lessThanOrEqualTo(layout.width));
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

  group('centerMostBarcodeValue', () {
    /// [center] 를 중심으로 하는 40x20 크기의 바코드.
    Barcode barcodeAt(String value, Offset center) {
      return Barcode(
        rawValue: value,
        corners: [
          center + const Offset(-20, -10),
          center + const Offset(20, -10),
          center + const Offset(20, 10),
          center + const Offset(-20, 10),
        ],
      );
    }

    test('바코드가 없으면 null 을 반환한다', () {
      expect(
        centerMostBarcodeValue(const BarcodeCapture(size: Size(1280, 720))),
        isNull,
      );
    });

    test('값이 비어 있는 바코드는 후보에서 제외한다', () {
      final capture = BarcodeCapture(
        size: const Size(1280, 720),
        barcodes: [
          barcodeAt('   ', const Offset(640, 360)),
          barcodeAt('8801045123456', const Offset(200, 100)),
        ],
      );

      expect(centerMostBarcodeValue(capture), '8801045123456');
    });

    test('여러 개가 잡히면 이미지 중심에 가장 가까운 값을 고른다', () {
      final capture = BarcodeCapture(
        size: const Size(1280, 720),
        barcodes: [
          barcodeAt('8801045000001', const Offset(120, 80)),
          barcodeAt('8801045000002', const Offset(660, 380)),
          barcodeAt('8801045000003', const Offset(1200, 700)),
        ],
      );

      expect(centerMostBarcodeValue(capture), '8801045000002');
    });

    test('좌표를 알 수 없는 바코드는 좌표가 있는 바코드보다 후순위다', () {
      final capture = BarcodeCapture(
        size: const Size(1280, 720),
        barcodes: [
          const Barcode(rawValue: '8801045000009'),
          barcodeAt('8801045000002', const Offset(1200, 700)),
        ],
      );

      expect(centerMostBarcodeValue(capture), '8801045000002');
    });

    test('좌표가 전혀 없으면 검출 순서를 따른다', () {
      final capture = BarcodeCapture(
        size: Size.zero,
        barcodes: [
          const Barcode(rawValue: '8801045000009'),
          const Barcode(rawValue: '8801045000002'),
        ],
      );

      expect(centerMostBarcodeValue(capture), '8801045000009');
    });

    test('값 앞뒤 공백은 제거한다', () {
      final capture = BarcodeCapture(
        size: const Size(1280, 720),
        barcodes: [barcodeAt('  8801045123456 ', const Offset(640, 360))],
      );

      expect(centerMostBarcodeValue(capture), '8801045123456');
    });
  });
}
