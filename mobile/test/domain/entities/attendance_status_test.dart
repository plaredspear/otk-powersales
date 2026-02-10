import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_status.dart';

void main() {
  group('AttendanceStatus', () {
    final testDateTime = DateTime(2024, 1, 15, 9, 30);

    test('AttendanceStatus가 모든 필드와 함께 생성된다', () {
      // Arrange & Act
      final status = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'COMPLETED',
        workType: '방문',
        registeredAt: testDateTime,
      );

      // Assert
      expect(status.storeId, 100);
      expect(status.storeName, '테스트 거래처');
      expect(status.status, 'COMPLETED');
      expect(status.workType, '방문');
      expect(status.registeredAt, testDateTime);
    });

    test('AttendanceStatus가 nullable 필드를 null로 생성할 수 있다', () {
      // Arrange & Act
      const status = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'PENDING',
      );

      // Assert
      expect(status.storeId, 100);
      expect(status.storeName, '테스트 거래처');
      expect(status.status, 'PENDING');
      expect(status.workType, isNull);
      expect(status.registeredAt, isNull);
    });

    test('copyWith는 일부 필드만 변경할 수 있다', () {
      // Arrange
      const original = AttendanceStatus(
        storeId: 100,
        storeName: '원본 거래처',
        status: 'PENDING',
      );

      // Act
      final copied = original.copyWith(
        status: 'COMPLETED',
        workType: '방문',
        registeredAt: testDateTime,
      );

      // Assert
      expect(copied.storeId, 100);
      expect(copied.storeName, '원본 거래처');
      expect(copied.status, 'COMPLETED');
      expect(copied.workType, '방문');
      expect(copied.registeredAt, testDateTime);
    });

    test('copyWith는 원본 객체를 변경하지 않는다 (불변성)', () {
      // Arrange
      const original = AttendanceStatus(
        storeId: 100,
        storeName: '원본 거래처',
        status: 'PENDING',
      );

      // Act
      final copied = original.copyWith(status: 'COMPLETED');

      // Assert
      expect(original.status, 'PENDING');
      expect(copied.status, 'COMPLETED');
    });

    test('toJson과 fromJson은 양방향 변환이 가능하다', () {
      // Arrange
      final original = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'COMPLETED',
        workType: '방문',
        registeredAt: testDateTime,
      );

      // Act
      final json = original.toJson();
      final restored = AttendanceStatus.fromJson(json);

      // Assert
      expect(restored, original);
      expect(json['storeId'], 100);
      expect(json['storeName'], '테스트 거래처');
      expect(json['status'], 'COMPLETED');
      expect(json['workType'], '방문');
      expect(json['registeredAt'], testDateTime.toIso8601String());
    });

    test('fromJson은 null workType과 registeredAt을 처리한다', () {
      // Arrange
      final json = {
        'storeId': 100,
        'storeName': '테스트 거래처',
        'status': 'PENDING',
        'workType': null,
        'registeredAt': null,
      };

      // Act
      final status = AttendanceStatus.fromJson(json);

      // Assert
      expect(status.storeId, 100);
      expect(status.storeName, '테스트 거래처');
      expect(status.status, 'PENDING');
      expect(status.workType, isNull);
      expect(status.registeredAt, isNull);
    });

    test('isCompleted는 status가 COMPLETED일 때 true다', () {
      // Arrange
      const status = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'COMPLETED',
        workType: '방문',
      );

      // Act & Assert
      expect(status.isCompleted, true);
    });

    test('isCompleted는 status가 COMPLETED가 아닐 때 false다', () {
      // Arrange
      const status = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'PENDING',
      );

      // Act & Assert
      expect(status.isCompleted, false);
    });

    test('isPending은 status가 PENDING일 때 true다', () {
      // Arrange
      const status = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'PENDING',
      );

      // Act & Assert
      expect(status.isPending, true);
    });

    test('isPending은 status가 PENDING이 아닐 때 false다', () {
      // Arrange
      const status = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'COMPLETED',
        workType: '방문',
      );

      // Act & Assert
      expect(status.isPending, false);
    });

    test('같은 값을 가진 객체는 동일하다', () {
      // Arrange
      final status1 = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'COMPLETED',
        workType: '방문',
        registeredAt: testDateTime,
      );
      final status2 = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'COMPLETED',
        workType: '방문',
        registeredAt: testDateTime,
      );

      // Act & Assert
      expect(status1, status2);
    });

    test('다른 값을 가진 객체는 동일하지 않다', () {
      // Arrange
      const status1 = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'COMPLETED',
        workType: '방문',
      );
      const status2 = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'PENDING',
      );

      // Act & Assert
      expect(status1, isNot(status2));
    });

    test('같은 객체는 같은 hashCode를 가진다', () {
      // Arrange
      final status1 = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'COMPLETED',
        workType: '방문',
        registeredAt: testDateTime,
      );
      final status2 = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'COMPLETED',
        workType: '방문',
        registeredAt: testDateTime,
      );

      // Act & Assert
      expect(status1.hashCode, status2.hashCode);
    });

    test('toString은 관련 정보를 포함한다', () {
      // Arrange
      final status = AttendanceStatus(
        storeId: 100,
        storeName: '테스트 거래처',
        status: 'COMPLETED',
        workType: '방문',
        registeredAt: testDateTime,
      );

      // Act
      final result = status.toString();

      // Assert
      expect(result, contains('AttendanceStatus'));
      expect(result, contains('storeId: 100'));
      expect(result, contains('storeName: 테스트 거래처'));
      expect(result, contains('status: COMPLETED'));
      expect(result, contains('workType: 방문'));
      expect(result, contains('registeredAt: $testDateTime'));
    });
  });
}
