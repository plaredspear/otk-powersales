import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:geolocator_platform_interface/geolocator_platform_interface.dart';
import 'package:mobile/core/services/location_service.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

void main() {
  late LocationService service;
  late MockGeolocatorPlatform mockPlatform;

  setUp(() {
    mockPlatform = MockGeolocatorPlatform();
    GeolocatorPlatform.instance = mockPlatform;
    service = LocationService();
  });

  group('checkPermission', () {
    test('위치 서비스 비활성화 시 serviceDisabled 반환', () async {
      mockPlatform.serviceEnabled = false;

      final result = await service.checkPermission();

      expect(result, LocationPermissionStatus.serviceDisabled);
    });

    test('권한 denied 시 denied 반환', () async {
      mockPlatform.serviceEnabled = true;
      mockPlatform.permissionToReturn = LocationPermission.denied;

      final result = await service.checkPermission();

      expect(result, LocationPermissionStatus.denied);
    });

    test('권한 deniedForever 시 deniedForever 반환', () async {
      mockPlatform.serviceEnabled = true;
      mockPlatform.permissionToReturn = LocationPermission.deniedForever;

      final result = await service.checkPermission();

      expect(result, LocationPermissionStatus.deniedForever);
    });

    test('권한 whileInUse 시 granted 반환', () async {
      mockPlatform.serviceEnabled = true;
      mockPlatform.permissionToReturn = LocationPermission.whileInUse;

      final result = await service.checkPermission();

      expect(result, LocationPermissionStatus.granted);
    });

    test('권한 always 시 granted 반환', () async {
      mockPlatform.serviceEnabled = true;
      mockPlatform.permissionToReturn = LocationPermission.always;

      final result = await service.checkPermission();

      expect(result, LocationPermissionStatus.granted);
    });
  });

  group('requestPermission', () {
    test('이미 granted이면 granted 그대로 반환', () async {
      mockPlatform.serviceEnabled = true;
      mockPlatform.permissionToReturn = LocationPermission.whileInUse;

      final result = await service.requestPermission();

      expect(result, LocationPermissionStatus.granted);
      expect(mockPlatform.requestPermissionCalled, false);
    });

    test('deniedForever이면 OS 팝업 없이 deniedForever 반환', () async {
      mockPlatform.serviceEnabled = true;
      mockPlatform.permissionToReturn = LocationPermission.deniedForever;

      final result = await service.requestPermission();

      expect(result, LocationPermissionStatus.deniedForever);
      expect(mockPlatform.requestPermissionCalled, false);
    });

    test('serviceDisabled이면 serviceDisabled 반환', () async {
      mockPlatform.serviceEnabled = false;

      final result = await service.requestPermission();

      expect(result, LocationPermissionStatus.serviceDisabled);
      expect(mockPlatform.requestPermissionCalled, false);
    });

    test('denied이면 권한 요청 후 결과 반환', () async {
      mockPlatform.serviceEnabled = true;
      mockPlatform.permissionToReturn = LocationPermission.denied;
      mockPlatform.requestPermissionResult = LocationPermission.whileInUse;

      final result = await service.requestPermission();

      expect(result, LocationPermissionStatus.granted);
      expect(mockPlatform.requestPermissionCalled, true);
    });

    test('denied에서 요청 후 사용자 거부 시 denied 반환', () async {
      mockPlatform.serviceEnabled = true;
      mockPlatform.permissionToReturn = LocationPermission.denied;
      mockPlatform.requestPermissionResult = LocationPermission.denied;

      final result = await service.requestPermission();

      expect(result, LocationPermissionStatus.denied);
      expect(mockPlatform.requestPermissionCalled, true);
    });
  });

  group('getCurrentPosition', () {
    test('정상 조회 시 Position 반환', () async {
      mockPlatform.serviceEnabled = true;
      mockPlatform.permissionToReturn = LocationPermission.whileInUse;
      mockPlatform.positionToReturn = _testPosition;

      final result = await service.getCurrentPosition();

      expect(result.latitude, 37.5665);
      expect(result.longitude, 126.9780);
    });

    test('위치 서비스 비활성화 시 LocationServiceDisabledException throw', () async {
      mockPlatform.serviceEnabled = false;

      expect(
        () => service.getCurrentPosition(),
        throwsA(isA<LocationServiceDisabledException>()),
      );
    });

    test('권한 denied 시 PermissionDeniedException throw', () async {
      mockPlatform.serviceEnabled = true;
      mockPlatform.permissionToReturn = LocationPermission.denied;

      expect(
        () => service.getCurrentPosition(),
        throwsA(isA<PermissionDeniedException>()),
      );
    });

    test('권한 deniedForever 시 PermissionDeniedException throw', () async {
      mockPlatform.serviceEnabled = true;
      mockPlatform.permissionToReturn = LocationPermission.deniedForever;

      expect(
        () => service.getCurrentPosition(),
        throwsA(isA<PermissionDeniedException>()),
      );
    });
  });
}

// --- Mock ---

class MockGeolocatorPlatform extends GeolocatorPlatform
    with MockPlatformInterfaceMixin {
  bool serviceEnabled = true;
  LocationPermission permissionToReturn = LocationPermission.denied;
  LocationPermission requestPermissionResult = LocationPermission.denied;
  Position? positionToReturn;
  bool requestPermissionCalled = false;

  @override
  Future<bool> isLocationServiceEnabled() async => serviceEnabled;

  @override
  Future<LocationPermission> checkPermission() async => permissionToReturn;

  @override
  Future<LocationPermission> requestPermission() async {
    requestPermissionCalled = true;
    return requestPermissionResult;
  }

  @override
  Future<Position> getCurrentPosition({
    LocationSettings? locationSettings,
  }) async {
    if (positionToReturn != null) return positionToReturn!;
    throw const LocationServiceDisabledException();
  }
}

// --- Test Data ---

final _testPosition = Position(
  latitude: 37.5665,
  longitude: 126.9780,
  accuracy: 10.0,
  altitude: 0.0,
  altitudeAccuracy: 0.0,
  heading: 0.0,
  headingAccuracy: 0.0,
  speed: 0.0,
  speedAccuracy: 0.0,
  timestamp: DateTime(2026, 2, 26),
);
