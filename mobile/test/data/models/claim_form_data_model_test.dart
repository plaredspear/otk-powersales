import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/claim_form_data_model.dart';
import 'package:mobile/domain/entities/claim_form_data.dart';

void main() {
  group('ClaimFormDataModel', () {
    test('fromJson이 올바르게 동작한다 (중첩 구조)', () {
      // Given
      final json = {
        'categories': [
          {
            'id': 1,
            'name': '이물',
            'subcategories': [
              {'id': 101, 'name': '벌레'},
              {'id': 102, 'name': '금속'},
            ],
          },
        ],
        'purchaseMethods': [
          {'code': 'PM01', 'name': '대형마트'},
        ],
        'requestTypes': [
          {'code': 'RT01', 'name': '교환'},
        ],
      };

      // When
      final model = ClaimFormDataModel.fromJson(json);

      // Then
      expect(model.categories.length, 1);
      expect(model.categories[0].name, '이물');
      expect(model.categories[0].subcategories.length, 2);
      expect(model.purchaseMethods.length, 1);
      expect(model.requestTypes.length, 1);
    });

    test('toEntity가 올바르게 동작한다', () {
      // Given
      final json = {
        'categories': [
          {
            'id': 1,
            'name': '이물',
            'subcategories': [
              {'id': 101, 'name': '벌레'},
            ],
          },
        ],
        'purchaseMethods': [
          {'code': 'PM01', 'name': '대형마트'},
        ],
        'requestTypes': [
          {'code': 'RT01', 'name': '교환'},
        ],
      };
      final model = ClaimFormDataModel.fromJson(json);

      // When
      final entity = model.toEntity();

      // Then
      expect(entity, isA<ClaimFormData>());
      expect(entity.categories.length, 1);
      expect(entity.purchaseMethods.length, 1);
      expect(entity.requestTypes.length, 1);
    });

    test('빈 목록을 처리할 수 있다', () {
      // Given
      final json = {
        'categories': [],
        'purchaseMethods': [],
        'requestTypes': [],
      };

      // When
      final model = ClaimFormDataModel.fromJson(json);
      final entity = model.toEntity();

      // Then
      expect(model.categories, isEmpty);
      expect(entity.categories, isEmpty);
    });
  });
}
