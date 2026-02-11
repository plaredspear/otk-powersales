import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';

void main() {
  group('InspectionTheme Entity', () {
    group('생성 테스트', () {
      test('InspectionTheme이 올바르게 생성된다', () {
        final theme = InspectionTheme(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        expect(theme.id, 10);
        expect(theme.name, '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)');
        expect(theme.startDate, DateTime(2020, 8, 1));
        expect(theme.endDate, DateTime(2020, 8, 31));
      });
    });

    group('유효성 검증 테스트', () {
      test('오늘이 테마 기간 내에 있으면 isValid가 true', () {
        final now = DateTime.now();
        final theme = InspectionTheme(
          id: 10,
          name: '테스트 테마',
          startDate: now.subtract(const Duration(days: 7)),
          endDate: now.add(const Duration(days: 7)),
        );

        expect(theme.isValid, true);
      });

      test('오늘이 테마 시작일이면 isValid가 true', () {
        final now = DateTime.now();
        final today = DateTime(now.year, now.month, now.day);
        final theme = InspectionTheme(
          id: 10,
          name: '테스트 테마',
          startDate: today,
          endDate: today.add(const Duration(days: 7)),
        );

        expect(theme.isValid, true);
      });

      test('오늘이 테마 종료일이면 isValid가 true', () {
        final now = DateTime.now();
        final today = DateTime(now.year, now.month, now.day);
        final theme = InspectionTheme(
          id: 10,
          name: '테스트 테마',
          startDate: today.subtract(const Duration(days: 7)),
          endDate: today,
        );

        expect(theme.isValid, true);
      });

      test('오늘이 테마 시작일 이전이면 isValid가 false', () {
        final now = DateTime.now();
        final today = DateTime(now.year, now.month, now.day);
        final theme = InspectionTheme(
          id: 10,
          name: '테스트 테마',
          startDate: today.add(const Duration(days: 1)),
          endDate: today.add(const Duration(days: 7)),
        );

        expect(theme.isValid, false);
      });

      test('오늘이 테마 종료일 이후이면 isValid가 false', () {
        final now = DateTime.now();
        final today = DateTime(now.year, now.month, now.day);
        final theme = InspectionTheme(
          id: 10,
          name: '테스트 테마',
          startDate: today.subtract(const Duration(days: 14)),
          endDate: today.subtract(const Duration(days: 1)),
        );

        expect(theme.isValid, false);
      });
    });

    group('periodString 테스트', () {
      test('periodString이 올바른 형식으로 반환된다', () {
        final theme = InspectionTheme(
          id: 10,
          name: '테스트 테마',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        expect(theme.periodString, '2020.08.01 ~ 2020.08.31');
      });

      test('periodString이 한 자리 월/일을 0으로 패딩한다', () {
        final theme = InspectionTheme(
          id: 10,
          name: '테스트 테마',
          startDate: DateTime(2020, 1, 5),
          endDate: DateTime(2020, 9, 15),
        );

        expect(theme.periodString, '2020.01.05 ~ 2020.09.15');
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다 - 모든 필드 변경', () {
        final original = InspectionTheme(
          id: 10,
          name: '테마 1',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        final copied = original.copyWith(
          id: 11,
          name: '테마 2',
          startDate: DateTime(2020, 9, 1),
          endDate: DateTime(2020, 9, 30),
        );

        expect(copied.id, 11);
        expect(copied.name, '테마 2');
        expect(copied.startDate, DateTime(2020, 9, 1));
        expect(copied.endDate, DateTime(2020, 9, 30));
      });

      test('copyWith가 일부 필드만 변경한다', () {
        final original = InspectionTheme(
          id: 10,
          name: '테마 1',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        final copied = original.copyWith(name: '테마 2');

        expect(copied.id, original.id);
        expect(copied.name, '테마 2');
        expect(copied.startDate, original.startDate);
        expect(copied.endDate, original.endDate);
      });

      test('copyWith가 원본을 변경하지 않는다 (불변성)', () {
        final original = InspectionTheme(
          id: 10,
          name: '테마 1',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        final copied = original.copyWith(name: '테마 2');

        expect(original.name, '테마 1');
        expect(copied.name, '테마 2');
      });
    });

    group('직렬화 테스트', () {
      test('toJson이 올바르게 동작한다', () {
        final theme = InspectionTheme(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        final json = theme.toJson();

        expect(json['id'], 10);
        expect(json['name'], '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)');
        expect(json['startDate'], '2020-08-01');
        expect(json['endDate'], '2020-08-31');
      });

      test('fromJson이 올바르게 동작한다', () {
        final json = {
          'id': 10,
          'name': '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          'startDate': '2020-08-01',
          'endDate': '2020-08-31',
        };

        final theme = InspectionTheme.fromJson(json);

        expect(theme.id, 10);
        expect(theme.name, '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)');
        expect(theme.startDate, DateTime(2020, 8, 1));
        expect(theme.endDate, DateTime(2020, 8, 31));
      });

      test('toJson과 fromJson이 정확히 왕복 변환된다', () {
        final original = InspectionTheme(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        final json = original.toJson();
        final restored = InspectionTheme.fromJson(json);

        expect(restored, original);
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
        final theme1 = InspectionTheme(
          id: 10,
          name: '테마 1',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        final theme2 = InspectionTheme(
          id: 10,
          name: '테마 1',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        expect(theme1, theme2);
        expect(theme1.hashCode, theme2.hashCode);
      });

      test('다른 값을 가진 엔티티가 다르게 비교된다', () {
        final theme1 = InspectionTheme(
          id: 10,
          name: '테마 1',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        final theme2 = InspectionTheme(
          id: 11,
          name: '테마 2',
          startDate: DateTime(2020, 9, 1),
          endDate: DateTime(2020, 9, 30),
        );

        expect(theme1, isNot(theme2));
      });

      test('자기 자신과 비교하면 동일하다 (identical)', () {
        final theme = InspectionTheme(
          id: 10,
          name: '테마 1',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        expect(theme, theme);
      });
    });

    group('toString 테스트', () {
      test('toString이 모든 필드를 포함한다', () {
        final theme = InspectionTheme(
          id: 10,
          name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          startDate: DateTime(2020, 8, 1),
          endDate: DateTime(2020, 8, 31),
        );

        final str = theme.toString();

        expect(str, contains('10'));
        expect(str, contains('롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)'));
      });
    });
  });
}
