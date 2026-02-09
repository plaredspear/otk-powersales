import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/safety_check_item_model.dart';
import 'package:mobile/domain/entities/safety_check_item.dart';

void main() {
  group('SafetyCheckItemModel', () {
    const tSafetyCheckItemModel = SafetyCheckItemModel(
      id: 1,
      label: '손목보호대',
      sortOrder: 1,
      required: true,
    );

    const tSafetyCheckItem = SafetyCheckItem(
      id: 1,
      label: '손목보호대',
      sortOrder: 1,
      required: true,
    );

    final tJson = {
      'id': 1,
      'label': '손목보호대',
      'sort_order': 1,
      'required': true,
    };

    group('fromJson', () {
      test('should parse snake_case JSON correctly', () {
        // act
        final result = SafetyCheckItemModel.fromJson(tJson);

        // assert
        expect(result, tSafetyCheckItemModel);
      });

      test('should parse required field as false when not provided', () {
        // arrange
        final json = {
          'id': 2,
          'label': '안전화',
          'sort_order': 2,
          'required': false,
        };

        // act
        final result = SafetyCheckItemModel.fromJson(json);

        // assert
        expect(result.required, false);
      });
    });

    group('toJson', () {
      test('should output snake_case JSON correctly', () {
        // act
        final result = tSafetyCheckItemModel.toJson();

        // assert
        expect(result, tJson);
      });

      test('should include all required fields', () {
        // act
        final result = tSafetyCheckItemModel.toJson();

        // assert
        expect(result.containsKey('id'), true);
        expect(result.containsKey('label'), true);
        expect(result.containsKey('sort_order'), true);
        expect(result.containsKey('required'), true);
      });
    });

    group('toEntity', () {
      test('should convert to SafetyCheckItem correctly', () {
        // act
        final result = tSafetyCheckItemModel.toEntity();

        // assert
        expect(result, tSafetyCheckItem);
        expect(result.id, tSafetyCheckItemModel.id);
        expect(result.label, tSafetyCheckItemModel.label);
        expect(result.sortOrder, tSafetyCheckItemModel.sortOrder);
        expect(result.required, tSafetyCheckItemModel.required);
      });
    });

    group('fromEntity', () {
      test('should create model from entity correctly', () {
        // act
        final result = SafetyCheckItemModel.fromEntity(tSafetyCheckItem);

        // assert
        expect(result, tSafetyCheckItemModel);
        expect(result.id, tSafetyCheckItem.id);
        expect(result.label, tSafetyCheckItem.label);
        expect(result.sortOrder, tSafetyCheckItem.sortOrder);
        expect(result.required, tSafetyCheckItem.required);
      });
    });

    group('round-trip conversion', () {
      test('fromJson -> toEntity -> fromEntity -> toJson should preserve data', () {
        // arrange
        final originalJson = tJson;

        // act
        final model = SafetyCheckItemModel.fromJson(originalJson);
        final entity = model.toEntity();
        final newModel = SafetyCheckItemModel.fromEntity(entity);
        final resultJson = newModel.toJson();

        // assert
        expect(resultJson, originalJson);
      });
    });

    group('equality', () {
      test('should be equal when all fields are the same', () {
        // arrange
        const model1 = SafetyCheckItemModel(
          id: 1,
          label: '손목보호대',
          sortOrder: 1,
          required: true,
        );
        const model2 = SafetyCheckItemModel(
          id: 1,
          label: '손목보호대',
          sortOrder: 1,
          required: true,
        );

        // assert
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('should not be equal when fields differ', () {
        // arrange
        const model1 = SafetyCheckItemModel(
          id: 1,
          label: '손목보호대',
          sortOrder: 1,
          required: true,
        );
        const model2 = SafetyCheckItemModel(
          id: 2,
          label: '안전화',
          sortOrder: 2,
          required: false,
        );

        // assert
        expect(model1, isNot(model2));
      });
    });

    group('toString', () {
      test('should return string representation', () {
        // act
        final result = tSafetyCheckItemModel.toString();

        // assert
        expect(result, contains('SafetyCheckItemModel'));
        expect(result, contains('id: 1'));
        expect(result, contains('label: 손목보호대'));
        expect(result, contains('sortOrder: 1'));
        expect(result, contains('required: true'));
      });
    });
  });
}
