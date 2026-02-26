import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';

/// 위치 권한 상태
enum LocationPermissionStatus {
  /// 권한 허용됨
  granted,

  /// 권한 거부됨 (재요청 가능)
  denied,

  /// 권한 영구 거부됨 (설정에서 수동 변경 필요)
  deniedForever,

  /// 위치 서비스(GPS) 자체가 꺼져 있음
  serviceDisabled,
}

/// 디바이스 GPS 위치 권한 요청 및 좌표 조회 서비스
class LocationService {
  /// 현재 위치 권한 상태 확인 (OS 팝업 없음)
  Future<LocationPermissionStatus> checkPermission() async {
    final serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return LocationPermissionStatus.serviceDisabled;
    }

    final permission = await Geolocator.checkPermission();
    return _toStatus(permission);
  }

  /// 런타임 권한 요청 (OS 팝업 표시)
  Future<LocationPermissionStatus> requestPermission() async {
    final current = await checkPermission();
    if (current == LocationPermissionStatus.granted) {
      return LocationPermissionStatus.granted;
    }
    if (current == LocationPermissionStatus.deniedForever) {
      return LocationPermissionStatus.deniedForever;
    }
    if (current == LocationPermissionStatus.serviceDisabled) {
      return LocationPermissionStatus.serviceDisabled;
    }

    final permission = await Geolocator.requestPermission();
    return _toStatus(permission);
  }

  /// 현재 GPS 좌표 조회
  Future<Position> getCurrentPosition() async {
    final serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      throw const LocationServiceDisabledException();
    }

    final permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied ||
        permission == LocationPermission.deniedForever) {
      throw PermissionDeniedException(permission.toString());
    }

    return await Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(
        timeLimit: Duration(seconds: 10),
      ),
    );
  }

  LocationPermissionStatus _toStatus(LocationPermission permission) {
    switch (permission) {
      case LocationPermission.whileInUse:
      case LocationPermission.always:
        return LocationPermissionStatus.granted;
      case LocationPermission.denied:
      case LocationPermission.unableToDetermine:
        return LocationPermissionStatus.denied;
      case LocationPermission.deniedForever:
        return LocationPermissionStatus.deniedForever;
    }
  }
}

/// LocationService Riverpod Provider
final locationServiceProvider = Provider<LocationService>((ref) {
  return LocationService();
});
