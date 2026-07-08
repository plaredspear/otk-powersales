import 'package:flutter/material.dart';
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

    /// MaterialApp + Scaffold 를 pump 하고 helper 호출용 context 반환
    Future<BuildContext> pumpHost(WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: Scaffold(body: SizedBox())),
      );
      return tester.element(find.byType(SizedBox));
    }

    group('granted 상태', () {
      testWidgets('좌표 조회 성공 시 success 결과 반환', (tester) async {
        fakeService.permissionStatus = LocationPermissionStatus.granted;
        fakeService.positionToReturn = _testPosition;

        final context = await pumpHost(tester);
        final result = await helper.ensurePermissionAndGetPosition(context);

        expect(result.isSuccess, true);
        expect(result.position!.latitude, 35.1796);
        expect(result.position!.longitude, 129.0756);
        expect(result.failureReason, isNull);
      });

      testWidgets('좌표 조회 실패(timeout 등) 시 positionUnavailable 반환',
          (tester) async {
        fakeService.permissionStatus = LocationPermissionStatus.granted;
        fakeService.exceptionToThrow = Exception('GPS 신호 없음');

        final context = await pumpHost(tester);
        final result = await helper.ensurePermissionAndGetPosition(context);

        expect(result.isSuccess, false);
        expect(
          result.failureReason,
          LocationFailureReason.positionUnavailable,
        );
      });
    });

    group('denied 상태 (바텀시트 → OS 팝업 재요청)', () {
      testWidgets('바텀시트에서 취소 시 cancelled 반환', (tester) async {
        fakeService.permissionStatus = LocationPermissionStatus.denied;

        final context = await pumpHost(tester);
        final future = helper.ensurePermissionAndGetPosition(context);
        await tester.pumpAndSettle();

        expect(find.text('위치 권한이 필요합니다'), findsOneWidget);
        await tester.tap(find.text('취소'));
        await tester.pumpAndSettle();

        final result = await future;
        expect(result.failureReason, LocationFailureReason.cancelled);
      });

      testWidgets('허용 탭 후 OS 팝업에서도 거부 시 permissionDenied 반환',
          (tester) async {
        fakeService.permissionStatus = LocationPermissionStatus.denied;
        fakeService.requestPermissionResult = LocationPermissionStatus.denied;

        final context = await pumpHost(tester);
        final future = helper.ensurePermissionAndGetPosition(context);
        await tester.pumpAndSettle();

        await tester.tap(find.text('권한 허용하기'));
        await tester.pumpAndSettle();

        final result = await future;
        expect(result.failureReason, LocationFailureReason.permissionDenied);
      });

      testWidgets('허용 탭 후 OS 팝업에서 허용 시 success 반환', (tester) async {
        fakeService.permissionStatus = LocationPermissionStatus.denied;
        fakeService.requestPermissionResult = LocationPermissionStatus.granted;
        fakeService.positionToReturn = _testPosition;

        final context = await pumpHost(tester);
        final future = helper.ensurePermissionAndGetPosition(context);
        await tester.pumpAndSettle();

        await tester.tap(find.text('권한 허용하기'));
        await tester.pumpAndSettle();

        final result = await future;
        expect(result.isSuccess, true);
        expect(result.position!.latitude, 35.1796);
      });

      testWidgets('허용 탭 후 위치 서비스 꺼짐이면 serviceDisabled 반환',
          (tester) async {
        fakeService.permissionStatus = LocationPermissionStatus.denied;
        fakeService.requestPermissionResult =
            LocationPermissionStatus.serviceDisabled;

        final context = await pumpHost(tester);
        final future = helper.ensurePermissionAndGetPosition(context);
        await tester.pumpAndSettle();

        await tester.tap(find.text('권한 허용하기'));
        await tester.pumpAndSettle();

        final result = await future;
        expect(result.failureReason, LocationFailureReason.serviceDisabled);
      });
    });

    group('deniedForever 상태 (바텀시트 → 앱 설정 이동)', () {
      testWidgets('바텀시트에서 취소 시 cancelled 반환 (설정 미이동)',
          (tester) async {
        fakeService.permissionStatus = LocationPermissionStatus.deniedForever;

        final context = await pumpHost(tester);
        final future = helper.ensurePermissionAndGetPosition(context);
        await tester.pumpAndSettle();

        expect(find.text('설정으로 이동'), findsOneWidget);
        await tester.tap(find.text('취소'));
        await tester.pumpAndSettle();

        final result = await future;
        expect(result.failureReason, LocationFailureReason.cancelled);
      });
    });

    group('serviceDisabled 상태 (바텀시트 → 위치 서비스 설정 이동)', () {
      testWidgets('바텀시트에서 취소 시 cancelled 반환 (설정 미이동)',
          (tester) async {
        fakeService.permissionStatus =
            LocationPermissionStatus.serviceDisabled;

        final context = await pumpHost(tester);
        final future = helper.ensurePermissionAndGetPosition(context);
        await tester.pumpAndSettle();

        expect(find.text('설정으로 이동'), findsOneWidget);
        await tester.tap(find.text('취소'));
        await tester.pumpAndSettle();

        final result = await future;
        expect(result.failureReason, LocationFailureReason.cancelled);
      });
    });
  });

  group('LocationAcquisitionResult', () {
    test('success 는 position 보유 + failureReason null', () {
      final result = LocationAcquisitionResult.success(_testPosition);
      expect(result.isSuccess, true);
      expect(result.position, _testPosition);
      expect(result.failureReason, isNull);
    });

    test('failure 는 failureReason 보유 + position null', () {
      const result = LocationAcquisitionResult.failure(
        LocationFailureReason.permissionDenied,
      );
      expect(result.isSuccess, false);
      expect(result.position, isNull);
      expect(result.failureReason, LocationFailureReason.permissionDenied);
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
