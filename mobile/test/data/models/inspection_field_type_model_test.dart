import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/inspection_field_type_model.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';

void main() {
  group('InspectionFieldTypeModel', () {
    group('fromJson', () {
      test('JSON에서 모델을 생성한다', () {
        // Given
        final json = {
          'code': 'FT01',
          'name': '본매대',
        };

        // When
        final model = InspectionFieldTypeModel.fromJson(json);

        // Then
        expect(model.code, 'FT01');
        expect(model.name, '본매대');
      });

      test('다른 현장 유형 JSON을 파싱한다', () {
        // Given
        final json = {
          'code': 'FT02',
          'name': '시식',
        };

        // When
        final model = InspectionFieldTypeModel.fromJson(json);

        // Then
        expect(model.code, 'FT02');
        expect(model.name, '시식');
      });
    });

    group('toJson', () {
      test('모델을 JSON으로 직렬화한다', () {
        // Given
        const model = InspectionFieldTypeModel(
          code: 'FT01',
          name: '본매대',
        );

        // When
        final json = model.toJson();

        // Then
        expect(json['code'], 'FT01');
        expect(json['name'], '본매대');
      });
    });

    group('toEntity', () {
      test('모델을 엔티티로 변환한다', () {
        // Given
        const model = InspectionFieldTypeModel(
          code: 'FT01',
          name: '본매대',
        );

        // When
        final entity = model.toEntity();

        // Then
        expect(entity.code, 'FT01');
        expect(entity.name, '본매대');
      });
    });

    group('fromEntity', () {
      test('엔티티에서 모델을 생성한다', () {
        // Given
        const entity = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        // When
        final model = InspectionFieldTypeModel.fromEntity(entity);

        // Then
        expect(model.code, 'FT01');
        expect(model.name, '본매대');
      });
    });

    group('round-trip conversion', () {
      test('Entity → Model → Entity 변환이 정확하다', () {
        // Given
        const originalEntity = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        // When
        final model = InspectionFieldTypeModel.fromEntity(originalEntity);
        final convertedEntity = model.toEntity();

        // Then
        expect(convertedEntity.code, originalEntity.code);
        expect(convertedEntity.name, originalEntity.name);
      });

      test('JSON → Model → JSON 변환이 정확하다', () {
        // Given
        final originalJson = {
          'code': 'FT01',
          'name': '본매대',
        };

        // When
        final model = InspectionFieldTypeModel.fromJson(originalJson);
        final convertedJson = model.toJson();

        // Then
        expect(convertedJson, originalJson);
      });
    });

    group('equality and hashCode', () {
      test('같은 값을 가진 인스턴스는 동일하다', () {
        // Given
        const model1 = InspectionFieldTypeModel(
          code: 'FT01',
          name: '본매대',
        );
        const model2 = InspectionFieldTypeModel(
          code: 'FT01',
          name: '본매대',
        );

        // Then
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('다른 값을 가진 인스턴스는 동일하지 않다', () {
        // Given
        const model1 = InspectionFieldTypeModel(
          code: 'FT01',
          name: '본매대',
        );
        const model2 = InspectionFieldTypeModel(
          code: 'FT02',
          name: '시식',
        );

        // Then
        expect(model1, isNot(model2));
      });

      test('자기 자신과 동일하다', () {
        // Given
        const model = InspectionFieldTypeModel(
          code: 'FT01',
          name: '본매대',
        );

        // Then
        expect(model, model);
      });
    });

    group('toString', () {
      test('문자열 표현을 반환한다', () {
        // Given
        const model = InspectionFieldTypeModel(
          code: 'FT01',
          name: '본매대',
        );

        // When
        final result = model.toString();

        // Then
        expect(result, contains('InspectionFieldTypeModel'));
        expect(result, contains('code: FT01'));
        expect(result, contains('name: 본매대'));
      });
    });
  });
}
