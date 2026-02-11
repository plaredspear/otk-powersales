import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_form.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';

void main() {
  group('ValidationResult', () {
    test('유효한 결과가 올바르게 생성된다', () {
      final result = ValidationResult(isValid: true, errors: []);

      expect(result.isValid, true);
      expect(result.errors, isEmpty);
      expect(result.firstError, isNull);
    });

    test('무효한 결과가 올바르게 생성된다', () {
      final result = ValidationResult(
        isValid: false,
        errors: ['에러1', '에러2'],
      );

      expect(result.isValid, false);
      expect(result.errors.length, 2);
      expect(result.firstError, '에러1');
    });

    test('Equality가 올바르게 동작한다', () {
      final result1 = ValidationResult(isValid: true, errors: []);
      final result2 = ValidationResult(isValid: true, errors: []);

      expect(result1, result2);
      expect(result1.hashCode, result2.hashCode);
    });
  });

  group('InspectionRegisterForm Entity', () {
    // Mock File 생성 헬퍼
    File createMockFile(String path) {
      return File(path);
    }

    group('생성 테스트 - 자사', () {
      test('자사 InspectionRegisterForm이 올바르게 생성된다', () {
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          description: '자사 설명',
          productCode: '12345678',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        expect(form.themeId, 10);
        expect(form.category, InspectionCategory.OWN);
        expect(form.storeId, 3001);
        expect(form.inspectionDate, DateTime(2020, 8, 19));
        expect(form.fieldTypeCode, 'FT01');
        expect(form.description, '자사 설명');
        expect(form.productCode, '12345678');
        expect(form.photos.length, 1);
      });

      test('자사 점검 여부 getter가 올바르게 동작한다', () {
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        expect(form.isOwn, true);
        expect(form.isCompetitor, false);
      });
    });

    group('생성 테스트 - 경쟁사', () {
      test('경쟁사 InspectionRegisterForm이 올바르게 생성된다', () {
        final form = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: true,
          competitorProductName: '상품1',
          competitorProductPrice: 10000,
          competitorSalesQuantity: 1,
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        expect(form.themeId, 11);
        expect(form.category, InspectionCategory.COMPETITOR);
        expect(form.competitorName, '경쟁사1');
        expect(form.competitorActivity, '활동1');
        expect(form.competitorTasting, true);
        expect(form.competitorProductName, '상품1');
        expect(form.competitorProductPrice, 10000);
        expect(form.competitorSalesQuantity, 1);
      });

      test('경쟁사 점검 여부 getter가 올바르게 동작한다', () {
        final form = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: false,
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        expect(form.isOwn, false);
        expect(form.isCompetitor, true);
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다 - 일부 필드 변경', () {
        final original = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final copied = original.copyWith(
          storeId: 2001,
          fieldTypeCode: 'FT02',
        );

        expect(copied.themeId, original.themeId);
        expect(copied.category, original.category);
        expect(copied.storeId, 2001);
        expect(copied.fieldTypeCode, 'FT02');
        expect(copied.productCode, original.productCode);
      });

      test('copyWith가 원본을 변경하지 않는다 (불변성)', () {
        final original = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final copied = original.copyWith(storeId: 2001);

        expect(original.storeId, 3001);
        expect(copied.storeId, 2001);
      });
    });

    group('유효성 검증 테스트 - 공통', () {
      test('모든 공통 필수 항목이 유효하면 검증 통과', () {
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, true);
        expect(result.errors, isEmpty);
      });

      test('themeId가 0 이하면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 0,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('테마를 선택해주세요'));
      });

      test('storeId가 0 이하면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 0,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('거래처를 선택해주세요'));
      });

      test('fieldTypeCode가 비어있으면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: '',
          productCode: '12345678',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('현장 유형을 선택해주세요'));
      });

      test('사진이 없으면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          photos: [],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('사진을 1장 이상 첨부해주세요'));
      });

      test('사진이 3장 이상이면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          photos: [
            createMockFile('/path/to/photo1.jpg'),
            createMockFile('/path/to/photo2.jpg'),
            createMockFile('/path/to/photo3.jpg'),
          ],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('사진은 최대 2장까지 첨부 가능합니다'));
      });

      test('사진이 2장이면 검증 통과', () {
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          photos: [
            createMockFile('/path/to/photo1.jpg'),
            createMockFile('/path/to/photo2.jpg'),
          ],
        );

        final result = form.validate();

        expect(result.isValid, true);
      });
    });

    group('유효성 검증 테스트 - 자사', () {
      test('자사 점검에서 productCode가 없으면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('제품을 선택해주세요'));
      });

      test('자사 점검에서 productCode가 빈 문자열이면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('제품을 선택해주세요'));
      });

      test('자사 점검에서 모든 필수 항목이 유효하면 검증 통과', () {
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          description: '자사 설명',
          productCode: '12345678',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, true);
        expect(result.errors, isEmpty);
      });
    });

    group('유효성 검증 테스트 - 경쟁사', () {
      test('경쟁사 점검에서 competitorName이 없으면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorActivity: '활동1',
          competitorTasting: false,
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('경쟁사명을 입력해주세요'));
      });

      test('경쟁사 점검에서 competitorActivity가 없으면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorTasting: false,
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('경쟁사 활동 내용을 입력해주세요'));
      });

      test('경쟁사 점검에서 competitorTasting이 null이면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('경쟁사 상품 시식 여부를 선택해주세요'));
      });

      test('경쟁사 점검 시식=아니요일 때 모든 필수 항목이 유효하면 검증 통과', () {
        final form = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: false,
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, true);
        expect(result.errors, isEmpty);
      });
    });

    group('유효성 검증 테스트 - 경쟁사 시식=예', () {
      test('시식=예일 때 competitorProductName이 없으면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: true,
          competitorProductPrice: 10000,
          competitorSalesQuantity: 1,
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('경쟁사 상품명을 입력해주세요'));
      });

      test('시식=예일 때 competitorProductPrice가 없으면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: true,
          competitorProductName: '상품1',
          competitorSalesQuantity: 1,
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('제품 가격을 입력해주세요'));
      });

      test('시식=예일 때 competitorProductPrice가 음수면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: true,
          competitorProductName: '상품1',
          competitorProductPrice: -100,
          competitorSalesQuantity: 1,
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('제품 가격은 0 이상이어야 합니다'));
      });

      test('시식=예일 때 competitorSalesQuantity가 없으면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: true,
          competitorProductName: '상품1',
          competitorProductPrice: 10000,
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('판매 수량을 입력해주세요'));
      });

      test('시식=예일 때 competitorSalesQuantity가 음수면 검증 실패', () {
        final form = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: true,
          competitorProductName: '상품1',
          competitorProductPrice: 10000,
          competitorSalesQuantity: -1,
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors, contains('판매 수량은 0 이상이어야 합니다'));
      });

      test('경쟁사 시식=예일 때 모든 필수 항목이 유효하면 검증 통과', () {
        final form = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: true,
          competitorProductName: '상품1',
          competitorProductPrice: 10000,
          competitorSalesQuantity: 1,
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final result = form.validate();

        expect(result.isValid, true);
        expect(result.errors, isEmpty);
      });

      test('여러 필드가 동시에 검증 실패하면 모든 에러 메시지를 반환', () {
        final form = InspectionRegisterForm(
          themeId: 0,
          category: InspectionCategory.COMPETITOR,
          storeId: 0,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: '',
          competitorName: '',
          competitorActivity: '',
          competitorTasting: null,
          photos: [],
        );

        final result = form.validate();

        expect(result.isValid, false);
        expect(result.errors.length, greaterThan(1));
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
        final file1 = createMockFile('/path/to/photo1.jpg');
        final form1 = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          photos: [file1],
        );

        final form2 = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          photos: [file1],
        );

        expect(form1, form2);
        expect(form1.hashCode, form2.hashCode);
      });

      test('다른 값을 가진 엔티티가 다르게 비교된다', () {
        final form1 = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final form2 = InspectionRegisterForm(
          themeId: 11,
          category: InspectionCategory.COMPETITOR,
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 25),
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: false,
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        expect(form1, isNot(form2));
      });
    });

    group('toString 테스트', () {
      test('toString이 모든 필드를 포함한다', () {
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 3001,
          inspectionDate: DateTime(2020, 8, 19),
          fieldTypeCode: 'FT01',
          description: '자사 설명',
          productCode: '12345678',
          photos: [createMockFile('/path/to/photo1.jpg')],
        );

        final str = form.toString();

        expect(str, contains('10'));
        expect(str, contains('OWN'));
        expect(str, contains('3001'));
        expect(str, contains('FT01'));
        expect(str, contains('12345678'));
        expect(str, contains('1 files'));
      });
    });
  });
}
