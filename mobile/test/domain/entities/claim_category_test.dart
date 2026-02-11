import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_category.dart';

void main() {
  group('ClaimSubcategory Entity', () {
    test('엔티티가 올바르게 생성된다', () {
      // Given & When
      const subcategory = ClaimSubcategory(
        id: 101,
        name: '벌레',
      );

      // Then
      expect(subcategory.id, 101);
      expect(subcategory.name, '벌레');
    });

    test('toJson이 올바르게 동작한다', () {
      // Given
      const subcategory = ClaimSubcategory(
        id: 102,
        name: '금속',
      );

      // When
      final json = subcategory.toJson();

      // Then
      expect(json, {
        'id': 102,
        'name': '금속',
      });
    });

    test('fromJson이 올바르게 동작한다', () {
      // Given
      final json = {
        'id': 103,
        'name': '비닐',
      };

      // When
      final subcategory = ClaimSubcategory.fromJson(json);

      // Then
      expect(subcategory.id, 103);
      expect(subcategory.name, '비닐');
    });

    test('toJson과 fromJson이 정확히 왕복 변환된다', () {
      // Given
      const original = ClaimSubcategory(
        id: 104,
        name: '기타 이물',
      );

      // When
      final json = original.toJson();
      final restored = ClaimSubcategory.fromJson(json);

      // Then
      expect(restored, original);
    });

    test('copyWith가 올바르게 동작한다', () {
      // Given
      const original = ClaimSubcategory(
        id: 101,
        name: '벌레',
      );

      // When
      final copied = original.copyWith(name: '곤충');

      // Then
      expect(copied.id, 101);
      expect(copied.name, '곤충');
      expect(original.name, '벌레'); // 원본 불변성 확인
    });

    test('같은 값을 가진 엔티티는 동일하게 비교된다', () {
      // Given
      const sub1 = ClaimSubcategory(id: 101, name: '벌레');
      const sub2 = ClaimSubcategory(id: 101, name: '벌레');

      // Then
      expect(sub1, sub2);
      expect(sub1.hashCode, sub2.hashCode);
    });

    test('다른 값을 가진 엔티티는 다르게 비교된다', () {
      // Given
      const sub1 = ClaimSubcategory(id: 101, name: '벌레');
      const sub2 = ClaimSubcategory(id: 102, name: '금속');

      // Then
      expect(sub1, isNot(sub2));
    });

    test('toString이 올바른 형식으로 출력된다', () {
      // Given
      const subcategory = ClaimSubcategory(id: 101, name: '벌레');

      // When
      final str = subcategory.toString();

      // Then
      expect(str, 'ClaimSubcategory(id: 101, name: 벌레)');
    });
  });

  group('ClaimCategory Entity', () {
    final testSubcategories = [
      const ClaimSubcategory(id: 101, name: '벌레'),
      const ClaimSubcategory(id: 102, name: '금속'),
      const ClaimSubcategory(id: 103, name: '비닐'),
    ];

    test('엔티티가 올바르게 생성된다', () {
      // Given & When
      final category = ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: testSubcategories,
      );

      // Then
      expect(category.id, 1);
      expect(category.name, '이물');
      expect(category.subcategories.length, 3);
      expect(category.subcategories[0].name, '벌레');
    });

    test('toJson이 올바르게 동작한다 (중첩 구조)', () {
      // Given
      final category = ClaimCategory(
        id: 2,
        name: '변질/변패',
        subcategories: const [
          ClaimSubcategory(id: 201, name: '맛 변질'),
          ClaimSubcategory(id: 202, name: '냄새 이상'),
        ],
      );

      // When
      final json = category.toJson();

      // Then
      expect(json, {
        'id': 2,
        'name': '변질/변패',
        'subcategories': [
          {'id': 201, 'name': '맛 변질'},
          {'id': 202, 'name': '냄새 이상'},
        ],
      });
    });

    test('fromJson이 올바르게 동작한다 (중첩 구조)', () {
      // Given
      final json = {
        'id': 1,
        'name': '이물',
        'subcategories': [
          {'id': 101, 'name': '벌레'},
          {'id': 102, 'name': '금속'},
        ],
      };

      // When
      final category = ClaimCategory.fromJson(json);

      // Then
      expect(category.id, 1);
      expect(category.name, '이물');
      expect(category.subcategories.length, 2);
      expect(category.subcategories[0].id, 101);
      expect(category.subcategories[0].name, '벌레');
      expect(category.subcategories[1].id, 102);
      expect(category.subcategories[1].name, '금속');
    });

    test('toJson과 fromJson이 정확히 왕복 변환된다', () {
      // Given
      final original = ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: testSubcategories,
      );

      // When
      final json = original.toJson();
      final restored = ClaimCategory.fromJson(json);

      // Then
      expect(restored, original);
    });

    test('copyWith가 올바르게 동작한다', () {
      // Given
      final original = ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: testSubcategories,
      );

      // When
      final copied = original.copyWith(name: '이물 발견');

      // Then
      expect(copied.id, 1);
      expect(copied.name, '이물 발견');
      expect(copied.subcategories, testSubcategories);
      expect(original.name, '이물'); // 원본 불변성 확인
    });

    test('copyWith로 subcategories를 변경할 수 있다', () {
      // Given
      final original = ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: testSubcategories,
      );

      final newSubcategories = [
        const ClaimSubcategory(id: 201, name: '맛 변질'),
      ];

      // When
      final copied = original.copyWith(subcategories: newSubcategories);

      // Then
      expect(copied.subcategories.length, 1);
      expect(copied.subcategories[0].name, '맛 변질');
      expect(original.subcategories.length, 3); // 원본 불변성 확인
    });

    test('같은 값을 가진 엔티티는 동일하게 비교된다', () {
      // Given
      final cat1 = ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: testSubcategories,
      );
      final cat2 = ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: testSubcategories,
      );

      // Then
      expect(cat1, cat2);
      expect(cat1.hashCode, cat2.hashCode);
    });

    test('다른 값을 가진 엔티티는 다르게 비교된다', () {
      // Given
      final cat1 = ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: testSubcategories,
      );
      final cat2 = ClaimCategory(
        id: 2,
        name: '변질/변패',
        subcategories: const [
          ClaimSubcategory(id: 201, name: '맛 변질'),
        ],
      );

      // Then
      expect(cat1, isNot(cat2));
    });

    test('subcategories가 다르면 다르게 비교된다', () {
      // Given
      final cat1 = ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: testSubcategories,
      );
      final cat2 = ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: const [
          ClaimSubcategory(id: 101, name: '벌레'),
          ClaimSubcategory(id: 102, name: '금속'),
        ],
      );

      // Then
      expect(cat1, isNot(cat2));
    });

    test('빈 subcategories로 엔티티를 생성할 수 있다', () {
      // Given & When
      const category = ClaimCategory(
        id: 3,
        name: '기타',
        subcategories: [],
      );

      // Then
      expect(category.subcategories, isEmpty);
    });

    test('toString이 올바른 형식으로 출력된다', () {
      // Given
      const category = ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: [
          ClaimSubcategory(id: 101, name: '벌레'),
        ],
      );

      // When
      final str = category.toString();

      // Then
      expect(str, contains('ClaimCategory'));
      expect(str, contains('id: 1'));
      expect(str, contains('name: 이물'));
      expect(str, contains('subcategories'));
    });
  });
}
