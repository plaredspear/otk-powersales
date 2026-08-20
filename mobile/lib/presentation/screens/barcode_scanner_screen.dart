import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../../core/theme/app_colors.dart';

/// 스캔 가이드 프레임 크기 — 화면 폭 비례(상한 [kBarcodeGuideMaxWidth]).
///
/// 고정 폭(260) 으로 두면 고밀도 대화면 안드로이드에서 프레임이 상대적으로 좁아
/// 바코드를 통째로 넣기 위해 거리를 벌려야 하고, 그만큼 바코드가 작아져 디코딩이
/// 늦어진다. 화면 폭에 비례시켜 겨냥 여유를 준다.
const double kBarcodeGuideWidthRatio = 0.8;
const double kBarcodeGuideMaxWidth = 420;
const double kBarcodeGuideHeight = 200;

/// 가이드 프레임의 위치/크기를 계산한다.
///
/// 이 사각형은 **겨냥용 안내**이며 인식 영역을 물리적으로 자르지 않는다
/// (`MobileScanner.scanWindow` 미사용 — 사유는 [BarcodeScannerScreen] 참조).
/// 프레임 밖 바코드가 함께 잡히더라도 [centerMostBarcodeValue] 가 화면 중앙에
/// 가장 가까운 것을 고른다. 레이아웃이 기본 크기보다 작으면 프레임이 화면 밖으로
/// 나가지 않도록 레이아웃 경계까지 줄인다.
Rect barcodeGuideRect(Size layoutSize) {
  // 비율 폭은 항상 레이아웃 안에 들어오므로 상한만 적용하면 된다.
  final double width = math.min(
    layoutSize.width * kBarcodeGuideWidthRatio,
    kBarcodeGuideMaxWidth,
  );
  final height = kBarcodeGuideHeight.clamp(0.0, layoutSize.height);
  return Rect.fromCenter(
    center: layoutSize.center(Offset.zero),
    width: width,
    height: height,
  );
}

/// 한 프레임에서 검출된 바코드 중 이미지 중심에 가장 가까운 값을 고른다.
///
/// 값이 비어 있는 바코드는 제외하고, 위치를 알 수 없는(코너 좌표가 없거나 이미지
/// 크기가 0인) 바코드는 최후순위로 밀어 검출 순서를 그대로 따른다. 후보가 없으면
/// null.
String? centerMostBarcodeValue(BarcodeCapture capture) {
  final imageCenter = Offset(capture.size.width / 2, capture.size.height / 2);

  String? best;
  double bestDistance = double.infinity;

  for (final barcode in capture.barcodes) {
    final value = barcode.rawValue?.trim();
    if (value == null || value.isEmpty) continue;

    final distance = _distanceFromImageCenter(barcode, imageCenter);
    if (best == null || distance < bestDistance) {
      best = value;
      bestDistance = distance;
    }
  }

  return best;
}

/// 바코드 코너 좌표의 무게중심과 이미지 중심 사이의 거리.
///
/// 좌표를 알 수 없으면 [double.infinity] — 위치를 아는 바코드가 항상 우선한다.
double _distanceFromImageCenter(Barcode barcode, Offset imageCenter) {
  if (barcode.corners.isEmpty || imageCenter == Offset.zero) {
    return double.infinity;
  }

  var sumX = 0.0;
  var sumY = 0.0;
  for (final corner in barcode.corners) {
    sumX += corner.dx;
    sumY += corner.dy;
  }
  final centroid = Offset(
    sumX / barcode.corners.length,
    sumY / barcode.corners.length,
  );

  return (centroid - imageCenter).distance;
}

/// 바코드 스캔 화면.
///
/// 레거시 `posmain.jsp` 의 `powersales://barcode` 딥링크(네이티브 스캐너) 동등 — 카메라로 제품
/// 바코드를 스캔해 그 값을 문자열로 반환한다. 취소 시 null.
///
/// ## 안드로이드 인식 지연 대응 (mobile_scanner 7.2.0)
///
/// 안드로이드(CameraX + ML Kit) 와 iOS(AVCapture + Vision) 의 구현 차이 때문에
/// 아래 설정은 안드로이드 기준으로 잡혀 있다.
///
/// - `scanWindow` 미사용: 안드로이드는 **전체 프레임을 검출한 뒤** 창 밖 결과를
///   버리는데, ① 중복 판정(`noDuplicates`) 이 이 필터보다 먼저 실행돼 창 밖에서
///   한 번 잡힌 값이 이후 계속 "중복" 으로 폐기되고, ② 판정이 "네 꼭짓점 전부
///   포함" 이라 조금만 걸쳐도 탈락하며, ③ 창 좌표가 preview 가 아닌 analysis
///   해상도 기준이라 기기에 따라 보이는 사각형과 실제 인식 영역이 어긋난다.
///   iOS 는 `regionOfInterest` 로 검출 전에 잘라내 이런 문제가 없다.
///   대신 [centerMostBarcodeValue] 로 화면 중앙에 가장 가까운 바코드를 고른다.
/// - [DetectionSpeed.unrestricted]: 1회 스캔 후 즉시 화면을 닫으므로 중복 제거가
///   불필요하다. 중복 방지는 `_handled` 가드가 담당한다.
/// - `cameraResolution` 720p 고정: 미지정 시 안드로이드 기본값이 1920x1080 이라
///   중저가 기기에서 프레임당 처리시간이 커지고 실효 프레임률이 떨어진다.
/// - `formats` 4종: 취급 제품 바코드에 쓰이지 않는 포맷까지 켜두면 프레임마다
///   1D 디코더를 불필요하게 더 돌린다.
/// - `initialZoom` 미사용: 초기 줌을 주면 프레임 픽셀 수는 늘지만 안드로이드의
///   디지털 줌은 센서 크롭이라 화면이 흐려 보이고, 확대된 화각에서 오토포커스가
///   더 오래 헤맨다. 현장에서 "흐려서 잘 안 된다" 는 피드백이 나와 되돌렸다.
class BarcodeScannerScreen extends StatefulWidget {
  const BarcodeScannerScreen({super.key});

