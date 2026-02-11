import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/claim_code_model.dart';
import 'package:mobile/domain/entities/claim_code.dart';

void main() {
  group('PurchaseMethodModel', () {
    test('fromJson이 올바르게 동작한다', () {
      // Given
      final json = {
        'code': 'PM01',
        'name': '대형마트',
      };

      // When
      final model = PurchaseMethodModel.fromJson(json);

      // Then
      expect(model.code, 'PM01');
      expect(model.name, '대형마트');
    });

    test('toJson이 올바르게 동작한다', () {
      // Given
      const model = PurchaseMethodModel(
        code: 'PM02',
        name: '편의점',
      );

      // When
      final json = model.toJson();

      // Then
      expect(json, {
        'code': 'PM02',
        'name': '편의점',
      });
    });

    test('toEntity가 올바르게 동작한다', () {
      // Given
      const model = PurchaseMethodModel(
        code: 'PM03',
        name: '온라인',
      );

      // When
      final entity = model.toEntity();

      // Then
      expect(entity, isA<PurchaseMethod>());
      expect(entity.code, 'PM03');
      expect(entity.name, '온라인');
    });

    test('fromEntity가 올바르게 동작한다', () {
      // Given
      const entity = PurchaseMethod(
        code: 'PM04',
        name: '슈퍼마켓',
      );

      // When
      final model = PurchaseMethodModel.fromEntity(entity);

      // Then
      expect(model.code, 'PM04');
      expect(model.name, '슈퍼마켓');
    });

    test('fromJson -> toEntity 변환이 올바르게 동작한다', () {
      // Given
      final json = {
        'code': 'PM01',
        'name': '대형마트',
      };

      // When
      final model = PurchaseMethodModel.fromJson(json);
      final entity = model.toEntity();

      // Then
      expect(entity.code, 'PM01');
      expect(entity.name, '대형마트');
    });

    test('fromEntity -> toJson 변환이 올바르게 동작한다', () {
      // Given
      const entity = PurchaseMethod(
        code: 'PM01',
        name: '대형마트',
      );

      // When
      final model = PurchaseMethodModel.fromEntity(entity);
      final json = model.toJson();

      // Then
      expect(json['code'], 'PM01');
      expect(json['name'], '대형마트');
    });

    test('Entity -> Model -> Entity 왕복 변환이 정확하다', () {
      // Given
      const original = PurchaseMethod(
        code: 'PM99',
        name: '기타',
      );

      // When
      final model = PurchaseMethodModel.fromEntity(original);
      final restored = model.toEntity();

      // Then
      expect(restored, original);
    });
  });

  group('ClaimRequestTypeModel', () {
    test('fromJson이 올바르게 동작한다', () {
      // Given
      final json = {
        'code': 'RT01',
        'name': '교환',
      };

      // When
      final model = ClaimRequestTypeModel.fromJson(json);

      // Then
      expect(model.code, 'RT01');
      expect(model.name, '교환');
    });

    test('toJson이 올바르게 동작한다', () {
      // Given
      const model = ClaimRequestTypeModel(
        code: 'RT02',
        name: '환불',
      );

      // When
      final json = model.toJson();

      // Then
      expect(json, {
        'code': 'RT02',
        'name': '환불',
      });
    });

    test('toEntity가 올바르게 동작한다', () {
      // Given
      const model = ClaimRequestTypeModel(
        code: 'RT03',
        name: '원인 규명',
      );

      // When
      final entity = model.toEntity();

      // Then
      expect(entity, isA<ClaimRequestType>());
      expect(entity.code, 'RT03');
      expect(entity.name, '원인 규명');
    });

    test('fromEntity가 올바르게 동작한다', () {
      // Given
      const entity = ClaimRequestType(
        code: 'RT99',
        name: '기타',
      );

      // When
      final model = ClaimRequestTypeModel.fromEntity(entity);

      // Then
      expect(model.code, 'RT99');
      expect(model.name, '기타');
    });

    test('fromJson -> toEntity 변환이 올바르게 동작한다', () {
      // Given
      final json = {
        'code': 'RT01',
        'name': '교환',
      };

      // When
      final model = ClaimRequestTypeModel.fromJson(json);
      final entity = model.toEntity();

      // Then
      expect(entity.code, 'RT01');
      expect(entity.name, '교환');
    });

    test('fromEntity -> toJson 변환이 올바르게 동작한다', () {
      // Given
      const entity = ClaimRequestType(
        code: 'RT01',
        name: '교환',
      );

      // When
      final model = ClaimRequestTypeModel.fromEntity(entity);
      final json = model.toJson();

      // Then
      expect(json['code'], 'RT01');
      expect(json['name'], '교환');
    });

    test('Entity -> Model -> Entity 왕복 변환이 정확하다', () {
      // Given
      const original = ClaimRequestType(
        code: 'RT99',
        name: '기타',
      );

      // When
      final model = ClaimRequestTypeModel.fromEntity(original);
      final restored = model.toEntity();

      // Then
      expect(restored, original);
    });
  });
}
