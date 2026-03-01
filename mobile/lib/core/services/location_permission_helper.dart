import 'dart:async';

import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';

import '../../presentation/widgets/common/location_permission_bottom_sheet.dart';
import 'location_service.dart';

/// 출근등록 시 OS 권한을 확인하고 GPS 좌표를 반환하는 헬퍼
///
/// 권한이 없으면 바텀시트를 표시하여 권한 획득을 유도한다.
/// 좌표 획득에 실패하면 null을 반환한다 (출근등록은 좌표 없이 진행).
class LocationPermissionHelper {
  final LocationService _locationService;

  LocationPermissionHelper(this._locationService);

  /// 권한 확인 → 바텀시트 → 좌표 조회
  ///
  /// 반환: Position (성공) 또는 null (실패/취소)
  Future<Position?> ensurePermissionAndGetPosition(
    BuildContext context,
  ) async {
    final status = await _locationService.checkPermission();

    switch (status) {
      case LocationPermissionStatus.granted:
        return _getPositionSafely();

      case LocationPermissionStatus.denied:
        return _handleDenied(context);

      case LocationPermissionStatus.deniedForever:
        return _handleDeniedForever(context);

      case LocationPermissionStatus.serviceDisabled:
        return _handleServiceDisabled(context);
    }
  }

  /// denied: 바텀시트 → OS 팝업 재요청
  Future<Position?> _handleDenied(BuildContext context) async {
    final result = await LocationPermissionBottomSheet.show(
      context,
      openSettings: false,
    );
    if (result != true) return null;

    final newStatus = await _locationService.requestPermission();
    if (newStatus == LocationPermissionStatus.granted) {
      return _getPositionSafely();
    }
    return null;
  }

  /// deniedForever: 바텀시트 → 앱 설정 → 복귀 시 재확인
  Future<Position?> _handleDeniedForever(BuildContext context) async {
    final result = await LocationPermissionBottomSheet.show(
      context,
      openSettings: true,
    );
    if (result != true) return null;

    await Geolocator.openAppSettings();

    // 설정에서 돌아올 때까지 대기
    if (!context.mounted) return null;
    await _waitForAppResumed(context);

    final newStatus = await _locationService.checkPermission();
    if (newStatus == LocationPermissionStatus.granted) {
      return _getPositionSafely();
    }
    return null;
  }

  /// serviceDisabled: 바텀시트 → 위치 서비스 설정 → 복귀 시 재확인
  Future<Position?> _handleServiceDisabled(BuildContext context) async {
    final result = await LocationPermissionBottomSheet.show(
      context,
      openSettings: true,
    );
    if (result != true) return null;

    await Geolocator.openLocationSettings();

    // 설정에서 돌아올 때까지 대기
    if (!context.mounted) return null;
    await _waitForAppResumed(context);

    final newStatus = await _locationService.checkPermission();
    if (newStatus == LocationPermissionStatus.granted) {
      return _getPositionSafely();
    }
    return null;
  }

  /// GPS 좌표 조회 (예외 → null)
  Future<Position?> _getPositionSafely() async {
    try {
      return await _locationService.getCurrentPosition();
    } catch (_) {
      return null;
    }
  }

  /// AppLifecycleState.resumed 까지 대기
  Future<void> _waitForAppResumed(BuildContext context) async {
    final completer = _AppLifecycleCompleter();
    final binding = WidgetsBinding.instance;
    binding.addObserver(completer);
    try {
      await completer.future;
    } finally {
      binding.removeObserver(completer);
    }
  }
}

/// AppLifecycleState.resumed 감지용 내부 옵저버
class _AppLifecycleCompleter extends WidgetsBindingObserver {
  final _completer = Completer<void>();

  Future<void> get future => _completer.future;

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && !_completer.isCompleted) {
      _completer.complete();
    }
  }
}
