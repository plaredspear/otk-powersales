import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/suggestion_form.dart';

void main() {
  group('SuggestionCategory Enum', () {
    test('enum 값이 올바르게 정의된다', () {
      // When & Then
      expect(SuggestionCategory.newProduct.code, 'NEW_PRODUCT');
      expect(SuggestionCategory.newProduct.displayName, '신제품 제안');
      expect(SuggestionCategory.existingProduct.code, 'EXISTING_PRODUCT');
      expect(
          SuggestionCategory.existingProduct.displayName, '기존제품 상품가치향상');
    });

    test('fromCode가 올바른 enum 값을 반환한다', () {
      // When
      final newProduct = SuggestionCategory.fromCode('NEW_PRODUCT');
      final existingProduct =
          SuggestionCategory.fromCode('EXISTING_PRODUCT');

      // Then
      expect(newProduct, SuggestionCategory.newProduct);
      expect(existingProduct, SuggestionCategory.existingProduct);
    });

    test('fromCode에 잘못된 코드를 전달하면 기본값을 반환한다', () {
      // When
      final result = SuggestionCategory.fromCode('INVALID_CODE');

      // Then
      expect(result, SuggestionCategory.newProduct);
    });
  });

  group('SuggestionRegisterForm Entity', () {
    final testPhoto1 = File('test_photo1.jpg');
    final testPhoto2 = File('test_photo2.jpg');

    SuggestionRegisterForm createValidForm({
      SuggestionCategory? category,
      String? productCode,
      String? productName,
      String? title,
      String? content,
      List<File>? photos,
    }) {
      return SuggestionRegisterForm(
        category: category ?? SuggestionCategory.newProduct,
        productCode: productCode,
        productName: productName,
        title: title ?? '저당 라면 시리즈 출시 제안',
        content: content ?? '건강을 생각하는 저당 라면 시리즈를 출시하면 좋을 것 같습니다.',
        photos: photos ?? [],
      );
    }

    group('생성 테스트', () {
      test('신제품 제안 폼이 올바르게 생성된다', () {
        // Given & When
        final form = createValidForm();

        // Then
        expect(form.category, SuggestionCategory.newProduct);
        expect(form.productCode, null);
        expect(form.productName, null);
        expect(form.title, '저당 라면 시리즈 출시 제안');
        expect(form.content, '건강을 생각하는 저당 라면 시리즈를 출시하면 좋을 것 같습니다.');
        expect(form.photos, isEmpty);
      });

      test('기존제품 제안 폼이 올바르게 생성된다', () {
        // Given & When
        final form = createValidForm(
          category: SuggestionCategory.existingProduct,
          productCode: '12345678',
          productName: '진라면',
        );

        // Then
        expect(form.category, SuggestionCategory.existingProduct);
        expect(form.productCode, '12345678');
        expect(form.productName, '진라면');
      });

      test('사진을 포함한 폼이 생성된다', () {
        // Given & When
        final form = createValidForm(
          photos: [testPhoto1, testPhoto2],
        );

        // Then
        expect(form.photos.length, 2);
        expect(form.photos[0], testPhoto1);
        expect(form.photos[1], testPhoto2);
      });
    });

    group('Getter 테스트', () {
      test('isNewProduct가 올바르게 동작한다', () {
        // Given
        final newProductForm = createValidForm(
          category: SuggestionCategory.newProduct,
        );
        final existingProductForm = createValidForm(
          category: SuggestionCategory.existingProduct,
        );

        // Then
        expect(newProductForm.isNewProduct, true);
        expect(existingProductForm.isNewProduct, false);
      });

      test('isExistingProduct가 올바르게 동작한다', () {
        // Given
        final newProductForm = createValidForm(
          category: SuggestionCategory.newProduct,
        );
        final existingProductForm = createValidForm(
          category: SuggestionCategory.existingProduct,
        );

        // Then
        expect(newProductForm.isExistingProduct, false);
        expect(existingProductForm.isExistingProduct, true);
      });

      test('hasProduct가 올바르게 동작한다', () {
        // Given
        final formWithoutProduct = createValidForm();
        final formWithProduct = createValidForm(
          productCode: '12345678',
          productName: '진라면',
        );

        // Then
        expect(formWithoutProduct.hasProduct, false);
        expect(formWithProduct.hasProduct, true);
      });

      test('hasPhotos가 올바르게 동작한다', () {
        // Given
        final formWithoutPhotos = createValidForm();
        final formWithPhotos = createValidForm(
          photos: [testPhoto1],
        );

        // Then
        expect(formWithoutPhotos.hasPhotos, false);
        expect(formWithPhotos.hasPhotos, true);
      });
    });

    group('Validation 테스트', () {
      test('유효한 신제품 제안 폼은 에러가 없다', () {
        // Given
        final form = createValidForm(
          category: SuggestionCategory.newProduct,
          title: '신제품 제안',
          content: '제안 내용',
        );

        // When
        final errors = form.validate();

        // Then
        expect(errors, isEmpty);
        expect(form.isValid, true);
      });

      test('유효한 기존제품 제안 폼은 에러가 없다', () {
        // Given
        final form = createValidForm(
          category: SuggestionCategory.existingProduct,
          productCode: '12345678',
          productName: '진라면',
          title: '기존제품 개선 제안',
          content: '제안 내용',
        );

        // When
        final errors = form.validate();

        // Then
        expect(errors, isEmpty);
        expect(form.isValid, true);
      });

      test('제목이 비어있으면 에러를 반환한다', () {
        // Given
        final form = createValidForm(title: '');

        // When
        final errors = form.validate();

        // Then
        expect(errors, contains('제목을 입력해주세요'));
        expect(form.isValid, false);
      });

      test('제안 내용이 비어있으면 에러를 반환한다', () {
        // Given
        final form = createValidForm(content: '');

        // When
        final errors = form.validate();

        // Then
        expect(errors, contains('제안 내용을 입력해주세요'));
        expect(form.isValid, false);
      });

      test('기존제품 선택 시 제품이 없으면 에러를 반환한다', () {
        // Given
        final form = createValidForm(
          category: SuggestionCategory.existingProduct,
          productCode: null,
          productName: null,
        );

        // When
        final errors = form.validate();

        // Then
        expect(errors, contains('제품을 선택해주세요'));
        expect(form.isValid, false);
      });

      test('사진이 2장을 초과하면 에러를 반환한다', () {
        // Given
        final form = createValidForm(
          photos: [testPhoto1, testPhoto2, File('test_photo3.jpg')],
        );

        // When
        final errors = form.validate();

        // Then
        expect(errors, contains('사진은 최대 2장까지 첨부 가능합니다'));
        expect(form.isValid, false);
      });

      test('여러 에러가 동시에 발생할 수 있다', () {
        // Given
        final form = createValidForm(
          title: '',
          content: '',
          category: SuggestionCategory.existingProduct,
          productCode: null,
        );

        // When
        final errors = form.validate();

        // Then
        expect(errors.length, 3);
        expect(errors, contains('제목을 입력해주세요'));
        expect(errors, contains('제안 내용을 입력해주세요'));
        expect(errors, contains('제품을 선택해주세요'));
      });
    });

    group('copyWith 테스트', () {
      test('copyWith로 일부 필드를 변경할 수 있다', () {
        // Given
        final original = createValidForm();

        // When
        final updated = original.copyWith(
          title: '새로운 제목',
          content: '새로운 내용',
        );

        // Then
        expect(updated.title, '새로운 제목');
        expect(updated.content, '새로운 내용');
        expect(updated.category, original.category);
      });

      test('copyWith로 분류를 변경할 수 있다', () {
        // Given
        final original = createValidForm(
          category: SuggestionCategory.newProduct,
        );

        // When
        final updated = original.copyWith(
          category: SuggestionCategory.existingProduct,
          productCode: '12345678',
          productName: '진라면',
        );

        // Then
        expect(updated.category, SuggestionCategory.existingProduct);
        expect(updated.productCode, '12345678');
        expect(updated.productName, '진라면');
      });

      test('copyWithNull로 제품 정보를 초기화할 수 있다', () {
        // Given
        final original = createValidForm(
          productCode: '12345678',
          productName: '진라면',
        );

        // When
        final updated = original.copyWithNull(
          productCode: true,
          productName: true,
        );

        // Then
        expect(updated.productCode, null);
        expect(updated.productName, null);
        expect(updated.title, original.title);
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티는 동일하다', () {
        // Given
        final form1 = createValidForm();
        final form2 = createValidForm();

        // Then
        expect(form1, form2);
        expect(form1.hashCode, form2.hashCode);
      });

      test('다른 값을 가진 엔티티는 다르다', () {
        // Given
        final form1 = createValidForm(title: '제목1');
        final form2 = createValidForm(title: '제목2');

        // Then
        expect(form1, isNot(form2));
      });

      test('사진이 다르면 엔티티도 다르다', () {
        // Given
        final form1 = createValidForm(photos: [testPhoto1]);
        final form2 = createValidForm(photos: [testPhoto2]);

        // Then
        expect(form1, isNot(form2));
      });
    });

    group('toString 테스트', () {
      test('toString이 올바르게 동작한다', () {
        // Given
        final form = createValidForm(
          photos: [testPhoto1, testPhoto2],
        );

        // When
        final result = form.toString();

        // Then
        expect(result, contains('SuggestionRegisterForm'));
        expect(result, contains('2장'));
      });
    });
  });
}
