import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';

void main() {
  group('InspectionFieldType Entity', () {
    group('생성 테스트', () {
      test('InspectionFieldType이 올바르게 생성된다', () {
        final fieldType = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        expect(fieldType.code, 'FT01');
        expect(fieldType.name, '본매대');
      });

      test('다양한 현장 유형이 올바르게 생성된다', () {
        final types = [
          InspectionFieldType(code: 'FT01', name: '본매대'),
          InspectionFieldType(code: 'FT02', name: '시식'),
          InspectionFieldType(code: 'FT03', name: '행사매대'),
          InspectionFieldType(code: 'FT99', name: '기타'),
        ];

        expect(types.length, 4);
        expect(types[0].code, 'FT01');
        expect(types[1].name, '시식');
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다 - 모든 필드 변경', () {
        final original = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        final copied = original.copyWith(
          code: 'FT02',
          name: '시식',
        );

        expect(copied.code, 'FT02');
        expect(copied.name, '시식');
      });

      test('copyWith가 일부 필드만 변경한다', () {
        final original = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        final copied = original.copyWith(name: '시식');

        expect(copied.code, original.code);
        expect(copied.name, '시식');
      });

      test('copyWith가 원본을 변경하지 않는다 (불변성)', () {
        final original = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        final copied = original.copyWith(name: '시식');

        expect(original.name, '본매대');
        expect(copied.name, '시식');
      });
    });

    group('직렬화 테스트', () {
      test('toJson이 올바르게 동작한다', () {
        final fieldType = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        final json = fieldType.toJson();

        expect(json['code'], 'FT01');
        expect(json['name'], '본매대');
      });

      test('fromJson이 올바르게 동작한다', () {
        final json = {
          'code': 'FT01',
          'name': '본매대',
        };

        final fieldType = InspectionFieldType.fromJson(json);

        expect(fieldType.code, 'FT01');
        expect(fieldType.name, '본매대');
      });

      test('toJson과 fromJson이 정확히 왕복 변환된다', () {
        final original = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        final json = original.toJson();
        final restored = InspectionFieldType.fromJson(json);

        expect(restored, original);
      });

      test('여러 현장 유형의 직렬화가 올바르게 동작한다', () {
        final types = [
          InspectionFieldType(code: 'FT01', name: '본매대'),
          InspectionFieldType(code: 'FT02', name: '시식'),
          InspectionFieldType(code: 'FT03', name: '행사매대'),
          InspectionFieldType(code: 'FT99', name: '기타'),
        ];

        final jsonList = types.map((t) => t.toJson()).toList();
        final restored = jsonList
            .map((json) => InspectionFieldType.fromJson(json))
            .toList();

        for (int i = 0; i < types.length; i++) {
          expect(restored[i], types[i]);
        }
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
        final fieldType1 = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        final fieldType2 = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        expect(fieldType1, fieldType2);
        expect(fieldType1.hashCode, fieldType2.hashCode);
      });

      test('다른 값을 가진 엔티티가 다르게 비교된다', () {
        final fieldType1 = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        final fieldType2 = InspectionFieldType(
          code: 'FT02',
          name: '시식',
        );

        expect(fieldType1, isNot(fieldType2));
      });

      test('code만 다르면 다르게 비교된다', () {
        final fieldType1 = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        final fieldType2 = InspectionFieldType(
          code: 'FT02',
          name: '본매대',
        );

        expect(fieldType1, isNot(fieldType2));
      });

      test('name만 다르면 다르게 비교된다', () {
        final fieldType1 = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        final fieldType2 = InspectionFieldType(
          code: 'FT01',
          name: '시식',
        );

        expect(fieldType1, isNot(fieldType2));
      });

      test('자기 자신과 비교하면 동일하다 (identical)', () {
        final fieldType = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        expect(fieldType, fieldType);
      });
    });

    group('toString 테스트', () {
      test('toString이 모든 필드를 포함한다', () {
        final fieldType = InspectionFieldType(
          code: 'FT01',
          name: '본매대',
        );

        final str = fieldType.toString();

        expect(str, contains('FT01'));
        expect(str, contains('본매대'));
      });

      test('다양한 현장 유형의 toString이 올바르게 동작한다', () {
        final types = [
          InspectionFieldType(code: 'FT01', name: '본매대'),
          InspectionFieldType(code: 'FT02', name: '시식'),
          InspectionFieldType(code: 'FT03', name: '행사매대'),
          InspectionFieldType(code: 'FT99', name: '기타'),
        ];

        for (final type in types) {
          final str = type.toString();
          expect(str, contains(type.code));
          expect(str, contains(type.name));
        }
      });
    });
  });
}
