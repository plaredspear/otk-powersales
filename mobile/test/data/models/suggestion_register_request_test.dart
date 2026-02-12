import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/suggestion_register_request.dart';
import 'package:mobile/domain/entities/suggestion_form.dart';

void main() {
  group('SuggestionRegisterRequest Model', () {
    final testPhoto1 = File('test_photo1.jpg');
    final testPhoto2 = File('test_photo2.jpg');

    group('fromEntity 변환', () {
      test('신제품 제안 폼에서 변환된다', () {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.newProduct,
          title: '저당 라면 시리즈 출시 제안',
          content: '건강을 생각하는 저당 라면 시리즈를 출시하면 좋을 것 같습니다.',
        );

        // When
        final request = SuggestionRegisterRequest.fromEntity(form);

        // Then
        expect(request.category, 'NEW_PRODUCT');
        expect(request.productCode, null);
        expect(request.title, '저당 라면 시리즈 출시 제안');
        expect(request.content, '건강을 생각하는 저당 라면 시리즈를 출시하면 좋을 것 같습니다.');
        expect(request.photos, isEmpty);
      });

      test('기존제품 제안 폼에서 변환된다', () {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.existingProduct,
          productCode: '12345678',
          productName: '진라면',
          title: '진라면 매운맛 개선 제안',
          content: '매운맛을 더 강화하면 좋을 것 같습니다.',
        );

        // When
        final request = SuggestionRegisterRequest.fromEntity(form);

        // Then
        expect(request.category, 'EXISTING_PRODUCT');
        expect(request.productCode, '12345678');
        expect(request.title, '진라면 매운맛 개선 제안');
        expect(request.content, '매운맛을 더 강화하면 좋을 것 같습니다.');
      });

      test('사진을 포함한 폼에서 변환된다', () {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.newProduct,
          title: '신제품 제안',
          content: '제안 내용',
          photos: [testPhoto1, testPhoto2],
        );

        // When
        final request = SuggestionRegisterRequest.fromEntity(form);

        // Then
        expect(request.photos.length, 2);
        expect(request.photos[0].path, 'test_photo1.jpg');
        expect(request.photos[1].path, 'test_photo2.jpg');
      });
    });

    group('toFormData 변환', () {
      test('신제품 제안 요청이 FormData로 변환된다', () async {
        // Given
        final request = SuggestionRegisterRequest(
          category: 'NEW_PRODUCT',
          title: '저당 라면 시리즈 출시 제안',
          content: '건강을 생각하는 저당 라면 시리즈를 출시하면 좋을 것 같습니다.',
        );

        // When
        final formData = await request.toFormData();

        // Then
        expect(formData.fields.any((e) => e.key == 'category' && e.value == 'NEW_PRODUCT'), true);
        expect(formData.fields.any((e) => e.key == 'title'), true);
        expect(formData.fields.any((e) => e.key == 'content'), true);
        expect(formData.fields.any((e) => e.key == 'productCode'), false);
      });

      test('기존제품 제안 요청이 FormData로 변환된다', () async {
        // Given
        final request = SuggestionRegisterRequest(
          category: 'EXISTING_PRODUCT',
          productCode: '12345678',
          title: '진라면 매운맛 개선 제안',
          content: '매운맛을 더 강화하면 좋을 것 같습니다.',
        );

        // When
        final formData = await request.toFormData();

        // Then
        expect(formData.fields.any((e) => e.key == 'category' && e.value == 'EXISTING_PRODUCT'), true);
        expect(formData.fields.any((e) => e.key == 'productCode' && e.value == '12345678'), true);
        expect(formData.fields.any((e) => e.key == 'title'), true);
        expect(formData.fields.any((e) => e.key == 'content'), true);
      });

      test('productCode가 null이면 FormData에 포함되지 않는다', () async {
        // Given
        final request = SuggestionRegisterRequest(
          category: 'NEW_PRODUCT',
          productCode: null,
          title: '신제품 제안',
          content: '제안 내용',
        );

        // When
        final formData = await request.toFormData();

        // Then
        expect(formData.fields.any((e) => e.key == 'productCode'), false);
      });

      test('productCode가 빈 문자열이면 FormData에 포함되지 않는다', () async {
        // Given
        final request = SuggestionRegisterRequest(
          category: 'NEW_PRODUCT',
          productCode: '',
          title: '신제품 제안',
          content: '제안 내용',
        );

        // When
        final formData = await request.toFormData();

        // Then
        expect(formData.fields.any((e) => e.key == 'productCode'), false);
      });
    });
  });
}
