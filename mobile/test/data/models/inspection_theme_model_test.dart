import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/inspection_theme_model.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';

void main() {
  group('InspectionThemeModel', () {
    group('fromJson', () {
      test('JSON에서 모델을 생성한다', () {
        // Given
        final json = {
          'id': 10,
          'name': '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          'startDate': '2020-08-01',
          'endDate': '2020-08-31',
        };

        // When
        final model = InspectionThemeModel.fromJson(json);

        // Then
        expect(model.id, 10);
        expect(model.name, '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)');
        expect(model.startDate, '2020-08-01');
        expect(model.endDate, '2020-08-31');
      });
    });

    group('toJson', () {
      test('모델을 JSON으로 직렬화한다', () {
        // Given
        const model = InspectionThemeModel(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: '2020-08-01',
          endDate: '2020-08-31',
        );

        // When
        final json = model.toJson();

        // Then
        expect(json['id'], 10);
        expect(json['name'], '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)');
        expect(json['startDate'], '2020-08-01');
        expect(json['endDate'], '2020-08-31');
      });
    });

    group('toEntity', () {
      test('모델을 엔티티로 변환한다', () {
        // Given
        const model = InspectionThemeModel(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: '2020-08-01',
          endDate: '2020-08-31',
        );

        // When
        final entity = model.toEntity();

        // Then
        expect(entity.id, 10);
        expect(entity.name, '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)');
        expect(entity.startDate, DateTime(2020, 8, 1));
        expect(entity.endDate, DateTime(2020, 8, 31));
      });

      test('날짜 문자열을 DateTime으로 변환한다', () {
        // Given
        const model = InspectionThemeModel(
          id: 11,
          name: '8월 테마',
          startDate: '2020-12-01',
          endDate: '2020-12-31',
        );

        // When
        final entity = model.toEntity();

        // Then
        expect(entity.startDate, DateTime(2020, 12, 1));
        expect(entity.endDate, DateTime(2020, 12, 31));
      });
    });

    group('fromEntity', () {
      test('엔티티에서 모델을 생성한다', () {
        // Given
        final entity = InspectionTheme(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        // When
        final model = InspectionThemeModel.fromEntity(entity);

        // Then
        expect(model.id, 10);
        expect(model.name, '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)');
        expect(model.startDate, '2020-08-01');
        expect(model.endDate, '2020-08-31');
      });

      test('DateTime을 ISO 8601 날짜 문자열로 변환한다', () {
        // Given
        final entity = InspectionTheme(
          id: 11,
          name: '8월 테마',
          startDate: DateTime(2020, 12, 1, 10, 30, 45),
          endDate: DateTime(2020, 12, 31, 23, 59, 59),
        );

        // When
        final model = InspectionThemeModel.fromEntity(entity);

        // Then
        expect(model.startDate, '2020-12-01');
        expect(model.endDate, '2020-12-31');
      });
    });

    group('round-trip conversion', () {
      test('Entity → Model → Entity 변환이 정확하다', () {
        // Given
        final originalEntity = InspectionTheme(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        // When
        final model = InspectionThemeModel.fromEntity(originalEntity);
        final convertedEntity = model.toEntity();

        // Then
        expect(convertedEntity.id, originalEntity.id);
        expect(convertedEntity.name, originalEntity.name);
        expect(convertedEntity.startDate, originalEntity.startDate);
        expect(convertedEntity.endDate, originalEntity.endDate);
      });

      test('JSON → Model → JSON 변환이 정확하다', () {
        // Given
        final originalJson = {
          'id': 10,
          'name': '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          'startDate': '2020-08-01',
          'endDate': '2020-08-31',
        };

        // When
        final model = InspectionThemeModel.fromJson(originalJson);
        final convertedJson = model.toJson();

        // Then
        expect(convertedJson, originalJson);
      });
    });

    group('equality and hashCode', () {
      test('같은 값을 가진 인스턴스는 동일하다', () {
        // Given
        const model1 = InspectionThemeModel(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: '2020-08-01',
          endDate: '2020-08-31',
        );
        const model2 = InspectionThemeModel(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: '2020-08-01',
          endDate: '2020-08-31',
        );

        // Then
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('다른 값을 가진 인스턴스는 동일하지 않다', () {
        // Given
        const model1 = InspectionThemeModel(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: '2020-08-01',
          endDate: '2020-08-31',
        );
        const model2 = InspectionThemeModel(
          id: 11,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: '2020-08-01',
          endDate: '2020-08-31',
        );

        // Then
        expect(model1, isNot(model2));
      });

      test('자기 자신과 동일하다', () {
        // Given
        const model = InspectionThemeModel(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: '2020-08-01',
          endDate: '2020-08-31',
        );

        // Then
        expect(model, model);
      });
    });

    group('toString', () {
      test('문자열 표현을 반환한다', () {
        // Given
        const model = InspectionThemeModel(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: '2020-08-01',
          endDate: '2020-08-31',
        );

        // When
        final result = model.toString();

        // Then
        expect(result, contains('InspectionThemeModel'));
        expect(result, contains('id: 10'));
        expect(result, contains('name: 롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)'));
        expect(result, contains('startDate: 2020-08-01'));
        expect(result, contains('endDate: 2020-08-31'));
      });
    });
  });
}
