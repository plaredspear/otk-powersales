import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/safety_check_today_status_model.dart';
import 'package:mobile/domain/entities/safety_check_today_status.dart';

void main() {
  group('SafetyCheckTodayStatusModel', () {
    final tDateTime = DateTime.parse('2026-02-08T09:00:00.000');

    final tSafetyCheckTodayStatusModelCompleted = SafetyCheckTodayStatusModel(
      completed: true,
      submittedAt: tDateTime,
    );

    const tSafetyCheckTodayStatusModelNotCompleted = SafetyCheckTodayStatusModel(
      completed: false,
      submittedAt: null,
    );

    final tSafetyCheckTodayStatusCompleted = SafetyCheckTodayStatus(
      completed: true,
      submittedAt: tDateTime,
    );

    const tSafetyCheckTodayStatusNotCompleted = SafetyCheckTodayStatus(
      completed: false,
      submittedAt: null,
    );

    final tJsonCompleted = {
      'completed': true,
      'submitted_at': '2026-02-08T09:00:00.000',
    };

    final tJsonNotCompleted = {
      'completed': false,
      'submitted_at': null,
    };

    group('fromJson', () {
      test('should parse with completed=false and submitted_at=null', () {
        // act
        final result = SafetyCheckTodayStatusModel.fromJson(tJsonNotCompleted);

        // assert
        expect(result, tSafetyCheckTodayStatusModelNotCompleted);
        expect(result.completed, false);
        expect(result.submittedAt, null);
      });

      test('should parse with completed=true and submitted_at set (ISO 8601 string)', () {
        // act
        final result = SafetyCheckTodayStatusModel.fromJson(tJsonCompleted);

        // assert
        expect(result, tSafetyCheckTodayStatusModelCompleted);
        expect(result.completed, true);
        expect(result.submittedAt, tDateTime);
      });

      test('should handle various ISO 8601 date formats', () {
        // arrange
        final jsonWithZ = {
          'completed': true,
          'submitted_at': '2026-02-08T09:00:00.000Z',
        };

        // act
        final result = SafetyCheckTodayStatusModel.fromJson(jsonWithZ);

        // assert
        expect(result.completed, true);
        expect(result.submittedAt, isNotNull);
      });
    });

    group('toJson', () {
      test('should output snake_case with null submitted_at', () {
        // act
        final result = tSafetyCheckTodayStatusModelNotCompleted.toJson();

        // assert
        expect(result, tJsonNotCompleted);
      });

      test('should output snake_case with ISO 8601 submitted_at', () {
        // act
        final result = tSafetyCheckTodayStatusModelCompleted.toJson();

        // assert
        expect(result, tJsonCompleted);
        expect(result['completed'], true);
        expect(result['submitted_at'], '2026-02-08T09:00:00.000');
      });

      test('should include all required fields', () {
        // act
        final result = tSafetyCheckTodayStatusModelCompleted.toJson();

        // assert
        expect(result.containsKey('completed'), true);
        expect(result.containsKey('submitted_at'), true);
      });
    });

    group('toEntity', () {
      test('should convert to SafetyCheckTodayStatus correctly (not completed)', () {
        // act
        final result = tSafetyCheckTodayStatusModelNotCompleted.toEntity();

        // assert
        expect(result, tSafetyCheckTodayStatusNotCompleted);
        expect(result.completed, false);
        expect(result.submittedAt, null);
      });

      test('should convert to SafetyCheckTodayStatus correctly (completed)', () {
        // act
        final result = tSafetyCheckTodayStatusModelCompleted.toEntity();

        // assert
        expect(result, tSafetyCheckTodayStatusCompleted);
        expect(result.completed, true);
        expect(result.submittedAt, tDateTime);
      });
    });

    group('fromEntity', () {
      test('should create model from entity correctly (not completed)', () {
        // act
        final result = SafetyCheckTodayStatusModel.fromEntity(
          tSafetyCheckTodayStatusNotCompleted,
        );

        // assert
        expect(result, tSafetyCheckTodayStatusModelNotCompleted);
        expect(result.completed, false);
        expect(result.submittedAt, null);
      });

      test('should create model from entity correctly (completed)', () {
        // act
        final result = SafetyCheckTodayStatusModel.fromEntity(
          tSafetyCheckTodayStatusCompleted,
        );

        // assert
        expect(result, tSafetyCheckTodayStatusModelCompleted);
        expect(result.completed, true);
        expect(result.submittedAt, tDateTime);
      });
    });

    group('round-trip conversion', () {
      test('fromJson -> toEntity -> fromEntity -> toJson should preserve data (not completed)', () {
        // arrange
        final originalJson = tJsonNotCompleted;

        // act
        final model = SafetyCheckTodayStatusModel.fromJson(originalJson);
        final entity = model.toEntity();
        final newModel = SafetyCheckTodayStatusModel.fromEntity(entity);
        final resultJson = newModel.toJson();

        // assert
        expect(resultJson, originalJson);
      });

      test('fromJson -> toEntity -> fromEntity -> toJson should preserve data (completed)', () {
        // arrange
        final originalJson = tJsonCompleted;

        // act
        final model = SafetyCheckTodayStatusModel.fromJson(originalJson);
        final entity = model.toEntity();
        final newModel = SafetyCheckTodayStatusModel.fromEntity(entity);
        final resultJson = newModel.toJson();

        // assert
        expect(resultJson, originalJson);
      });
    });

    group('equality', () {
      test('should be equal when all fields are the same (not completed)', () {
        // arrange
        const model1 = SafetyCheckTodayStatusModel(
          completed: false,
          submittedAt: null,
        );
        const model2 = SafetyCheckTodayStatusModel(
          completed: false,
          submittedAt: null,
        );

        // assert
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('should be equal when all fields are the same (completed)', () {
        // arrange
        final dateTime = DateTime.parse('2026-02-08T09:00:00.000');
        final model1 = SafetyCheckTodayStatusModel(
          completed: true,
          submittedAt: dateTime,
        );
        final model2 = SafetyCheckTodayStatusModel(
          completed: true,
          submittedAt: dateTime,
        );

        // assert
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('should not be equal when fields differ', () {
        // arrange
        const model1 = SafetyCheckTodayStatusModel(
          completed: false,
          submittedAt: null,
        );
        final model2 = SafetyCheckTodayStatusModel(
          completed: true,
          submittedAt: DateTime.parse('2026-02-08T09:00:00.000'),
        );

        // assert
        expect(model1, isNot(model2));
      });
    });

    group('toString', () {
      test('should return string representation (not completed)', () {
        // act
        final result = tSafetyCheckTodayStatusModelNotCompleted.toString();

        // assert
        expect(result, contains('SafetyCheckTodayStatusModel'));
        expect(result, contains('completed: false'));
        expect(result, contains('submittedAt: null'));
      });

      test('should return string representation (completed)', () {
        // act
        final result = tSafetyCheckTodayStatusModelCompleted.toString();

        // assert
        expect(result, contains('SafetyCheckTodayStatusModel'));
        expect(result, contains('completed: true'));
        expect(result, contains('submittedAt:'));
      });
    });
  });
}
