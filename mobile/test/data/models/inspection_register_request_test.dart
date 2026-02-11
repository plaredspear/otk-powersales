import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/inspection_register_request.dart';
import 'package:mobile/domain/entities/inspection_form.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';

void main() {
  group('InspectionRegisterRequest', () {
    // Mock File objects for testing
    final mockPhoto1 = File('/mock/photo1.jpg');
    final mockPhoto2 = File('/mock/photo2.jpg');

    group('fromEntity - 자사', () {
      test('자사 엔티티에서 요청 모델을 생성한다', () {
        // Given
        final entity = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 100,
          inspectionDate: DateTime(2020, 8, 13),
          fieldTypeCode: 'FT01',
          description: '냉장고 앞 본매대',
          productCode: 'P001',
          photos: [mockPhoto1],
        );

        // When
        final request = InspectionRegisterRequest.fromEntity(entity);

        // Then
        expect(request.themeId, 10);
        expect(request.category, 'OWN');
        expect(request.storeId, 100);
        expect(request.inspectionDate, '2020-08-13');
        expect(request.fieldTypeCode, 'FT01');
        expect(request.description, '냉장고 앞 본매대');
        expect(request.productCode, 'P001');
        expect(request.competitorName, null);
        expect(request.competitorActivity, null);
        expect(request.competitorTasting, null);
        expect(request.photos.length, 1);
        expect(request.photos[0].path, mockPhoto1.path);
      });

      test('자사 엔티티에서 설명이 없는 요청 모델을 생성한다', () {
        // Given
        final entity = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 100,
          inspectionDate: DateTime(2020, 8, 13),
          fieldTypeCode: 'FT01',
          productCode: 'P001',
          photos: [mockPhoto1],
        );

        // When
        final request = InspectionRegisterRequest.fromEntity(entity);

        // Then
        expect(request.description, null);
      });
    });

    group('fromEntity - 경쟁사', () {
      test('경쟁사 엔티티에서 요청 모델을 생성한다 (시식=아니오)', () {
        // Given
        final entity = InspectionRegisterForm(
          themeId: 20,
          category: InspectionCategory.COMPETITOR,
          storeId: 200,
          inspectionDate: DateTime(2020, 8, 14),
          fieldTypeCode: 'FT02',
          competitorName: '농심',
          competitorActivity: '시식 행사 진행 중',
          competitorTasting: false,
          photos: [mockPhoto1, mockPhoto2],
        );

        // When
        final request = InspectionRegisterRequest.fromEntity(entity);

        // Then
        expect(request.category, 'COMPETITOR');
        expect(request.competitorName, '농심');
        expect(request.competitorActivity, '시식 행사 진행 중');
        expect(request.competitorTasting, false);
        expect(request.competitorProductName, null);
        expect(request.competitorProductPrice, null);
        expect(request.competitorSalesQuantity, null);
        expect(request.photos.length, 2);
      });

      test('경쟁사 엔티티에서 요청 모델을 생성한다 (시식=예)', () {
        // Given
        final entity = InspectionRegisterForm(
          themeId: 20,
          category: InspectionCategory.COMPETITOR,
          storeId: 200,
          inspectionDate: DateTime(2020, 8, 15),
          fieldTypeCode: 'FT02',
          competitorName: '농심',
          competitorActivity: '신제품 시식 행사',
          competitorTasting: true,
          competitorProductName: '신라면 블랙',
          competitorProductPrice: 5000,
          competitorSalesQuantity: 50,
          photos: [mockPhoto1],
        );

        // When
        final request = InspectionRegisterRequest.fromEntity(entity);

        // Then
        expect(request.competitorTasting, true);
        expect(request.competitorProductName, '신라면 블랙');
        expect(request.competitorProductPrice, 5000);
        expect(request.competitorSalesQuantity, 50);
      });
    });

    group('toFormData', () {
      test('자사 요청 모델을 Form Data로 변환한다', () {
        // Given
        final request = InspectionRegisterRequest(
          themeId: 10,
          category: 'OWN',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldTypeCode: 'FT01',
          description: '냉장고 앞 본매대',
          productCode: 'P001',
          photos: [mockPhoto1],
        );

        // When
        final formData = request.toFormData();

        // Then
        expect(formData['themeId'], 10);
        expect(formData['category'], 'OWN');
        expect(formData['storeId'], 100);
        expect(formData['inspectionDate'], '2020-08-13');
        expect(formData['fieldTypeCode'], 'FT01');
        expect(formData['description'], '냉장고 앞 본매대');
        expect(formData['productCode'], 'P001');
        expect(formData.containsKey('competitorName'), false);
        expect(formData.containsKey('competitorActivity'), false);
        expect(formData.containsKey('competitorTasting'), false);
      });

      test('경쟁사 요청 모델을 Form Data로 변환한다 (시식=예)', () {
        // Given
        const request = InspectionRegisterRequest(
          themeId: 20,
          category: 'COMPETITOR',
          storeId: 200,
          inspectionDate: '2020-08-14',
          fieldTypeCode: 'FT02',
          competitorName: '농심',
          competitorActivity: '신제품 시식 행사',
          competitorTasting: true,
          competitorProductName: '신라면 블랙',
          competitorProductPrice: 5000,
          competitorSalesQuantity: 50,
          photos: [],
        );

        // When
        final formData = request.toFormData();

        // Then
        expect(formData['competitorName'], '농심');
        expect(formData['competitorActivity'], '신제품 시식 행사');
        expect(formData['competitorTasting'], true);
        expect(formData['competitorProductName'], '신라면 블랙');
        expect(formData['competitorProductPrice'], 5000);
        expect(formData['competitorSalesQuantity'], 50);
        expect(formData.containsKey('description'), false);
        expect(formData.containsKey('productCode'), false);
      });

      test('null 값은 Form Data에 포함하지 않는다', () {
        // Given
        final request = InspectionRegisterRequest(
          themeId: 10,
          category: 'OWN',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldTypeCode: 'FT01',
          productCode: 'P001',
          photos: [mockPhoto1],
        );

        // When
        final formData = request.toFormData();

        // Then
        expect(formData.containsKey('description'), false);
        expect(formData.containsKey('competitorName'), false);
        expect(formData.containsKey('competitorActivity'), false);
        expect(formData.containsKey('competitorTasting'), false);
        expect(formData.containsKey('competitorProductName'), false);
        expect(formData.containsKey('competitorProductPrice'), false);
        expect(formData.containsKey('competitorSalesQuantity'), false);
      });
    });

    group('equality and hashCode', () {
      test('같은 값을 가진 인스턴스는 동일하다', () {
        // Given
        final request1 = InspectionRegisterRequest(
          themeId: 10,
          category: 'OWN',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldTypeCode: 'FT01',
          productCode: 'P001',
          photos: [mockPhoto1],
        );
        final request2 = InspectionRegisterRequest(
          themeId: 10,
          category: 'OWN',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldTypeCode: 'FT01',
          productCode: 'P001',
          photos: [mockPhoto1],
        );

        // Then
        expect(request1, request2);
        expect(request1.hashCode, request2.hashCode);
      });

      test('다른 값을 가진 인스턴스는 동일하지 않다', () {
        // Given
        final request1 = InspectionRegisterRequest(
          themeId: 10,
          category: 'OWN',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldTypeCode: 'FT01',
          productCode: 'P001',
          photos: [mockPhoto1],
        );
        final request2 = InspectionRegisterRequest(
          themeId: 20,
          category: 'OWN',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldTypeCode: 'FT01',
          productCode: 'P001',
          photos: [mockPhoto1],
        );

        // Then
        expect(request1, isNot(request2));
      });

      test('자기 자신과 동일하다', () {
        // Given
        final request = InspectionRegisterRequest(
          themeId: 10,
          category: 'OWN',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldTypeCode: 'FT01',
          productCode: 'P001',
          photos: [mockPhoto1],
        );

        // Then
        expect(request, request);
      });
    });

    group('toString', () {
      test('문자열 표현을 반환한다', () {
        // Given
        final request = InspectionRegisterRequest(
          themeId: 10,
          category: 'OWN',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldTypeCode: 'FT01',
          description: '냉장고 앞 본매대',
          productCode: 'P001',
          photos: [mockPhoto1, mockPhoto2],
        );

        // When
        final result = request.toString();

        // Then
        expect(result, contains('InspectionRegisterRequest'));
        expect(result, contains('themeId: 10'));
        expect(result, contains('category: OWN'));
        expect(result, contains('storeId: 100'));
        expect(result, contains('inspectionDate: 2020-08-13'));
        expect(result, contains('fieldTypeCode: FT01'));
        expect(result, contains('description: 냉장고 앞 본매대'));
        expect(result, contains('productCode: P001'));
        expect(result, contains('photos: 2 files'));
      });
    });
  });
}
