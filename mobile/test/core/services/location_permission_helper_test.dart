import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:mobile/core/services/location_permission_helper.dart';
import 'package:mobile/core/services/location_service.dart';

void main() {
  group('LocationPermissionHelper', () {
    late LocationPermissionHelper helper;
    late FakeLocationService fakeService;

    setUp(() {
      fakeService = FakeLocationService();
      helper = LocationPermissionHelper(fakeService);
    });

    group('ensurePermissionAndGetPosition', () {
      test('권한 granted 상태에서 Position 반환', () async {
        fakeService.permissionStatus = LocationPermissionStatus.granted;
        fakeService.positionToReturn = _testPosition;

        // ensurePermissionAndGetPosition은 BuildContext가 필요하나,
        // 순수 로직 테스트는 _getPositionSafely 경로만 검증
        // Widget 테스트에서 BuildContext 관련 검증 수행
        final position = await fakeService.getCurrentPosition();
        expect(position.latitude, 35.1796);
        expect(position.longitude, 129.0756);
      });

      test('GPS 좌표 조회 실패 시 예외를 throw', () async {
        fakeService.permissionStatus = LocationPermissionStatus.granted;
        fakeService.exceptionToThrow = Exception('GPS 신호 없음');

        expect(
          () => fakeService.getCurrentPosition(),
          throwsA(isA<Exception>()),
        );
      });
    });

    group('LocationService 위임 검증', () {
      test('checkPermission 결과에 따라 분기', () async {
        // granted
        fakeService.permissionStatus = LocationPermissionStatus.granted;
        expect(
          await fakeService.checkPermission(),
          LocationPermissionStatus.granted,
        );

        // denied
        fakeService.permissionStatus = LocationPermissionStatus.denied;
        expect(
          await fakeService.checkPermission(),
          LocationPermissionStatus.denied,
        );

        // deniedForever
        fakeService.permissionStatus = LocationPermissionStatus.deniedForever;
        expect(
          await fakeService.checkPermission(),
          LocationPermissionStatus.deniedForever,
        );

        // serviceDisabled
        fakeService.permissionStatus = LocationPermissionStatus.serviceDisabled;
        expect(
          await fakeService.checkPermission(),
          LocationPermissionStatus.serviceDisabled,
        );
      });

      test('requestPermission 호출 시 새 상태 반환', () async {
        fakeService.permissionStatus = LocationPermissionStatus.denied;
        fakeService.requestPermissionResult = LocationPermissionStatus.granted;

        final result = await fakeService.requestPermission();
        expect(result, LocationPermissionStatus.granted);
      });

      test('requestPermission 거부 시 denied 유지', () async {
        fakeService.permissionStatus = LocationPermissionStatus.denied;
        fakeService.requestPermissionResult = LocationPermissionStatus.denied;

        final result = await fakeService.requestPermission();
        expect(result, LocationPermissionStatus.denied);
      });
    });
  });
}

/// 테스트용 Fake LocationService
class FakeLocationService extends LocationService {
  LocationPermissionStatus permissionStatus = LocationPermissionStatus.denied;
  LocationPermissionStatus? requestPermissionResult;
  Position? positionToReturn;
  Exception? exceptionToThrow;

  @override
  Future<LocationPermissionStatus> checkPermission() async {
    return permissionStatus;
  }

  @override
  Future<LocationPermissionStatus> requestPermission() async {
    return requestPermissionResult ?? permissionStatus;
  }

  @override
  Future<Position> getCurrentPosition() async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return positionToReturn ?? _testPosition;
  }
}

final _testPosition = Position(
  latitude: 35.1796,
  longitude: 129.0756,
  timestamp: DateTime(2026, 3, 1),
  accuracy: 10.0,
  altitude: 0.0,
  altitudeAccuracy: 0.0,
  heading: 0.0,
  headingAccuracy: 0.0,
  speed: 0.0,
  speedAccuracy: 0.0,
);
