import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/safety_check_submit_result.dart';

void main() {
  group('SafetyCheckSubmitResult', () {
    group('Creation', () {
      test('creates correctly with all fields', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);

        // Act
        final result = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );

        // Assert
        expect(result.submissionId, 12345);
        expect(result.submittedAt, submittedTime);
        expect(result.safetyCheckCompleted, true);
      });

      test('creates correctly with safetyCheckCompleted=false', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);

        // Act
        final result = SafetyCheckSubmitResult(
          submissionId: 67890,
          submittedAt: submittedTime,
          safetyCheckCompleted: false,
        );

        // Assert
        expect(result.safetyCheckCompleted, false);
      });
    });

    group('copyWith', () {
      test('returns new instance with updated submissionId', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final original = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );

        // Act
        final updated = original.copyWith(submissionId: 99999);

        // Assert
        expect(updated.submissionId, 99999);
        expect(updated.submittedAt, original.submittedAt);
        expect(updated.safetyCheckCompleted, original.safetyCheckCompleted);
      });

      test('returns new instance with updated submittedAt', () {
        // Arrange
        final originalTime = DateTime(2024, 1, 15, 9, 30, 0);
        final newTime = DateTime(2024, 1, 15, 10, 0, 0);
        final original = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: originalTime,
          safetyCheckCompleted: true,
        );

        // Act
        final updated = original.copyWith(submittedAt: newTime);

        // Assert
        expect(updated.submittedAt, newTime);
        expect(updated.submissionId, original.submissionId);
        expect(updated.safetyCheckCompleted, original.safetyCheckCompleted);
      });

      test('returns new instance with updated safetyCheckCompleted', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final original = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );

        // Act
        final updated = original.copyWith(safetyCheckCompleted: false);

        // Assert
        expect(updated.safetyCheckCompleted, false);
        expect(updated.submissionId, original.submissionId);
        expect(updated.submittedAt, original.submittedAt);
      });

      test('returns new instance with all fields updated', () {
        // Arrange
        final originalTime = DateTime(2024, 1, 15, 9, 30, 0);
        final newTime = DateTime(2024, 1, 15, 10, 0, 0);
        final original = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: originalTime,
          safetyCheckCompleted: true,
        );

        // Act
        final updated = original.copyWith(
          submissionId: 67890,
          submittedAt: newTime,
          safetyCheckCompleted: false,
        );

        // Assert
        expect(updated.submissionId, 67890);
        expect(updated.submittedAt, newTime);
        expect(updated.safetyCheckCompleted, false);
      });

      test('returns identical instance when no fields provided', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final original = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );

        // Act
        final copied = original.copyWith();

        // Assert
        expect(copied.submissionId, original.submissionId);
        expect(copied.submittedAt, original.submittedAt);
        expect(copied.safetyCheckCompleted, original.safetyCheckCompleted);
      });
    });

    group('Serialization', () {
      test('toJson returns correct map', () {
        // Arrange
        final submittedTime = DateTime.utc(2024, 1, 15, 9, 30, 0);
        final result = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );

        // Act
        final json = result.toJson();

        // Assert
        expect(json, {
          'submissionId': 12345,
          'submittedAt': '2024-01-15T09:30:00.000Z',
          'safetyCheckCompleted': true,
        });
      });

      test('fromJson creates entity from map correctly', () {
        // Arrange
        final json = {
          'submissionId': 67890,
          'submittedAt': '2024-01-15T10:00:00.000Z',
          'safetyCheckCompleted': false,
        };

        // Act
        final result = SafetyCheckSubmitResult.fromJson(json);

        // Assert
        expect(result.submissionId, 67890);
        expect(result.submittedAt, DateTime.utc(2024, 1, 15, 10, 0, 0));
        expect(result.safetyCheckCompleted, false);
      });

      test('toJson and fromJson round-trip preserves data', () {
        // Arrange
        final submittedTime = DateTime.utc(2024, 1, 15, 9, 30, 0);
        final original = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );

        // Act
        final json = original.toJson();
        final restored = SafetyCheckSubmitResult.fromJson(json);

        // Assert
        expect(restored.submissionId, original.submissionId);
        expect(restored.submittedAt, original.submittedAt);
        expect(restored.safetyCheckCompleted, original.safetyCheckCompleted);
      });

      test('fromJson handles DateTime with milliseconds', () {
        // Arrange
        final json = {
          'submissionId': 11111,
          'submittedAt': '2024-01-15T09:30:45.123Z',
          'safetyCheckCompleted': true,
        };

        // Act
        final result = SafetyCheckSubmitResult.fromJson(json);

        // Assert
        expect(result.submittedAt, DateTime.utc(2024, 1, 15, 9, 30, 45, 123));
      });
    });

    group('Equality', () {
      test('two entities with same values are equal', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final result1 = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );
        final result2 = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );

        // Assert
        expect(result1, result2);
      });

      test('two entities with different submissionIds are not equal', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final result1 = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );
        final result2 = SafetyCheckSubmitResult(
          submissionId: 67890,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );

        // Assert
        expect(result1, isNot(result2));
      });

      test('two entities with different submittedAt values are not equal', () {
        // Arrange
        final time1 = DateTime(2024, 1, 15, 9, 30, 0);
        final time2 = DateTime(2024, 1, 15, 10, 0, 0);
        final result1 = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: time1,
          safetyCheckCompleted: true,
        );
        final result2 = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: time2,
          safetyCheckCompleted: true,
        );

        // Assert
        expect(result1, isNot(result2));
      });

      test('two entities with different safetyCheckCompleted values are not equal',
          () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final result1 = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );
        final result2 = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: false,
        );

        // Assert
        expect(result1, isNot(result2));
      });

      test('hashCode is consistent for equal entities', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final result1 = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );
        final result2 = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );

        // Assert
        expect(result1.hashCode, result2.hashCode);
      });

      test('hashCode differs for different entities', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final result1 = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );
        final result2 = SafetyCheckSubmitResult(
          submissionId: 67890,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );

        // Assert
        expect(result1.hashCode, isNot(result2.hashCode));
      });

      test('identical instances are equal', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final result = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );

        // Assert
        // ignore: unrelated_type_equality_checks
        expect(result == result, true);
      });
    });

    group('toString', () {
      test('toString contains all field values', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final result = SafetyCheckSubmitResult(
          submissionId: 12345,
          submittedAt: submittedTime,
          safetyCheckCompleted: true,
        );

        // Act
        final string = result.toString();

        // Assert
        expect(string, contains('SafetyCheckSubmitResult'));
        expect(string, contains('submissionId: 12345'));
        expect(string, contains('submittedAt:'));
        expect(string, contains('2024-01-15 09:30:00'));
        expect(string, contains('safetyCheckCompleted: true'));
      });

      test('toString format matches expected pattern', () {
        // Arrange
        final submittedTime = DateTime(2024, 1, 15, 9, 30, 0);
        final result = SafetyCheckSubmitResult(
          submissionId: 99999,
          submittedAt: submittedTime,
          safetyCheckCompleted: false,
        );

        // Act
        final string = result.toString();

        // Assert
        expect(
          string,
          'SafetyCheckSubmitResult(submissionId: 99999, submittedAt: 2024-01-15 09:30:00.000, safetyCheckCompleted: false)',
        );
      });
    });
  });
}
