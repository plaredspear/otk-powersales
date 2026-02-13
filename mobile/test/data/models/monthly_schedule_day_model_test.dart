import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/monthly_schedule_day_model.dart';
import 'package:mobile/domain/entities/monthly_schedule_day.dart';

void main() {
  group('MonthlyScheduleDayModel', () {
    final testDate = DateTime(2026, 2, 4);
    final testModel = MonthlyScheduleDayModel(
      date: testDate,
      hasWork: true,
    );

    final testJson = {
      'date': '2026-02-04',
      'has_work': true,
    };

    final testEntity = MonthlyScheduleDay(
      date: testDate,
      hasWork: true,
    );

    group('fromJson', () {
      test('snake_case JSON 키를 올바르게 파싱해야 한다', () {
        // Act
        final result = MonthlyScheduleDayModel.fromJson(testJson);

        // Assert
        expect(result.date, testDate);
        expect(result.hasWork, true);
      });

      test('근무가 없는 날을 파싱할 수 있다', () {
        // Arrange
        final json = {
          'date': '2026-02-05',
          'has_work': false,
        };

        // Act
        final result = MonthlyScheduleDayModel.fromJson(json);

        // Assert
        expect(result.date, DateTime(2026, 2, 5));
        expect(result.hasWork, false);
      });
    });

    group('toJson', () {
      test('snake_case JSON 키로 올바르게 직렬화해야 한다', () {
        // Act
        final result = testModel.toJson();

        // Assert
        expect(result['date'], '2026-02-04');
        expect(result['has_work'], true);
      });

      test('날짜를 YYYY-MM-DD 형식으로 직렬화해야 한다', () {
        // Arrange
        final model = MonthlyScheduleDayModel(
          date: DateTime(2026, 12, 31),
          hasWork: true,
        );

        // Act
        final result = model.toJson();

        // Assert
        expect(result['date'], '2026-12-31');
      });
    });

    group('toEntity', () {
      test('Domain Entity로 올바르게 변환해야 한다', () {
        // Act
        final result = testModel.toEntity();

        // Assert
        expect(result, isA<MonthlyScheduleDay>());
        expect(result.date, testDate);
        expect(result.hasWork, true);
      });
    });

    group('fromEntity', () {
      test('Domain Entity로부터 Model을 생성할 수 있다', () {
        // Act
        final result = MonthlyScheduleDayModel.fromEntity(testEntity);

        // Assert
        expect(result.date, testDate);
        expect(result.hasWork, true);
      });
    });

    group('왕복 변환', () {
      test('JSON → Model → JSON 왕복 변환이 정확해야 한다', () {
        // Act
        final model = MonthlyScheduleDayModel.fromJson(testJson);
        final json = model.toJson();

        // Assert
        expect(json, testJson);
      });

      test('Entity → Model → Entity 왕복 변환이 정확해야 한다', () {
        // Act
        final model = MonthlyScheduleDayModel.fromEntity(testEntity);
        final entity = model.toEntity();

        // Assert
        expect(entity, testEntity);
      });
    });

    group('equality', () {
      test('같은 값을 가진 모델이 동일하게 비교되어야 한다', () {
        // Arrange
        final model1 = MonthlyScheduleDayModel(
          date: testDate,
          hasWork: true,
        );
        final model2 = MonthlyScheduleDayModel(
          date: testDate,
          hasWork: true,
        );

        // Assert
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('다른 값을 가진 모델은 다르게 비교되어야 한다', () {
        // Arrange
        final model1 = MonthlyScheduleDayModel(
          date: testDate,
          hasWork: true,
        );
        final model2 = MonthlyScheduleDayModel(
          date: testDate,
          hasWork: false,
        );

        // Assert
        expect(model1, isNot(model2));
      });
    });

    test('toString이 올바르게 동작해야 한다', () {
      // Act
      final result = testModel.toString();

      // Assert
      expect(result, contains('MonthlyScheduleDayModel'));
      expect(result, contains('hasWork: true'));
    });
  });
}
