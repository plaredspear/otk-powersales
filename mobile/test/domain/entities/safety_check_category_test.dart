import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/safety_check_category.dart';
import 'package:mobile/domain/entities/safety_check_item.dart';

void main() {
  group('SafetyCheckCategory', () {
    // Test fixtures
    const item1 = SafetyCheckItem(
      id: 1,
      label: '차량 외관 점검',
      sortOrder: 1,
      required: true,
    );

    const item2 = SafetyCheckItem(
      id: 2,
      label: '타이어 공기압 확인',
      sortOrder: 2,
      required: true,
    );

    const item3 = SafetyCheckItem(
      id: 3,
      label: '브레이크 점검',
      sortOrder: 3,
      required: false,
    );

    group('Creation', () {
      test('creates correctly with all fields', () {
        // Arrange & Act
        const category = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '출발 전 차량 상태 점검',
          items: [item1, item2],
        );

        // Assert
        expect(category.id, 1);
        expect(category.name, '차량 점검');
        expect(category.description, '출발 전 차량 상태 점검');
        expect(category.items, [item1, item2]);
        expect(category.items.length, 2);
      });

      test('creates correctly with null description', () {
        // Arrange & Act
        const category = SafetyCheckCategory(
          id: 2,
          name: '기타 점검',
          description: null,
          items: [item3],
        );

        // Assert
        expect(category.id, 2);
        expect(category.name, '기타 점검');
        expect(category.description, isNull);
        expect(category.items, [item3]);
      });

      test('creates correctly with empty items list', () {
        // Arrange & Act
        const category = SafetyCheckCategory(
          id: 3,
          name: '빈 카테고리',
          description: '항목이 없는 카테고리',
          items: [],
        );

        // Assert
        expect(category.items, isEmpty);
      });
    });

    group('copyWith', () {
      test('returns new instance with updated id', () {
        // Arrange
        const original = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1],
        );

        // Act
        final updated = original.copyWith(id: 999);

        // Assert
        expect(updated.id, 999);
        expect(updated.name, original.name);
        expect(updated.description, original.description);
        expect(updated.items, original.items);
      });

      test('returns new instance with updated name', () {
        // Arrange
        const original = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1],
        );

        // Act
        final updated = original.copyWith(name: '새로운 카테고리');

        // Assert
        expect(updated.name, '새로운 카테고리');
        expect(updated.id, original.id);
      });

      test('returns new instance with updated description', () {
        // Arrange
        const original = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '원본 설명',
          items: [item1],
        );

        // Act
        final updated = original.copyWith(description: '새로운 설명');

        // Assert
        expect(updated.description, '새로운 설명');
        expect(updated.id, original.id);
        expect(updated.name, original.name);
      });

      test('returns new instance with updated items', () {
        // Arrange
        const original = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1],
        );

        // Act
        final updated = original.copyWith(items: [item2, item3]);

        // Assert
        expect(updated.items, [item2, item3]);
        expect(updated.items.length, 2);
        expect(updated.id, original.id);
      });

      test('returns new instance with all fields updated', () {
        // Arrange
        const original = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '원본 설명',
          items: [item1],
        );

        // Act
        final updated = original.copyWith(
          id: 2,
          name: '새 카테고리',
          description: '새 설명',
          items: [item2, item3],
        );

        // Assert
        expect(updated.id, 2);
        expect(updated.name, '새 카테고리');
        expect(updated.description, '새 설명');
        expect(updated.items, [item2, item3]);
      });

      test('returns identical instance when no fields provided', () {
        // Arrange
        const original = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1, item2],
        );

        // Act
        final copied = original.copyWith();

        // Assert
        expect(copied.id, original.id);
        expect(copied.name, original.name);
        expect(copied.description, original.description);
        expect(copied.items, original.items);
      });
    });

    group('Serialization', () {
      test('toJson returns correct map including items list', () {
        // Arrange
        const category = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '출발 전 차량 상태 점검',
          items: [item1, item2],
        );

        // Act
        final json = category.toJson();

        // Assert
        expect(json['id'], 1);
        expect(json['name'], '차량 점검');
        expect(json['description'], '출발 전 차량 상태 점검');
        expect(json['items'], isList);
        expect(json['items'], hasLength(2));
        expect(json['items'][0], isA<Map<String, dynamic>>());
        expect(json['items'][0]['id'], 1);
        expect(json['items'][1]['id'], 2);
      });

      test('toJson handles null description', () {
        // Arrange
        const category = SafetyCheckCategory(
          id: 2,
          name: '기타 점검',
          description: null,
          items: [],
        );

        // Act
        final json = category.toJson();

        // Assert
        expect(json['description'], isNull);
      });

      test('toJson handles empty items list', () {
        // Arrange
        const category = SafetyCheckCategory(
          id: 3,
          name: '빈 카테고리',
          description: '설명',
          items: [],
        );

        // Act
        final json = category.toJson();

        // Assert
        expect(json['items'], isEmpty);
      });

      test('fromJson creates entity from map correctly including nested items',
          () {
        // Arrange
        final json = {
          'id': 1,
          'name': '차량 점검',
          'description': '출발 전 차량 상태 점검',
          'items': [
            {
              'id': 1,
              'label': '차량 외관 점검',
              'sortOrder': 1,
              'required': true,
            },
            {
              'id': 2,
              'label': '타이어 공기압 확인',
              'sortOrder': 2,
              'required': true,
            },
          ],
        };

        // Act
        final category = SafetyCheckCategory.fromJson(json);

        // Assert
        expect(category.id, 1);
        expect(category.name, '차량 점검');
        expect(category.description, '출발 전 차량 상태 점검');
        expect(category.items, hasLength(2));
        expect(category.items[0].id, 1);
        expect(category.items[0].label, '차량 외관 점검');
        expect(category.items[1].id, 2);
        expect(category.items[1].label, '타이어 공기압 확인');
      });

      test('fromJson handles null description', () {
        // Arrange
        final json = {
          'id': 2,
          'name': '기타 점검',
          'description': null,
          'items': [],
        };

        // Act
        final category = SafetyCheckCategory.fromJson(json);

        // Assert
        expect(category.description, isNull);
      });

      test('toJson and fromJson round-trip preserves data', () {
        // Arrange
        const original = SafetyCheckCategory(
          id: 5,
          name: '안전 점검',
          description: '안전 관련 점검 항목',
          items: [item1, item2, item3],
        );

        // Act
        final json = original.toJson();
        final restored = SafetyCheckCategory.fromJson(json);

        // Assert
        expect(restored, original);
        expect(restored.items, original.items);
      });
    });

    group('Equality', () {
      test('two entities with same values including items are equal', () {
        // Arrange
        const category1 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1, item2],
        );
        const category2 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1, item2],
        );

        // Assert
        expect(category1, category2);
      });

      test('two entities with different ids are not equal', () {
        // Arrange
        const category1 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1],
        );
        const category2 = SafetyCheckCategory(
          id: 2,
          name: '차량 점검',
          description: '설명',
          items: [item1],
        );

        // Assert
        expect(category1, isNot(category2));
      });

      test('two entities with different names are not equal', () {
        // Arrange
        const category1 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1],
        );
        const category2 = SafetyCheckCategory(
          id: 1,
          name: '다른 카테고리',
          description: '설명',
          items: [item1],
        );

        // Assert
        expect(category1, isNot(category2));
      });

      test('two entities with different descriptions are not equal', () {
        // Arrange
        const category1 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명1',
          items: [item1],
        );
        const category2 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명2',
          items: [item1],
        );

        // Assert
        expect(category1, isNot(category2));
      });

      test('two entities with different items lists are not equal', () {
        // Arrange
        const category1 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1, item2],
        );
        const category2 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1, item3],
        );

        // Assert
        expect(category1, isNot(category2));
      });

      test('two entities with items in different order are not equal', () {
        // Arrange
        const category1 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1, item2],
        );
        const category2 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item2, item1],
        );

        // Assert
        expect(category1, isNot(category2));
      });

      test('hashCode is consistent for equal entities', () {
        // Arrange
        const category1 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1, item2],
        );
        const category2 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1, item2],
        );

        // Assert
        expect(category1.hashCode, category2.hashCode);
      });

      test('hashCode differs for different entities', () {
        // Arrange
        const category1 = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1],
        );
        const category2 = SafetyCheckCategory(
          id: 2,
          name: '차량 점검',
          description: '설명',
          items: [item1],
        );

        // Assert
        expect(category1.hashCode, isNot(category2.hashCode));
      });

      test('identical instances are equal', () {
        // Arrange
        const category = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '설명',
          items: [item1],
        );

        // Assert
        // ignore: unrelated_type_equality_checks
        expect(category == category, true);
      });
    });

    group('toString', () {
      test('toString contains useful info', () {
        // Arrange
        const category = SafetyCheckCategory(
          id: 1,
          name: '차량 점검',
          description: '출발 전 차량 상태 점검',
          items: [item1, item2],
        );

        // Act
        final string = category.toString();

        // Assert
        expect(string, contains('SafetyCheckCategory'));
        expect(string, contains('id: 1'));
        expect(string, contains('name: 차량 점검'));
        expect(string, contains('description: 출발 전 차량 상태 점검'));
        expect(string, contains('2 items'));
      });

      test('toString format matches expected pattern', () {
        // Arrange
        const category = SafetyCheckCategory(
          id: 42,
          name: '테스트 카테고리',
          description: '테스트 설명',
          items: [item1, item2, item3],
        );

        // Act
        final string = category.toString();

        // Assert
        expect(
          string,
          'SafetyCheckCategory(id: 42, name: 테스트 카테고리, description: 테스트 설명, items: 3 items)',
        );
      });

      test('toString shows items count correctly for empty list', () {
        // Arrange
        const category = SafetyCheckCategory(
          id: 1,
          name: '빈 카테고리',
          description: null,
          items: [],
        );

        // Act
        final string = category.toString();

        // Assert
        expect(string, contains('0 items'));
      });
    });
  });
}
