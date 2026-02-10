import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_result.dart';

void main() {
  group('AttendanceResult', () {
    final testDateTime = DateTime(2024, 1, 15, 9, 30);

    test('AttendanceResult가 올바르게 생성된다', () {
      // Arrange & Act
      final result = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 3,
      );

      // Assert
      expect(result.attendanceId, 1);
      expect(result.storeId, 100);
      expect(result.storeName, '테스트 거래처');
      expect(result.workType, '방문');
      expect(result.registeredAt, testDateTime);
      expect(result.totalCount, 5);
      expect(result.registeredCount, 3);
    });

    test('copyWith는 일부 필드만 변경할 수 있다', () {
      // Arrange
      final original = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '원본 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 3,
      );

      // Act
      final copied = original.copyWith(
        storeName: '변경된 거래처',
        registeredCount: 4,
      );

      // Assert
      expect(copied.attendanceId, 1);
      expect(copied.storeId, 100);
      expect(copied.storeName, '변경된 거래처');
      expect(copied.workType, '방문');
      expect(copied.registeredAt, testDateTime);
      expect(copied.totalCount, 5);
      expect(copied.registeredCount, 4);
    });

    test('copyWith는 원본 객체를 변경하지 않는다 (불변성)', () {
      // Arrange
      final original = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '원본 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 3,
      );

      // Act
      final copied = original.copyWith(registeredCount: 5);

      // Assert
      expect(original.registeredCount, 3);
      expect(copied.registeredCount, 5);
    });

    test('toJson과 fromJson은 양방향 변환이 가능하다', () {
      // Arrange
      final original = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 3,
      );

      // Act
      final json = original.toJson();
      final restored = AttendanceResult.fromJson(json);

      // Assert
      expect(restored, original);
      expect(json['attendanceId'], 1);
      expect(json['storeId'], 100);
      expect(json['storeName'], '테스트 거래처');
      expect(json['workType'], '방문');
      expect(json['registeredAt'], testDateTime.toIso8601String());
      expect(json['totalCount'], 5);
      expect(json['registeredCount'], 3);
    });

    test('isAllRegistered는 registeredCount가 totalCount와 같을 때 true다', () {
      // Arrange
      final result = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 5,
      );

      // Act & Assert
      expect(result.isAllRegistered, true);
    });

    test('isAllRegistered는 registeredCount가 totalCount보다 클 때 true다', () {
      // Arrange
      final result = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 6,
      );

      // Act & Assert
      expect(result.isAllRegistered, true);
    });

    test('isAllRegistered는 registeredCount가 totalCount보다 작을 때 false다', () {
      // Arrange
      final result = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 3,
      );

      // Act & Assert
      expect(result.isAllRegistered, false);
    });

    test('remainingCount는 남은 거래처 수를 정확히 계산한다', () {
      // Arrange
      final result = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 10,
        registeredCount: 3,
      );

      // Act & Assert
      expect(result.remainingCount, 7);
    });

    test('remainingCount는 모두 등록되면 0이다', () {
      // Arrange
      final result = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 5,
      );

      // Act & Assert
      expect(result.remainingCount, 0);
    });

    test('같은 값을 가진 객체는 동일하다', () {
      // Arrange
      final result1 = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 3,
      );
      final result2 = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 3,
      );

      // Act & Assert
      expect(result1, result2);
    });

    test('다른 값을 가진 객체는 동일하지 않다', () {
      // Arrange
      final result1 = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 3,
      );
      final result2 = AttendanceResult(
        attendanceId: 2,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 3,
      );

      // Act & Assert
      expect(result1, isNot(result2));
    });

    test('같은 객체는 같은 hashCode를 가진다', () {
      // Arrange
      final result1 = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 3,
      );
      final result2 = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 3,
      );

      // Act & Assert
      expect(result1.hashCode, result2.hashCode);
    });

    test('toString은 관련 정보를 포함한다', () {
      // Arrange
      final result = AttendanceResult(
        attendanceId: 1,
        storeId: 100,
        storeName: '테스트 거래처',
        workType: '방문',
        registeredAt: testDateTime,
        totalCount: 5,
        registeredCount: 3,
      );

      // Act
      final stringResult = result.toString();

      // Assert
      expect(stringResult, contains('AttendanceResult'));
      expect(stringResult, contains('attendanceId: 1'));
      expect(stringResult, contains('storeId: 100'));
      expect(stringResult, contains('storeName: 테스트 거래처'));
      expect(stringResult, contains('workType: 방문'));
      expect(stringResult, contains('registeredAt: $testDateTime'));
      expect(stringResult, contains('totalCount: 5'));
      expect(stringResult, contains('registeredCount: 3'));
    });
  });
}
