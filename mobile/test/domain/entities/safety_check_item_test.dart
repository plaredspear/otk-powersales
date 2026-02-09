import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/safety_check_item.dart';

void main() {
  group('SafetyCheckItem', () {
    group('Creation', () {
      test('creates correctly with all fields', () {
        // Arrange & Act
        const item = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );

        // Assert
        expect(item.id, 1);
        expect(item.label, '차량 외관 점검');
        expect(item.sortOrder, 1);
        expect(item.required, true);
      });

      test('creates correctly with required=false', () {
        // Arrange & Act
        const item = SafetyCheckItem(
          id: 2,
          label: '선택 항목',
          sortOrder: 5,
          required: false,
        );

        // Assert
        expect(item.required, false);
      });
    });

    group('copyWith', () {
      test('returns new instance with updated id', () {
        // Arrange
        const original = SafetyCheckItem(
          id: 1,
          label: '원본 항목',
          sortOrder: 1,
          required: true,
        );

        // Act
        final updated = original.copyWith(id: 999);

        // Assert
        expect(updated.id, 999);
        expect(updated.label, original.label);
        expect(updated.sortOrder, original.sortOrder);
        expect(updated.required, original.required);
      });

      test('returns new instance with updated label', () {
        // Arrange
        const original = SafetyCheckItem(
          id: 1,
          label: '원본 항목',
          sortOrder: 1,
          required: true,
        );

        // Act
        final updated = original.copyWith(label: '새로운 항목');

        // Assert
        expect(updated.label, '새로운 항목');
        expect(updated.id, original.id);
      });

      test('returns new instance with updated sortOrder', () {
        // Arrange
        const original = SafetyCheckItem(
          id: 1,
          label: '원본 항목',
          sortOrder: 1,
          required: true,
        );

        // Act
        final updated = original.copyWith(sortOrder: 10);

        // Assert
        expect(updated.sortOrder, 10);
        expect(updated.id, original.id);
        expect(updated.label, original.label);
      });

      test('returns new instance with updated required', () {
        // Arrange
        const original = SafetyCheckItem(
          id: 1,
          label: '원본 항목',
          sortOrder: 1,
          required: true,
        );

        // Act
        final updated = original.copyWith(required: false);

        // Assert
        expect(updated.required, false);
        expect(updated.id, original.id);
      });

      test('returns new instance with all fields updated', () {
        // Arrange
        const original = SafetyCheckItem(
          id: 1,
          label: '원본 항목',
          sortOrder: 1,
          required: true,
        );

        // Act
        final updated = original.copyWith(
          id: 2,
          label: '새 항목',
          sortOrder: 5,
          required: false,
        );

        // Assert
        expect(updated.id, 2);
        expect(updated.label, '새 항목');
        expect(updated.sortOrder, 5);
        expect(updated.required, false);
      });

      test('returns identical instance when no fields provided', () {
        // Arrange
        const original = SafetyCheckItem(
          id: 1,
          label: '원본 항목',
          sortOrder: 1,
          required: true,
        );

        // Act
        final copied = original.copyWith();

        // Assert
        expect(copied.id, original.id);
        expect(copied.label, original.label);
        expect(copied.sortOrder, original.sortOrder);
        expect(copied.required, original.required);
      });
    });

    group('Serialization', () {
      test('toJson returns correct map', () {
        // Arrange
        const item = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );

        // Act
        final json = item.toJson();

        // Assert
        expect(json, {
          'id': 1,
          'label': '차량 외관 점검',
          'sortOrder': 1,
          'required': true,
        });
      });

      test('fromJson creates entity from map correctly', () {
        // Arrange
        final json = {
          'id': 2,
          'label': '타이어 공기압 확인',
          'sortOrder': 3,
          'required': false,
        };

        // Act
        final item = SafetyCheckItem.fromJson(json);

        // Assert
        expect(item.id, 2);
        expect(item.label, '타이어 공기압 확인');
        expect(item.sortOrder, 3);
        expect(item.required, false);
      });

      test('toJson and fromJson round-trip preserves data', () {
        // Arrange
        const original = SafetyCheckItem(
          id: 5,
          label: '브레이크 점검',
          sortOrder: 7,
          required: true,
        );

        // Act
        final json = original.toJson();
        final restored = SafetyCheckItem.fromJson(json);

        // Assert
        expect(restored, original);
      });
    });

    group('Equality', () {
      test('two entities with same values are equal', () {
        // Arrange
        const item1 = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );
        const item2 = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );

        // Assert
        expect(item1, item2);
      });

      test('two entities with different ids are not equal', () {
        // Arrange
        const item1 = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );
        const item2 = SafetyCheckItem(
          id: 2,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );

        // Assert
        expect(item1, isNot(item2));
      });

      test('two entities with different labels are not equal', () {
        // Arrange
        const item1 = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );
        const item2 = SafetyCheckItem(
          id: 1,
          label: '다른 항목',
          sortOrder: 1,
          required: true,
        );

        // Assert
        expect(item1, isNot(item2));
      });

      test('two entities with different sortOrders are not equal', () {
        // Arrange
        const item1 = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );
        const item2 = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 2,
          required: true,
        );

        // Assert
        expect(item1, isNot(item2));
      });

      test('two entities with different required values are not equal', () {
        // Arrange
        const item1 = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );
        const item2 = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: false,
        );

        // Assert
        expect(item1, isNot(item2));
      });

      test('hashCode is consistent for equal entities', () {
        // Arrange
        const item1 = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );
        const item2 = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );

        // Assert
        expect(item1.hashCode, item2.hashCode);
      });

      test('hashCode differs for different entities', () {
        // Arrange
        const item1 = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );
        const item2 = SafetyCheckItem(
          id: 2,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );

        // Assert
        // Note: Hash codes can collide, but for different values they should typically differ
        expect(item1.hashCode, isNot(item2.hashCode));
      });

      test('identical instances are equal', () {
        // Arrange
        const item = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );

        // Assert
        // ignore: unrelated_type_equality_checks
        expect(item == item, true);
      });
    });

    group('toString', () {
      test('toString contains all field values', () {
        // Arrange
        const item = SafetyCheckItem(
          id: 1,
          label: '차량 외관 점검',
          sortOrder: 1,
          required: true,
        );

        // Act
        final string = item.toString();

        // Assert
        expect(string, contains('SafetyCheckItem'));
        expect(string, contains('id: 1'));
        expect(string, contains('label: 차량 외관 점검'));
        expect(string, contains('sortOrder: 1'));
        expect(string, contains('required: true'));
      });

      test('toString format matches expected pattern', () {
        // Arrange
        const item = SafetyCheckItem(
          id: 42,
          label: '테스트 항목',
          sortOrder: 10,
          required: false,
        );

        // Act
        final string = item.toString();

        // Assert
        expect(
          string,
          'SafetyCheckItem(id: 42, label: 테스트 항목, sortOrder: 10, required: false)',
        );
      });
    });
  });
}
