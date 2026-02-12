import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/education_category.dart';

void main() {
  group('EducationCategory', () {
    group('enum values', () {
      test('tastingManual has correct properties', () {
        final category = EducationCategory.tastingManual;

        expect(category.code, 'TASTING_MANUAL');
        expect(category.displayName, '시식 매뉴얼');
        expect(category.iconPath, 'assets/icons/education/tasting_manual.png');
      });

      test('csSafety has correct properties', () {
        final category = EducationCategory.csSafety;

        expect(category.code, 'CS_SAFETY');
        expect(category.displayName, 'CS/안전');
        expect(category.iconPath, 'assets/icons/education/cs_safety.png');
      });

      test('evaluation has correct properties', () {
        final category = EducationCategory.evaluation;

        expect(category.code, 'EVALUATION');
        expect(category.displayName, '교육 평가');
        expect(category.iconPath, 'assets/icons/education/evaluation.png');
      });

      test('newProduct has correct properties', () {
        final category = EducationCategory.newProduct;

        expect(category.code, 'NEW_PRODUCT');
        expect(category.displayName, '신제품 소개');
        expect(category.iconPath, 'assets/icons/education/new_product.png');
      });
    });

    group('fromCode', () {
      test('returns correct category for valid code', () {
        expect(
          EducationCategory.fromCode('TASTING_MANUAL'),
          EducationCategory.tastingManual,
        );
        expect(
          EducationCategory.fromCode('CS_SAFETY'),
          EducationCategory.csSafety,
        );
        expect(
          EducationCategory.fromCode('EVALUATION'),
          EducationCategory.evaluation,
        );
        expect(
          EducationCategory.fromCode('NEW_PRODUCT'),
          EducationCategory.newProduct,
        );
      });

      test('throws ArgumentError for invalid code', () {
        expect(
          () => EducationCategory.fromCode('INVALID_CODE'),
          throwsA(isA<ArgumentError>()),
        );
      });
    });
  });
}
