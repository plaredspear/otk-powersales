import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/claim_category_model.dart';
import 'package:mobile/domain/entities/claim_category.dart';

void main() {
  group('ClaimSubcategoryModel', () {
    test('fromJson이 올바르게 동작한다', () {
      // Given
      final json = {'id': 101, 'name': '벌레'};

      // When
      final model = ClaimSubcategoryModel.fromJson(json);

      // Then
      expect(model.id, 101);
      expect(model.name, '벌레');
    });

    test('toJson이 올바르게 동작한다', () {
      // Given
      const model = ClaimSubcategoryModel(id: 102, name: '금속');

      // When
      final json = model.toJson();

      // Then
      expect(json, {'id': 102, 'name': '금속'});
    });

    test('toEntity가 올바르게 동작한다', () {
      // Given
      const model = ClaimSubcategoryModel(id: 103, name: '비닐');

      // When
      final entity = model.toEntity();

      // Then
      expect(entity, isA<ClaimSubcategory>());
      expect(entity.id, 103);
      expect(entity.name, '비닐');
    });

    test('fromEntity가 올바르게 동작한다', () {
      // Given
      const entity = ClaimSubcategory(id: 104, name: '기타 이물');

      // When
      final model = ClaimSubcategoryModel.fromEntity(entity);

      // Then
      expect(model.id, 104);
      expect(model.name, '기타 이물');
    });

    test('Entity -> Model -> Entity 왕복 변환이 정확하다', () {
      // Given
      const original = ClaimSubcategory(id: 101, name: '벌레');

      // When
      final model = ClaimSubcategoryModel.fromEntity(original);
      final restored = model.toEntity();

      // Then
      expect(restored, original);
    });
  });

  group('ClaimCategoryModel', () {
    final testSubcategoriesJson = [
      {'id': 101, 'name': '벌레'},
      {'id': 102, 'name': '금속'},
    ];

    const testSubcategoriesModel = [
      ClaimSubcategoryModel(id: 101, name: '벌레'),
      ClaimSubcategoryModel(id: 102, name: '금속'),
    ];

    test('fromJson이 올바르게 동작한다 (중첩 구조)', () {
      // Given
      final json = {
        'id': 1,
        'name': '이물',
        'subcategories': testSubcategoriesJson,
      };

      // When
      final model = ClaimCategoryModel.fromJson(json);

      // Then
      expect(model.id, 1);
      expect(model.name, '이물');
      expect(model.subcategories.length, 2);
      expect(model.subcategories[0].id, 101);
      expect(model.subcategories[0].name, '벌레');
    });

    test('toJson이 올바르게 동작한다 (중첩 구조)', () {
      // Given
      const model = ClaimCategoryModel(
        id: 2,
        name: '변질/변패',
        subcategories: testSubcategoriesModel,
      );

      // When
      final json = model.toJson();

      // Then
      expect(json['id'], 2);
      expect(json['name'], '변질/변패');
      expect(json['subcategories'], isA<List>());
      expect(json['subcategories'].length, 2);
    });

    test('toEntity가 올바르게 동작한다 (중첩 변환)', () {
      // Given
      const model = ClaimCategoryModel(
        id: 1,
        name: '이물',
        subcategories: testSubcategoriesModel,
      );

      // When
      final entity = model.toEntity();

      // Then
      expect(entity, isA<ClaimCategory>());
      expect(entity.id, 1);
      expect(entity.name, '이물');
      expect(entity.subcategories.length, 2);
      expect(entity.subcategories[0], isA<ClaimSubcategory>());
      expect(entity.subcategories[0].name, '벌레');
    });

    test('fromEntity가 올바르게 동작한다 (중첩 변환)', () {
      // Given
      const entity = ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: [
          ClaimSubcategory(id: 101, name: '벌레'),
          ClaimSubcategory(id: 102, name: '금속'),
        ],
      );

      // When
      final model = ClaimCategoryModel.fromEntity(entity);

      // Then
      expect(model.id, 1);
      expect(model.name, '이물');
      expect(model.subcategories.length, 2);
      expect(model.subcategories[0].id, 101);
    });

    test('Entity -> Model -> Entity 왕복 변환이 정확하다', () {
      // Given
      const original = ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: [
          ClaimSubcategory(id: 101, name: '벌레'),
          ClaimSubcategory(id: 102, name: '금속'),
        ],
      );

      // When
      final model = ClaimCategoryModel.fromEntity(original);
      final restored = model.toEntity();

      // Then
      expect(restored, original);
    });

    test('빈 subcategories를 처리할 수 있다', () {
      // Given
      final json = {
        'id': 3,
        'name': '기타',
        'subcategories': [],
      };

      // When
      final model = ClaimCategoryModel.fromJson(json);
      final entity = model.toEntity();

      // Then
      expect(model.subcategories, isEmpty);
      expect(entity.subcategories, isEmpty);
    });
  });
}
