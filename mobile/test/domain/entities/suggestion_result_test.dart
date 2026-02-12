import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/suggestion_form.dart';
import 'package:mobile/domain/entities/suggestion_result.dart';

void main() {
  group('SuggestionRegisterResult Entity', () {
    SuggestionRegisterResult createValidResult({
      int? id,
      SuggestionCategory? category,
      String? categoryName,
      String? productCode,
      String? productName,
      String? title,
      DateTime? createdAt,
    }) {
      return SuggestionRegisterResult(
        id: id ?? 50,
        category: category ?? SuggestionCategory.newProduct,
        categoryName: categoryName ?? '신제품 제안',
        productCode: productCode,
        productName: productName,
        title: title ?? '저당 라면 시리즈 출시 제안',
        createdAt: createdAt ?? DateTime(2026, 2, 11, 11, 0, 0),
      );
    }

    group('생성 테스트', () {
      test('신제품 제안 결과가 올바르게 생성된다', () {
        // Given & When
        final result = createValidResult();

        // Then
        expect(result.id, 50);
        expect(result.category, SuggestionCategory.newProduct);
        expect(result.categoryName, '신제품 제안');
        expect(result.productCode, null);
        expect(result.productName, null);
        expect(result.title, '저당 라면 시리즈 출시 제안');
        expect(result.createdAt, DateTime(2026, 2, 11, 11, 0, 0));
      });

      test('기존제품 제안 결과가 올바르게 생성된다', () {
        // Given & When
        final result = createValidResult(
          category: SuggestionCategory.existingProduct,
          categoryName: '기존제품 상품가치향상',
          productCode: '12345678',
          productName: '진라면',
        );

        // Then
        expect(result.category, SuggestionCategory.existingProduct);
        expect(result.categoryName, '기존제품 상품가치향상');
        expect(result.productCode, '12345678');
        expect(result.productName, '진라면');
      });
    });

    group('JSON 직렬화 테스트', () {
      test('toJson이 올바르게 동작한다', () {
        // Given
        final result = createValidResult();

        // When
        final json = result.toJson();

        // Then
        expect(json['id'], 50);
        expect(json['category'], 'NEW_PRODUCT');
        expect(json['categoryName'], '신제품 제안');
        expect(json['productCode'], null);
        expect(json['productName'], null);
        expect(json['title'], '저당 라면 시리즈 출시 제안');
        expect(json['createdAt'], '2026-02-11T11:00:00.000');
      });

      test('fromJson이 올바르게 동작한다 (신제품)', () {
        // Given
        final json = {
          'id': 50,
          'category': 'NEW_PRODUCT',
          'categoryName': '신제품 제안',
          'productCode': null,
          'productName': null,
          'title': '저당 라면 시리즈 출시 제안',
          'createdAt': '2026-02-11T11:00:00.000',
        };

        // When
        final result = SuggestionRegisterResult.fromJson(json);

        // Then
        expect(result.id, 50);
        expect(result.category, SuggestionCategory.newProduct);
        expect(result.categoryName, '신제품 제안');
        expect(result.productCode, null);
        expect(result.productName, null);
        expect(result.title, '저당 라면 시리즈 출시 제안');
        expect(result.createdAt, DateTime(2026, 2, 11, 11, 0, 0));
      });

      test('fromJson이 올바르게 동작한다 (기존제품)', () {
        // Given
        final json = {
          'id': 51,
          'category': 'EXISTING_PRODUCT',
          'categoryName': '기존제품 상품가치향상',
          'productCode': '12345678',
          'productName': '진라면',
          'title': '진라면 매운맛 개선 제안',
          'createdAt': '2026-02-11T14:30:00.000',
        };

        // When
        final result = SuggestionRegisterResult.fromJson(json);

        // Then
        expect(result.id, 51);
        expect(result.category, SuggestionCategory.existingProduct);
        expect(result.categoryName, '기존제품 상품가치향상');
        expect(result.productCode, '12345678');
        expect(result.productName, '진라면');
        expect(result.title, '진라면 매운맛 개선 제안');
        expect(result.createdAt, DateTime(2026, 2, 11, 14, 30, 0));
      });

      test('toJson과 fromJson이 정확히 변환된다', () {
        // Given
        final original = createValidResult();

        // When
        final json = original.toJson();
        final restored = SuggestionRegisterResult.fromJson(json);

        // Then
        expect(restored, original);
      });
    });

    group('copyWith 테스트', () {
      test('copyWith로 일부 필드를 변경할 수 있다', () {
        // Given
        final original = createValidResult();

        // When
        final updated = original.copyWith(
          id: 100,
          title: '새로운 제목',
        );

        // Then
        expect(updated.id, 100);
        expect(updated.title, '새로운 제목');
        expect(updated.category, original.category);
        expect(updated.createdAt, original.createdAt);
      });

      test('copyWith로 모든 필드를 변경할 수 있다', () {
        // Given
        final original = createValidResult();
        final newDate = DateTime(2026, 3, 1, 10, 0, 0);

        // When
        final updated = original.copyWith(
          id: 200,
          category: SuggestionCategory.existingProduct,
          categoryName: '기존제품 상품가치향상',
          productCode: '87654321',
          productName: '너구리',
          title: '너구리 개선 제안',
          createdAt: newDate,
        );

        // Then
        expect(updated.id, 200);
        expect(updated.category, SuggestionCategory.existingProduct);
        expect(updated.categoryName, '기존제품 상품가치향상');
        expect(updated.productCode, '87654321');
        expect(updated.productName, '너구리');
        expect(updated.title, '너구리 개선 제안');
        expect(updated.createdAt, newDate);
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티는 동일하다', () {
        // Given
        final result1 = createValidResult();
        final result2 = createValidResult();

        // Then
        expect(result1, result2);
        expect(result1.hashCode, result2.hashCode);
      });

      test('다른 값을 가진 엔티티는 다르다', () {
        // Given
        final result1 = createValidResult(id: 50);
        final result2 = createValidResult(id: 51);

        // Then
        expect(result1, isNot(result2));
      });

      test('제품 정보가 다르면 엔티티도 다르다', () {
        // Given
        final result1 = createValidResult(
          productCode: '12345678',
          productName: '진라면',
        );
        final result2 = createValidResult(
          productCode: '87654321',
          productName: '너구리',
        );

        // Then
        expect(result1, isNot(result2));
      });
    });

    group('toString 테스트', () {
      test('toString이 올바르게 동작한다 (신제품)', () {
        // Given
        final result = createValidResult();

        // When
        final str = result.toString();

        // Then
        expect(str, contains('SuggestionRegisterResult'));
        expect(str, contains('id: 50'));
        expect(str, contains('신제품 제안'));
        expect(str, contains('저당 라면 시리즈 출시 제안'));
      });

      test('toString이 올바르게 동작한다 (기존제품)', () {
        // Given
        final result = createValidResult(
          category: SuggestionCategory.existingProduct,
          categoryName: '기존제품 상품가치향상',
          productCode: '12345678',
          productName: '진라면',
        );

        // When
        final str = result.toString();

        // Then
        expect(str, contains('SuggestionRegisterResult'));
        expect(str, contains('기존제품 상품가치향상'));
        expect(str, contains('productCode: 12345678'));
        expect(str, contains('productName: 진라면'));
      });
    });
  });
}
