import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/suggestion_register_result_model.dart';
import 'package:mobile/domain/entities/suggestion_form.dart';

void main() {
  group('SuggestionRegisterResultModel', () {
    group('fromJson 역직렬화', () {
      test('신제품 제안 응답을 Model로 변환한다', () {
        // Given
        final json = {
          'id': 50,
          'category': 'NEW_PRODUCT',
          'categoryName': '신제품 제안',
          'productCode': null,
          'productName': null,
          'title': '저당 라면 시리즈 출시 제안',
          'createdAt': '2026-02-11T11:00:00',
        };

        // When
        final model = SuggestionRegisterResultModel.fromJson(json);

        // Then
        expect(model.id, 50);
        expect(model.category, 'NEW_PRODUCT');
        expect(model.categoryName, '신제품 제안');
        expect(model.productCode, null);
        expect(model.productName, null);
        expect(model.title, '저당 라면 시리즈 출시 제안');
        expect(model.createdAt, '2026-02-11T11:00:00');
      });

      test('기존제품 제안 응답을 Model로 변환한다', () {
        // Given
        final json = {
          'id': 51,
          'category': 'EXISTING_PRODUCT',
          'categoryName': '기존제품 상품가치향상',
          'productCode': '12345678',
          'productName': '진라면',
          'title': '진라면 매운맛 개선 제안',
          'createdAt': '2026-02-11T14:30:00',
        };

        // When
        final model = SuggestionRegisterResultModel.fromJson(json);

        // Then
        expect(model.id, 51);
        expect(model.category, 'EXISTING_PRODUCT');
        expect(model.categoryName, '기존제품 상품가치향상');
        expect(model.productCode, '12345678');
        expect(model.productName, '진라면');
        expect(model.title, '진라면 매운맛 개선 제안');
        expect(model.createdAt, '2026-02-11T14:30:00');
      });
    });

    group('toEntity 변환', () {
      test('신제품 제안 Model을 Entity로 변환한다', () {
        // Given
        final model = SuggestionRegisterResultModel(
          id: 50,
          category: 'NEW_PRODUCT',
          categoryName: '신제품 제안',
          productCode: null,
          productName: null,
          title: '저당 라면 시리즈 출시 제안',
          createdAt: '2026-02-11T11:00:00',
        );

        // When
        final entity = model.toEntity();

        // Then
        expect(entity.id, 50);
        expect(entity.category, SuggestionCategory.newProduct);
        expect(entity.categoryName, '신제품 제안');
        expect(entity.productCode, null);
        expect(entity.productName, null);
        expect(entity.title, '저당 라면 시리즈 출시 제안');
        expect(entity.createdAt, DateTime(2026, 2, 11, 11, 0, 0));
      });

      test('기존제품 제안 Model을 Entity로 변환한다', () {
        // Given
        final model = SuggestionRegisterResultModel(
          id: 51,
          category: 'EXISTING_PRODUCT',
          categoryName: '기존제품 상품가치향상',
          productCode: '12345678',
          productName: '진라면',
          title: '진라면 매운맛 개선 제안',
          createdAt: '2026-02-11T14:30:00',
        );

        // When
        final entity = model.toEntity();

        // Then
        expect(entity.id, 51);
        expect(entity.category, SuggestionCategory.existingProduct);
        expect(entity.categoryName, '기존제품 상품가치향상');
        expect(entity.productCode, '12345678');
        expect(entity.productName, '진라면');
        expect(entity.title, '진라면 매운맛 개선 제안');
        expect(entity.createdAt, DateTime(2026, 2, 11, 14, 30, 0));
      });

      test('fromJson → toEntity 변환이 정확히 동작한다', () {
        // Given
        final json = {
          'id': 52,
          'category': 'NEW_PRODUCT',
          'categoryName': '신제품 제안',
          'productCode': null,
          'productName': null,
          'title': '새로운 제안',
          'createdAt': '2026-02-12T10:00:00',
        };

        // When
        final model = SuggestionRegisterResultModel.fromJson(json);
        final entity = model.toEntity();

        // Then
        expect(entity.id, 52);
        expect(entity.category, SuggestionCategory.newProduct);
        expect(entity.title, '새로운 제안');
        expect(entity.createdAt, DateTime(2026, 2, 12, 10, 0, 0));
      });
    });
  });
}