  /// 스캔 화면을 띄우고 스캔된 바코드 문자열을 반환한다 (취소/실패 시 null).
  static Future<String?> show(BuildContext context) {
    return Navigator.of(context).push<String>(
      MaterialPageRoute(builder: (_) => const BarcodeScannerScreen()),
    );
  }

  @override
  State<BarcodeScannerScreen> createState() => _BarcodeScannerScreenState();
}

class _BarcodeScannerScreenState extends State<BarcodeScannerScreen> {
  final MobileScannerController _controller = MobileScannerController(
    detectionSpeed: DetectionSpeed.unrestricted,
    cameraResolution: const Size(1280, 720),
    formats: const [
      BarcodeFormat.ean13,
      BarcodeFormat.ean8,
      BarcodeFormat.upcA,
      BarcodeFormat.code128,
    ],
  );

  /// 중복 콜백으로 인한 다중 pop 방지.
  bool _handled = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onDetect(BarcodeCapture capture) {
    if (_handled) return;
    final code = centerMostBarcodeValue(capture);
    if (code == null) return;
    _handled = true;
    Navigator.of(context).pop(code);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.black,
      appBar: AppBar(
        backgroundColor: AppColors.black,
        foregroundColor: AppColors.white,
        title: const Text('바코드 스캔'),
        actions: [
          IconButton(
            icon: const Icon(Icons.flash_on),
            tooltip: '플래시',
            onPressed: () => _controller.toggleTorch(),
          ),
          IconButton(
            icon: const Icon(Icons.cameraswitch),
            tooltip: '카메라 전환',
            onPressed: () => _controller.switchCamera(),
          ),
        ],
      ),
      body: LayoutBuilder(
        builder: (context, constraints) {
          final guide = barcodeGuideRect(constraints.biggest);
          return MobileScanner(
            controller: _controller,
            onDetect: _onDetect,
            errorBuilder: (context, error) => _buildError(error),
            overlayBuilder: (context, _) => _buildOverlay(guide),
          );
        },
      ),
    );
  }

  /// 스캔 가이드 프레임 + 안내 문구 오버레이.
  ///
  /// 프레임은 겨냥용 안내다. 인식 자체는 화면 전체에서 이루어지고, 여러 개가
  /// 잡히면 프레임 중앙에 가장 가까운 바코드가 선택된다.
  Widget _buildOverlay(Rect guide) {
    return Stack(
      children: [
        // 프레임 밖을 어둡게 덮어 인식 영역을 시각적으로 구분한다.
        Positioned.fill(
          child: IgnorePointer(
            child: ColorFiltered(
              colorFilter: ColorFilter.mode(
                AppColors.black.withValues(alpha: 0.5),
                BlendMode.srcOut,
              ),
              child: Stack(
                children: [
                  Positioned.fill(
                    child: DecoratedBox(
                      decoration: const BoxDecoration(
                        color: Colors.black,
                        backgroundBlendMode: BlendMode.dstOut,
                      ),
                    ),
                  ),
                  Positioned.fromRect(
                    rect: guide,
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        color: Colors.black,
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        Positioned.fromRect(
          rect: guide,
          child: IgnorePointer(
            child: DecoratedBox(
              decoration: BoxDecoration(
                border: Border.all(color: AppColors.otokiRed, width: 3),
                borderRadius: BorderRadius.circular(12),
              ),
            ),
          ),
        ),
        Positioned(
          left: 0,
          right: 0,
          bottom: 60,
          child: IgnorePointer(
            child: Center(
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: AppColors.overlay,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Text(
                  '제품 바코드를 사각형 안에 비춰주세요',
                  style: TextStyle(color: Colors.white, fontSize: 14),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildError(MobileScannerException error) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.no_photography, color: Colors.white70, size: 48),
            const SizedBox(height: 16),
            Text(
              error.errorCode == MobileScannerErrorCode.permissionDenied
                  ? '카메라 권한이 필요합니다.\n설정에서 카메라 접근을 허용해주세요.'
                  : '카메라를 시작할 수 없습니다.',
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.white70, fontSize: 14),
            ),
          ],
        ),
      ),
    );
  }
}
