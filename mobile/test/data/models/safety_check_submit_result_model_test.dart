import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/safety_check_submit_result_model.dart';
import 'package:mobile/domain/entities/safety_check_submit_result.dart';

void main() {
  group('SafetyCheckSubmitResultModel', () {
    final tDateTime = DateTime.parse('2026-02-08T09:00:00.000');

    final tSafetyCheckSubmitResultModel = SafetyCheckSubmitResultModel(
      submissionId: 1,
      submittedAt: tDateTime,
      safetyCheckCompleted: true,
    );

    final tSafetyCheckSubmitResult = SafetyCheckSubmitResult(
      submissionId: 1,
      submittedAt: tDateTime,
      safetyCheckCompleted: true,
    );

    final tJson = {
      'submission_id': 1,
      'submitted_at': '2026-02-08T09:00:00.000',
      'safety_check_completed': true,
    };

    group('fromJson', () {
      test('should parse snake_case correctly', () {
        // act
        final result = SafetyCheckSubmitResultModel.fromJson(tJson);

        // assert
        expect(result, tSafetyCheckSubmitResultModel);
        expect(result.submissionId, 1);
        expect(result.submittedAt, tDateTime);
        expect(result.safetyCheckCompleted, true);
      });

      test('should parse with different values', () {
        // arrange
        final json = {
          'submission_id': 123,
          'submitted_at': '2026-02-10T15:30:45.123',
          'safety_check_completed': false,
        };

        // act
        final result = SafetyCheckSubmitResultModel.fromJson(json);

        // assert
        expect(result.submissionId, 123);
        expect(result.submittedAt, DateTime.parse('2026-02-10T15:30:45.123'));
        expect(result.safetyCheckCompleted, false);
      });

      test('should handle ISO 8601 date formats with timezone', () {
        // arrange
        final json = {
          'submission_id': 1,
          'submitted_at': '2026-02-08T09:00:00.000Z',
          'safety_check_completed': true,
        };

        // act
        final result = SafetyCheckSubmitResultModel.fromJson(json);

        // assert
        expect(result.submissionId, 1);
        expect(result.submittedAt, isNotNull);
        expect(result.safetyCheckCompleted, true);
      });
    });

    group('toJson', () {
      test('should output snake_case correctly', () {
        // act
        final result = tSafetyCheckSubmitResultModel.toJson();

        // assert
        expect(result, tJson);
      });

      test('should include all required fields', () {
        // act
        final result = tSafetyCheckSubmitResultModel.toJson();

        // assert
        expect(result.containsKey('submission_id'), true);
        expect(result.containsKey('submitted_at'), true);
        expect(result.containsKey('safety_check_completed'), true);
      });

      test('should output correct data types', () {
        // act
        final result = tSafetyCheckSubmitResultModel.toJson();

        // assert
        expect(result['submission_id'] is int, true);
        expect(result['submitted_at'] is String, true);
        expect(result['safety_check_completed'] is bool, true);
      });
    });

    group('toEntity', () {
      test('should convert to SafetyCheckSubmitResult correctly', () {
        // act
        final result = tSafetyCheckSubmitResultModel.toEntity();

        // assert
        expect(result, tSafetyCheckSubmitResult);
        expect(result.submissionId, tSafetyCheckSubmitResultModel.submissionId);
        expect(result.submittedAt, tSafetyCheckSubmitResultModel.submittedAt);
        expect(result.safetyCheckCompleted, tSafetyCheckSubmitResultModel.safetyCheckCompleted);
      });
    });

    group('fromEntity', () {
      test('should create model from entity correctly', () {
        // act
        final result = SafetyCheckSubmitResultModel.fromEntity(tSafetyCheckSubmitResult);

        // assert
        expect(result, tSafetyCheckSubmitResultModel);
        expect(result.submissionId, tSafetyCheckSubmitResult.submissionId);
        expect(result.submittedAt, tSafetyCheckSubmitResult.submittedAt);
        expect(result.safetyCheckCompleted, tSafetyCheckSubmitResult.safetyCheckCompleted);
      });

      test('should create model from entity with different values', () {
        // arrange
        final entity = SafetyCheckSubmitResult(
          submissionId: 999,
          submittedAt: DateTime.parse('2026-12-25T18:00:00.000'),
          safetyCheckCompleted: false,
        );

        // act
        final result = SafetyCheckSubmitResultModel.fromEntity(entity);

        // assert
        expect(result.submissionId, 999);
        expect(result.submittedAt, DateTime.parse('2026-12-25T18:00:00.000'));
        expect(result.safetyCheckCompleted, false);
      });
    });

    group('round-trip conversion', () {
      test('fromJson -> toEntity -> fromEntity -> toJson should preserve data', () {
        // arrange
        final originalJson = tJson;

        // act
        final model = SafetyCheckSubmitResultModel.fromJson(originalJson);
        final entity = model.toEntity();
        final newModel = SafetyCheckSubmitResultModel.fromEntity(entity);
        final resultJson = newModel.toJson();

        // assert
        expect(resultJson, originalJson);
      });

      test('should preserve data through multiple conversions', () {
        // arrange
        final originalModel = tSafetyCheckSubmitResultModel;

        // act
        final json = originalModel.toJson();
        final entity = SafetyCheckSubmitResultModel.fromJson(json).toEntity();
        final finalModel = SafetyCheckSubmitResultModel.fromEntity(entity);

        // assert
        expect(finalModel.submissionId, originalModel.submissionId);
        expect(finalModel.submittedAt, originalModel.submittedAt);
        expect(finalModel.safetyCheckCompleted, originalModel.safetyCheckCompleted);
      });
    });

    group('equality', () {
      test('should be equal when all fields are the same', () {
        // arrange
        final dateTime = DateTime.parse('2026-02-08T09:00:00.000');
        final model1 = SafetyCheckSubmitResultModel(
          submissionId: 1,
          submittedAt: dateTime,
          safetyCheckCompleted: true,
        );
        final model2 = SafetyCheckSubmitResultModel(
          submissionId: 1,
          submittedAt: dateTime,
          safetyCheckCompleted: true,
        );

        // assert
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('should not be equal when submission_id differs', () {
        // arrange
        final dateTime = DateTime.parse('2026-02-08T09:00:00.000');
        final model1 = SafetyCheckSubmitResultModel(
          submissionId: 1,
          submittedAt: dateTime,
          safetyCheckCompleted: true,
        );
        final model2 = SafetyCheckSubmitResultModel(
          submissionId: 2,
          submittedAt: dateTime,
          safetyCheckCompleted: true,
        );

        // assert
        expect(model1, isNot(model2));
      });

      test('should not be equal when submitted_at differs', () {
        // arrange
        final model1 = SafetyCheckSubmitResultModel(
          submissionId: 1,
          submittedAt: DateTime.parse('2026-02-08T09:00:00.000'),
          safetyCheckCompleted: true,
        );
        final model2 = SafetyCheckSubmitResultModel(
          submissionId: 1,
          submittedAt: DateTime.parse('2026-02-09T09:00:00.000'),
          safetyCheckCompleted: true,
        );

        // assert
        expect(model1, isNot(model2));
      });

      test('should not be equal when safety_check_completed differs', () {
        // arrange
        final dateTime = DateTime.parse('2026-02-08T09:00:00.000');
        final model1 = SafetyCheckSubmitResultModel(
          submissionId: 1,
          submittedAt: dateTime,
          safetyCheckCompleted: true,
        );
        final model2 = SafetyCheckSubmitResultModel(
          submissionId: 1,
          submittedAt: dateTime,
          safetyCheckCompleted: false,
        );

        // assert
        expect(model1, isNot(model2));
      });
    });

    group('toString', () {
      test('should return string representation', () {
        // act
        final result = tSafetyCheckSubmitResultModel.toString();

        // assert
        expect(result, contains('SafetyCheckSubmitResultModel'));
        expect(result, contains('submissionId: 1'));
        expect(result, contains('submittedAt:'));
        expect(result, contains('safetyCheckCompleted: true'));
      });

      test('should include all field values', () {
        // arrange
        final model = SafetyCheckSubmitResultModel(
          submissionId: 456,
          submittedAt: DateTime.parse('2026-03-15T12:30:00.000'),
          safetyCheckCompleted: false,
        );

        // act
        final result = model.toString();

        // assert
        expect(result, contains('456'));
        expect(result, contains('false'));
      });
    });
  });
}
