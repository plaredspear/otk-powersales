import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/safety_check_today_status.dart';

void main() {
  group('SafetyCheckTodayStatus', () {
    group('Creation', () {
      test('creates with completed=false and submittedAt=null', () {
        // Arrange & Act
        const status = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );

        // Assert
        expect(status.completed, false);
        expect(status.submittedAt, isNull);
      });

      test('creates with completed=true and submittedAt set', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);

        // Act
        final status = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: submittedTime,
        );

        // Assert
        expect(status.completed, true);
        expect(status.submittedAt, submittedTime);
      });

      test('creates with completed=true but submittedAt=null', () {
        // Arrange & Act
        const status = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: null,
        );

        // Assert
        expect(status.completed, true);
        expect(status.submittedAt, isNull);
      });
    });

    group('copyWith', () {
      test('returns new instance with updated completed', () {
        // Arrange
        const original = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );

        // Act
        final updated = original.copyWith(completed: true);

        // Assert
        expect(updated.completed, true);
        expect(updated.submittedAt, original.submittedAt);
      });

      test('returns new instance with updated submittedAt', () {
        // Arrange
        const original = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );
        final newTime = DateTime(2024, 1, 15, 10, 0, 0);

        // Act
        final updated = original.copyWith(submittedAt: newTime);

        // Assert
        expect(updated.submittedAt, newTime);
        expect(updated.completed, original.completed);
      });

      test('returns new instance with both fields updated', () {
        // Arrange
        const original = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );
        final newTime = DateTime(2024, 1, 15, 10, 0, 0);

        // Act
        final updated = original.copyWith(
          completed: true,
          submittedAt: newTime,
        );

        // Assert
        expect(updated.completed, true);
        expect(updated.submittedAt, newTime);
      });

      test('returns identical instance when no fields provided', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final original = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: submittedTime,
        );

        // Act
        final copied = original.copyWith();

        // Assert
        expect(copied.completed, original.completed);
        expect(copied.submittedAt, original.submittedAt);
      });
    });

    group('Serialization', () {
      test('toJson returns correct map when completed=false', () {
        // Arrange
        const status = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );

        // Act
        final json = status.toJson();

        // Assert
        expect(json, {
          'completed': false,
          'submittedAt': null,
        });
      });

      test('toJson returns correct map when completed=true with submittedAt',
          () {
        // Arrange
        final submittedTime = DateTime.utc(2024, 1, 15, 9, 30, 0);
        final status = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: submittedTime,
        );

        // Act
        final json = status.toJson();

        // Assert
        expect(json['completed'], true);
        expect(json['submittedAt'], '2024-01-15T09:30:00.000Z');
      });

      test('fromJson creates entity from map when completed=false', () {
        // Arrange
        final json = {
          'completed': false,
          'submittedAt': null,
        };

        // Act
        final status = SafetyCheckTodayStatus.fromJson(json);

        // Assert
        expect(status.completed, false);
        expect(status.submittedAt, isNull);
      });

      test('fromJson creates entity from map when completed=true with submittedAt',
          () {
        // Arrange
        final json = {
          'completed': true,
          'submittedAt': '2024-01-15T09:30:00.000Z',
        };

        // Act
        final status = SafetyCheckTodayStatus.fromJson(json);

        // Assert
        expect(status.completed, true);
        expect(status.submittedAt, DateTime.utc(2024, 1, 15, 9, 30, 0));
      });

      test('toJson and fromJson round-trip preserves data with null submittedAt',
          () {
        // Arrange
        const original = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );

        // Act
        final json = original.toJson();
        final restored = SafetyCheckTodayStatus.fromJson(json);

        // Assert
        expect(restored, original);
      });

      test('toJson and fromJson round-trip preserves data with submittedAt',
          () {
        // Arrange
        final submittedTime = DateTime.utc(2024, 1, 15, 9, 30, 0);
        final original = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: submittedTime,
        );

        // Act
        final json = original.toJson();
        final restored = SafetyCheckTodayStatus.fromJson(json);

        // Assert
        expect(restored.completed, original.completed);
        expect(restored.submittedAt, original.submittedAt);
      });

      test('fromJson handles missing submittedAt field', () {
        // Arrange
        final json = {
          'completed': true,
        };

        // Act
        final status = SafetyCheckTodayStatus.fromJson(json);

        // Assert
        expect(status.completed, true);
        expect(status.submittedAt, isNull);
      });
    });

    group('Equality', () {
      test('two entities with same values are equal', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final status1 = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: submittedTime,
        );
        final status2 = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: submittedTime,
        );

        // Assert
        expect(status1, status2);
      });

      test('two entities with both null submittedAt are equal', () {
        // Arrange
        const status1 = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );
        const status2 = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );

        // Assert
        expect(status1, status2);
      });

      test('two entities with different completed values are not equal', () {
        // Arrange
        const status1 = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );
        const status2 = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: null,
        );

        // Assert
        expect(status1, isNot(status2));
      });

      test('two entities with different submittedAt values are not equal', () {
        // Arrange
        final time1 = DateTime(2024, 1, 15, 9, 30, 0);
        final time2 = DateTime(2024, 1, 15, 10, 0, 0);
        final status1 = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: time1,
        );
        final status2 = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: time2,
        );

        // Assert
        expect(status1, isNot(status2));
      });

      test('entity with null submittedAt is not equal to entity with submittedAt',
          () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        const status1 = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: null,
        );
        final status2 = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: submittedTime,
        );

        // Assert
        expect(status1, isNot(status2));
      });

      test('hashCode is consistent for equal entities', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final status1 = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: submittedTime,
        );
        final status2 = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: submittedTime,
        );

        // Assert
        expect(status1.hashCode, status2.hashCode);
      });

      test('hashCode differs for different entities', () {
        // Arrange
        const status1 = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );
        const status2 = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: null,
        );

        // Assert
        expect(status1.hashCode, isNot(status2.hashCode));
      });

      test('identical instances are equal', () {
        // Arrange
        const status = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );

        // Assert
        // ignore: unrelated_type_equality_checks
        expect(status == status, true);
      });
    });

    group('toString', () {
      test('toString contains all field values when completed=false', () {
        // Arrange
        const status = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );

        // Act
        final string = status.toString();

        // Assert
        expect(string, contains('SafetyCheckTodayStatus'));
        expect(string, contains('completed: false'));
        expect(string, contains('submittedAt: null'));
      });

      test('toString contains all field values when completed=true', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final status = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: submittedTime,
        );

        // Act
        final string = status.toString();

        // Assert
        expect(string, contains('SafetyCheckTodayStatus'));
        expect(string, contains('completed: true'));
        expect(string, contains('submittedAt:'));
        expect(string, contains('2024-01-15 09:30:00'));
      });

      test('toString format matches expected pattern', () {
        // Arrange
        const status = SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );

        // Act
        final string = status.toString();

        // Assert
        expect(
          string,
          'SafetyCheckTodayStatus(completed: false, submittedAt: null)',
        );
      });
    });
  });
}
