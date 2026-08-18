import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../../core/theme/app_colors.dart';

/// 바코드 스캔 화면.
///
/// 레거시 `posmain.jsp` 의 `powersales://barcode` 딥링크(네이티브 스캐너) 동등 — 카메라로 제품
/// 바코드를 스캔해 그 값을 문자열로 반환한다. 취소 시 null.
/// 스캔 가이드 프레임(= 실제 인식 영역) 기본 크기.
const double kBarcodeGuideWidth = 260;
const double kBarcodeGuideHeight = 160;

/// 가이드 프레임의 위치/크기를 계산한다.
///
/// 이 사각형을 `MobileScanner.scanWindow` 와 화면 오버레이 양쪽에 그대로 사용해
/// "보이는 사각형 = 실제 인식 영역" 을 보장한다. 레이아웃이 기본 크기보다 작으면
/// 프레임이 화면 밖으로 나가지 않도록 레이아웃 경계까지 줄인다.
Rect barcodeGuideRect(Size layoutSize) {
  final width = kBarcodeGuideWidth.clamp(0.0, layoutSize.width);
  final height = kBarcodeGuideHeight.clamp(0.0, layoutSize.height);
  return Rect.fromCenter(
    center: layoutSize.center(Offset.zero),
    width: width,
    height: height,
  );
}

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
    detectionSpeed: DetectionSpeed.noDuplicates,
    formats: const [
      BarcodeFormat.ean13,
      BarcodeFormat.ean8,
      BarcodeFormat.code128,
      BarcodeFormat.code39,
      BarcodeFormat.upcA,
      BarcodeFormat.upcE,
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
    final code = capture.barcodes
        .map((b) => b.rawValue)
        .firstWhere((v) => v != null && v.trim().isNotEmpty, orElse: () => null);
    if (code == null) return;
    _handled = true;
    Navigator.of(context).pop(code.trim());
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
            // 가이드 프레임 안의 바코드만 인식한다. 프레임 밖(인접 제품)의 바코드가
            // 의도치 않게 선택되는 것을 막는다.
            scanWindow: guide,
            errorBuilder: (context, error) => _buildError(error),
            overlayBuilder: (context, _) => _buildOverlay(guide),
          );
        },
      ),
    );
  }

  /// 스캔 가이드 프레임 + 안내 문구 오버레이.
  ///
  /// 프레임 위치는 [scanWindow] 로 넘긴 [guide] 와 동일한 좌표를 사용하므로
  /// 화면에 보이는 사각형이 곧 실제 인식 영역이다.
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
