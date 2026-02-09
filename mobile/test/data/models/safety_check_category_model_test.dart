import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/safety_check_category_model.dart';
import 'package:mobile/data/models/safety_check_item_model.dart';
import 'package:mobile/domain/entities/safety_check_category.dart';
import 'package:mobile/domain/entities/safety_check_item.dart';

void main() {
  group('SafetyCheckCategoryModel', () {
    final tSafetyCheckCategoryModel = SafetyCheckCategoryModel(
      id: 1,
      name: '안전예방 장비 착용',
      description: '아래 항목을 모두 체크하세요',
      items: const [
        SafetyCheckItemModel(
          id: 1,
          label: '손목보호대',
          sortOrder: 1,
          required: true,
        ),
        SafetyCheckItemModel(
          id: 2,
          label: '안전화',
          sortOrder: 2,
          required: true,
        ),
      ],
    );

    final tSafetyCheckCategory = SafetyCheckCategory(
      id: 1,
      name: '안전예방 장비 착용',
      description: '아래 항목을 모두 체크하세요',
      items: const [
        SafetyCheckItem(
          id: 1,
          label: '손목보호대',
          sortOrder: 1,
          required: true,
        ),
        SafetyCheckItem(
          id: 2,
          label: '안전화',
          sortOrder: 2,
          required: true,
        ),
      ],
    );

    final tJson = {
      'id': 1,
      'name': '안전예방 장비 착용',
      'description': '아래 항목을 모두 체크하세요',
      'items': [
        {
          'id': 1,
          'label': '손목보호대',
          'sort_order': 1,
          'required': true,
        },
        {
          'id': 2,
          'label': '안전화',
          'sort_order': 2,
          'required': true,
        },
      ],
    };

    group('fromJson', () {
      test('should parse correctly including nested items', () {
        // act
        final result = SafetyCheckCategoryModel.fromJson(tJson);

        // assert
        expect(result, tSafetyCheckCategoryModel);
        expect(result.id, 1);
        expect(result.name, '안전예방 장비 착용');
        expect(result.description, '아래 항목을 모두 체크하세요');
        expect(result.items.length, 2);
        expect(result.items[0].id, 1);
        expect(result.items[0].label, '손목보호대');
        expect(result.items[1].id, 2);
        expect(result.items[1].label, '안전화');
      });

      test('should parse with null description', () {
        // arrange
        final json = {
          'id': 2,
          'name': '기타 확인사항',
          'description': null,
          'items': [],
        };

        // act
        final result = SafetyCheckCategoryModel.fromJson(json);

        // assert
        expect(result.id, 2);
        expect(result.name, '기타 확인사항');
        expect(result.description, null);
        expect(result.items, isEmpty);
      });

      test('should parse with empty items list', () {
        // arrange
        final json = {
          'id': 3,
          'name': '빈 카테고리',
          'description': '설명',
          'items': [],
        };

        // act
        final result = SafetyCheckCategoryModel.fromJson(json);

        // assert
        expect(result.items, isEmpty);
      });
    });

    group('toJson', () {
      test('should output correctly with nested items', () {
        // act
        final result = tSafetyCheckCategoryModel.toJson();

        // assert
        expect(result, tJson);
      });

      test('should include all required fields', () {
        // act
        final result = tSafetyCheckCategoryModel.toJson();

        // assert
        expect(result.containsKey('id'), true);
        expect(result.containsKey('name'), true);
        expect(result.containsKey('description'), true);
        expect(result.containsKey('items'), true);
        expect(result['items'] is List, true);
      });

      test('should handle null description', () {
        // arrange
        final model = SafetyCheckCategoryModel(
          id: 2,
          name: '기타 확인사항',
          description: null,
          items: const [],
        );

        // act
        final result = model.toJson();

        // assert
        expect(result['description'], null);
      });
    });

    group('toEntity', () {
      test('should convert to SafetyCheckCategory including nested items', () {
        // act
        final result = tSafetyCheckCategoryModel.toEntity();

        // assert
        expect(result, tSafetyCheckCategory);
        expect(result.id, tSafetyCheckCategoryModel.id);
        expect(result.name, tSafetyCheckCategoryModel.name);
        expect(result.description, tSafetyCheckCategoryModel.description);
        expect(result.items.length, tSafetyCheckCategoryModel.items.length);

        for (var i = 0; i < result.items.length; i++) {
          expect(result.items[i].id, tSafetyCheckCategoryModel.items[i].id);
          expect(result.items[i].label, tSafetyCheckCategoryModel.items[i].label);
          expect(result.items[i].sortOrder, tSafetyCheckCategoryModel.items[i].sortOrder);
          expect(result.items[i].required, tSafetyCheckCategoryModel.items[i].required);
        }
      });
    });

    group('fromEntity', () {
      test('should create model from entity including nested items', () {
        // act
        final result = SafetyCheckCategoryModel.fromEntity(tSafetyCheckCategory);

        // assert
        expect(result, tSafetyCheckCategoryModel);
        expect(result.id, tSafetyCheckCategory.id);
        expect(result.name, tSafetyCheckCategory.name);
        expect(result.description, tSafetyCheckCategory.description);
        expect(result.items.length, tSafetyCheckCategory.items.length);

        for (var i = 0; i < result.items.length; i++) {
          expect(result.items[i].id, tSafetyCheckCategory.items[i].id);
          expect(result.items[i].label, tSafetyCheckCategory.items[i].label);
          expect(result.items[i].sortOrder, tSafetyCheckCategory.items[i].sortOrder);
          expect(result.items[i].required, tSafetyCheckCategory.items[i].required);
        }
      });
    });

    group('round-trip conversion', () {
      test('fromJson -> toEntity -> fromEntity -> toJson should preserve data', () {
        // arrange
        final originalJson = tJson;

        // act
        final model = SafetyCheckCategoryModel.fromJson(originalJson);
        final entity = model.toEntity();
        final newModel = SafetyCheckCategoryModel.fromEntity(entity);
        final resultJson = newModel.toJson();

        // assert
        expect(resultJson, originalJson);
      });
    });

    group('equality', () {
      test('should be equal when all fields including nested items are the same', () {
        // arrange
        final model1 = SafetyCheckCategoryModel(
          id: 1,
          name: '안전예방 장비 착용',
          description: '아래 항목을 모두 체크하세요',
          items: const [
            SafetyCheckItemModel(
              id: 1,
              label: '손목보호대',
              sortOrder: 1,
              required: true,
            ),
          ],
        );
        final model2 = SafetyCheckCategoryModel(
          id: 1,
          name: '안전예방 장비 착용',
          description: '아래 항목을 모두 체크하세요',
          items: const [
            SafetyCheckItemModel(
              id: 1,
              label: '손목보호대',
              sortOrder: 1,
              required: true,
            ),
          ],
        );

        // assert
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('should not be equal when fields differ', () {
        // arrange
        final model1 = SafetyCheckCategoryModel(
          id: 1,
          name: '안전예방 장비 착용',
          description: '설명1',
          items: const [],
        );
        final model2 = SafetyCheckCategoryModel(
          id: 2,
          name: '기타 확인사항',
          description: '설명2',
          items: const [],
        );

        // assert
        expect(model1, isNot(model2));
      });

      test('should not be equal when nested items differ', () {
        // arrange
        final model1 = SafetyCheckCategoryModel(
          id: 1,
          name: '안전예방 장비 착용',
          description: '설명',
          items: const [
            SafetyCheckItemModel(
              id: 1,
              label: '손목보호대',
              sortOrder: 1,
              required: true,
            ),
          ],
        );
        final model2 = SafetyCheckCategoryModel(
          id: 1,
          name: '안전예방 장비 착용',
          description: '설명',
          items: const [
            SafetyCheckItemModel(
              id: 2,
              label: '안전화',
              sortOrder: 2,
              required: true,
            ),
          ],
        );

        // assert
        expect(model1, isNot(model2));
      });
    });

    group('toString', () {
      test('should return string representation', () {
        // act
        final result = tSafetyCheckCategoryModel.toString();

        // assert
        expect(result, contains('SafetyCheckCategoryModel'));
        expect(result, contains('id: 1'));
        expect(result, contains('name: 안전예방 장비 착용'));
        expect(result, contains('description: 아래 항목을 모두 체크하세요'));
      });
    });
  });
}
