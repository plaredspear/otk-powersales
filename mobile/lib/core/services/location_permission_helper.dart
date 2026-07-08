import 'dart:async';

import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';

import '../../presentation/widgets/common/location_permission_bottom_sheet.dart';
import 'location_service.dart';

/// 위치 획득 실패 사유
enum LocationFailureReason {
  /// 사용자가 권한 안내 바텀시트에서 취소 (또는 외부 탭으로 닫음)
  cancelled,

  /// 권한 재요청 / 설정 복귀 후에도 권한이 거부 상태
  permissionDenied,

  /// 설정 복귀 후에도 위치 서비스(GPS)가 꺼져 있음
  serviceDisabled,

  /// 권한은 있으나 좌표 조회 실패 (timeout / 신호 없음)
  positionUnavailable,
}

/// [LocationPermissionHelper.ensurePermissionAndGetPosition] 결과
///
/// 성공이면 [position], 실패면 [failureReason] 이 채워진다 (상호 배타).
class LocationAcquisitionResult {
  final Position? position;
  final LocationFailureReason? failureReason;

  const LocationAcquisitionResult.success(Position this.position)
      : failureReason = null;

  const LocationAcquisitionResult.failure(
    LocationFailureReason this.failureReason,
  ) : position = null;

  bool get isSuccess => position != null;
}

/// 출근등록 시 OS 권한을 확인하고 GPS 좌표를 반환하는 헬퍼
///
/// 권한이 없으면 바텀시트를 표시하여 권한 획득을 유도한다.
/// 좌표 획득에 실패하면 사유([LocationFailureReason])가 담긴 실패 결과를
/// 반환한다 (출근등록은 좌표 필수 — 호출 측이 사유별 안내 후 등록 중단).
class LocationPermissionHelper {
  final LocationService _locationService;

  LocationPermissionHelper(this._locationService);

  /// 권한 확인 → 바텀시트 → 좌표 조회
  ///
  /// 반환: 성공(position) 또는 실패(failureReason)
  Future<LocationAcquisitionResult> ensurePermissionAndGetPosition(
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
  Future<LocationAcquisitionResult> _handleDenied(BuildContext context) async {
    final result = await LocationPermissionBottomSheet.show(
      context,
      openSettings: false,
    );
    if (result != true) {
      return const LocationAcquisitionResult.failure(
        LocationFailureReason.cancelled,
      );
    }

    final newStatus = await _locationService.requestPermission();
    return _resolveAfterRetry(newStatus);
  }

  /// deniedForever: 바텀시트 → 앱 설정 → 복귀 시 재확인
  Future<LocationAcquisitionResult> _handleDeniedForever(
    BuildContext context,
  ) async {
    final result = await LocationPermissionBottomSheet.show(
      context,
      openSettings: true,
    );
    if (result != true) {
      return const LocationAcquisitionResult.failure(
        LocationFailureReason.cancelled,
      );
    }

    await Geolocator.openAppSettings();

    // 설정에서 돌아올 때까지 대기
    if (!context.mounted) {
      return const LocationAcquisitionResult.failure(
        LocationFailureReason.permissionDenied,
      );
    }
    await _waitForAppResumed(context);

    final newStatus = await _locationService.checkPermission();
    return _resolveAfterRetry(newStatus);
  }

  /// serviceDisabled: 바텀시트 → 위치 서비스 설정 → 복귀 시 재확인
  Future<LocationAcquisitionResult> _handleServiceDisabled(
    BuildContext context,
  ) async {
    final result = await LocationPermissionBottomSheet.show(
      context,
      openSettings: true,
    );
    if (result != true) {
      return const LocationAcquisitionResult.failure(
        LocationFailureReason.cancelled,
      );
    }

    await Geolocator.openLocationSettings();

    // 설정에서 돌아올 때까지 대기
    if (!context.mounted) {
      return const LocationAcquisitionResult.failure(
        LocationFailureReason.serviceDisabled,
      );
    }
    await _waitForAppResumed(context);

    final newStatus = await _locationService.checkPermission();
    return _resolveAfterRetry(newStatus);
  }

  /// 재요청/설정 복귀 후 상태 → 결과 매핑
  Future<LocationAcquisitionResult> _resolveAfterRetry(
    LocationPermissionStatus status,
  ) async {
    switch (status) {
      case LocationPermissionStatus.granted:
        return _getPositionSafely();
      case LocationPermissionStatus.serviceDisabled:
        return const LocationAcquisitionResult.failure(
          LocationFailureReason.serviceDisabled,
        );
      case LocationPermissionStatus.denied:
      case LocationPermissionStatus.deniedForever:
        return const LocationAcquisitionResult.failure(
          LocationFailureReason.permissionDenied,
        );
    }
  }

  /// GPS 좌표 조회 (예외 → positionUnavailable)
  Future<LocationAcquisitionResult> _getPositionSafely() async {
    try {
      final position = await _locationService.getCurrentPosition();
      return LocationAcquisitionResult.success(position);
    } catch (_) {
      return const LocationAcquisitionResult.failure(
        LocationFailureReason.positionUnavailable,
      );
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
