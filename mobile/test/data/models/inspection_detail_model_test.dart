import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/inspection_detail_model.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';

void main() {
  group('InspectionPhotoModel', () {
    group('fromJson', () {
      test('JSON에서 모델을 생성한다', () {
        // Given
        final json = {
          'id': 1,
          'url': 'https://example.com/photo1.jpg',
        };

        // When
        final model = InspectionPhotoModel.fromJson(json);

        // Then
        expect(model.id, 1);
        expect(model.url, 'https://example.com/photo1.jpg');
      });
    });

    group('toJson', () {
      test('모델을 JSON으로 직렬화한다', () {
        // Given
        const model = InspectionPhotoModel(
          id: 1,
          url: 'https://example.com/photo1.jpg',
        );

        // When
        final json = model.toJson();

        // Then
        expect(json['id'], 1);
        expect(json['url'], 'https://example.com/photo1.jpg');
      });
    });

    group('toEntity', () {
      test('모델을 엔티티로 변환한다', () {
        // Given
        const model = InspectionPhotoModel(
          id: 1,
          url: 'https://example.com/photo1.jpg',
        );

        // When
        final entity = model.toEntity();

        // Then
        expect(entity.id, 1);
        expect(entity.url, 'https://example.com/photo1.jpg');
      });
    });

    group('fromEntity', () {
      test('엔티티에서 모델을 생성한다', () {
        // Given
        const entity = InspectionPhoto(
          id: 1,
          url: 'https://example.com/photo1.jpg',
        );

        // When
        final model = InspectionPhotoModel.fromEntity(entity);

        // Then
        expect(model.id, 1);
        expect(model.url, 'https://example.com/photo1.jpg');
      });
    });

    group('equality and hashCode', () {
      test('같은 값을 가진 인스턴스는 동일하다', () {
        // Given
        const model1 = InspectionPhotoModel(
          id: 1,
          url: 'https://example.com/photo1.jpg',
        );
        const model2 = InspectionPhotoModel(
          id: 1,
          url: 'https://example.com/photo1.jpg',
        );

        // Then
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('다른 값을 가진 인스턴스는 동일하지 않다', () {
        // Given
        const model1 = InspectionPhotoModel(
          id: 1,
          url: 'https://example.com/photo1.jpg',
        );
        const model2 = InspectionPhotoModel(
          id: 2,
          url: 'https://example.com/photo1.jpg',
        );

        // Then
        expect(model1, isNot(model2));
      });
    });

    group('toString', () {
      test('문자열 표현을 반환한다', () {
        // Given
        const model = InspectionPhotoModel(
          id: 1,
          url: 'https://example.com/photo1.jpg',
        );

        // When
        final result = model.toString();

        // Then
        expect(result, contains('InspectionPhotoModel'));
        expect(result, contains('id: 1'));
        expect(result, contains('url: https://example.com/photo1.jpg'));
      });
    });
  });

  group('InspectionDetailModel', () {
    group('fromJson - 자사', () {
      test('자사 점검 JSON에서 모델을 생성한다', () {
        // Given
        final json = {
          'id': 1,
          'category': 'OWN',
          'storeName': '이마트 죽전점',
          'storeId': 100,
          'themeName': '8월 테마',
          'themeId': 10,
          'inspectionDate': '2020-08-13',
          'fieldType': '본매대',
          'fieldTypeCode': 'FT01',
          'description': '냉장고 앞 본매대',
          'productCode': 'P001',
          'productName': '진라면',
          'photos': [
            {'id': 1, 'url': 'https://example.com/photo1.jpg'},
            {'id': 2, 'url': 'https://example.com/photo2.jpg'},
          ],
          'createdAt': '2020-08-13T10:30:00Z',
        };

        // When
        final model = InspectionDetailModel.fromJson(json);

        // Then
        expect(model.id, 1);
        expect(model.category, 'OWN');
        expect(model.storeName, '이마트 죽전점');
        expect(model.storeId, 100);
        expect(model.themeName, '8월 테마');
        expect(model.themeId, 10);
        expect(model.inspectionDate, '2020-08-13');
        expect(model.fieldType, '본매대');
        expect(model.fieldTypeCode, 'FT01');
        expect(model.description, '냉장고 앞 본매대');
        expect(model.productCode, 'P001');
        expect(model.productName, '진라면');
        expect(model.competitorName, null);
        expect(model.competitorActivity, null);
        expect(model.photos.length, 2);
        expect(model.photos[0].id, 1);
        expect(model.photos[1].url, 'https://example.com/photo2.jpg');
        expect(model.createdAt, '2020-08-13T10:30:00Z');
      });
    });

    group('fromJson - 경쟁사', () {
      test('경쟁사 점검 JSON에서 모델을 생성한다 (시식=아니오)', () {
        // Given
        final json = {
          'id': 2,
          'category': 'COMPETITOR',
          'storeName': '홈플러스 강남점',
          'storeId': 200,
          'themeName': '경쟁사 모니터링',
          'themeId': 20,
          'inspectionDate': '2020-08-14',
          'fieldType': '시식',
          'fieldTypeCode': 'FT02',
          'competitorName': '농심',
          'competitorActivity': '시식 행사 진행 중',
          'competitorTasting': false,
          'photos': [
            {'id': 3, 'url': 'https://example.com/photo3.jpg'},
          ],
          'createdAt': '2020-08-14T11:00:00Z',
        };

        // When
        final model = InspectionDetailModel.fromJson(json);

        // Then
        expect(model.category, 'COMPETITOR');
        expect(model.competitorName, '농심');
        expect(model.competitorActivity, '시식 행사 진행 중');
        expect(model.competitorTasting, false);
        expect(model.competitorProductName, null);
        expect(model.competitorProductPrice, null);
        expect(model.competitorSalesQuantity, null);
      });

      test('경쟁사 점검 JSON에서 모델을 생성한다 (시식=예)', () {
        // Given
        final json = {
          'id': 3,
          'category': 'COMPETITOR',
          'storeName': '롯데마트 잠실점',
          'storeId': 300,
          'themeName': '경쟁사 모니터링',
          'themeId': 20,
          'inspectionDate': '2020-08-15',
          'fieldType': '시식',
          'fieldTypeCode': 'FT02',
          'competitorName': '농심',
          'competitorActivity': '신제품 시식 행사',
          'competitorTasting': true,
          'competitorProductName': '신라면 블랙',
          'competitorProductPrice': 5000,
          'competitorSalesQuantity': 50,
          'photos': [],
          'createdAt': '2020-08-15T14:20:00Z',
        };

        // When
        final model = InspectionDetailModel.fromJson(json);

        // Then
        expect(model.competitorTasting, true);
        expect(model.competitorProductName, '신라면 블랙');
        expect(model.competitorProductPrice, 5000);
        expect(model.competitorSalesQuantity, 50);
      });
    });

    group('toJson', () {
      test('자사 모델을 JSON으로 직렬화한다', () {
        // Given
        final model = InspectionDetailModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          themeName: '8월 테마',
          themeId: 10,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          description: '냉장고 앞 본매대',
          productCode: 'P001',
          productName: '진라면',
          photos: const [
            InspectionPhotoModel(id: 1, url: 'https://example.com/photo1.jpg'),
          ],
          createdAt: '2020-08-13T10:30:00Z',
        );

        // When
        final json = model.toJson();

        // Then
        expect(json['id'], 1);
        expect(json['category'], 'OWN');
        expect(json['description'], '냉장고 앞 본매대');
        expect(json['productCode'], 'P001');
        expect(json['productName'], '진라면');
        expect(json.containsKey('competitorName'), false);
        expect(json['photos'], isA<List>());
        expect((json['photos'] as List).length, 1);
      });

      test('경쟁사 모델을 JSON으로 직렬화한다', () {
        // Given
        const model = InspectionDetailModel(
          id: 2,
          category: 'COMPETITOR',
          storeName: '홈플러스 강남점',
          storeId: 200,
          themeName: '경쟁사 모니터링',
          themeId: 20,
          inspectionDate: '2020-08-14',
          fieldType: '시식',
          fieldTypeCode: 'FT02',
          competitorName: '농심',
          competitorActivity: '시식 행사 진행 중',
          competitorTasting: true,
          competitorProductName: '신라면 블랙',
          competitorProductPrice: 5000,
          competitorSalesQuantity: 50,
          photos: [],
          createdAt: '2020-08-14T11:00:00Z',
        );

        // When
        final json = model.toJson();

        // Then
        expect(json['competitorName'], '농심');
        expect(json['competitorActivity'], '시식 행사 진행 중');
        expect(json['competitorTasting'], true);
        expect(json['competitorProductName'], '신라면 블랙');
        expect(json['competitorProductPrice'], 5000);
        expect(json['competitorSalesQuantity'], 50);
        expect(json.containsKey('description'), false);
        expect(json.containsKey('productCode'), false);
      });
    });

    group('toEntity', () {
      test('자사 모델을 엔티티로 변환한다', () {
        // Given
        final model = InspectionDetailModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          themeName: '8월 테마',
          themeId: 10,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          description: '냉장고 앞 본매대',
          productCode: 'P001',
          productName: '진라면',
          photos: const [
            InspectionPhotoModel(id: 1, url: 'https://example.com/photo1.jpg'),
          ],
          createdAt: '2020-08-13T10:30:00Z',
        );

        // When
        final entity = model.toEntity();

        // Then
        expect(entity.id, 1);
        expect(entity.category, InspectionCategory.OWN);
        expect(entity.storeName, '이마트 죽전점');
        expect(entity.inspectionDate, DateTime(2020, 8, 13));
        expect(entity.description, '냉장고 앞 본매대');
        expect(entity.productCode, 'P001');
        expect(entity.productName, '진라면');
        expect(entity.photos.length, 1);
        expect(entity.photos[0].id, 1);
        expect(entity.createdAt, DateTime.parse('2020-08-13T10:30:00Z'));
      });

      test('경쟁사 모델을 엔티티로 변환한다', () {
        // Given
        const model = InspectionDetailModel(
          id: 2,
          category: 'COMPETITOR',
          storeName: '홈플러스 강남점',
          storeId: 200,
          themeName: '경쟁사 모니터링',
          themeId: 20,
          inspectionDate: '2020-08-14',
          fieldType: '시식',
          fieldTypeCode: 'FT02',
          competitorName: '농심',
          competitorActivity: '시식 행사 진행 중',
          competitorTasting: true,
          competitorProductName: '신라면 블랙',
          competitorProductPrice: 5000,
          competitorSalesQuantity: 50,
          photos: [],
          createdAt: '2020-08-14T11:00:00Z',
        );

        // When
        final entity = model.toEntity();

        // Then
        expect(entity.category, InspectionCategory.COMPETITOR);
        expect(entity.competitorName, '농심');
        expect(entity.competitorTasting, true);
        expect(entity.competitorProductPrice, 5000);
      });
    });

    group('fromEntity', () {
      test('자사 엔티티에서 모델을 생성한다', () {
        // Given
        final entity = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트 죽전점',
          storeId: 100,
          themeName: '8월 테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 13),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          description: '냉장고 앞 본매대',
          productCode: 'P001',
          productName: '진라면',
          photos: const [
            InspectionPhoto(id: 1, url: 'https://example.com/photo1.jpg'),
          ],
          createdAt: DateTime.parse('2020-08-13T10:30:00Z'),
        );

        // When
        final model = InspectionDetailModel.fromEntity(entity);

        // Then
        expect(model.id, 1);
        expect(model.category, 'OWN');
        expect(model.inspectionDate, '2020-08-13');
        expect(model.description, '냉장고 앞 본매대');
        expect(model.photos.length, 1);
        expect(model.createdAt, '2020-08-13T10:30:00.000Z');
      });

      test('경쟁사 엔티티에서 모델을 생성한다', () {
        // Given
        final entity = InspectionDetail(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '홈플러스 강남점',
          storeId: 200,
          themeName: '경쟁사 모니터링',
          themeId: 20,
          inspectionDate: DateTime(2020, 8, 14),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
          competitorName: '농심',
          competitorActivity: '시식 행사 진행 중',
          competitorTasting: true,
          competitorProductName: '신라면 블랙',
          competitorProductPrice: 5000,
          competitorSalesQuantity: 50,
          photos: const [],
          createdAt: DateTime.parse('2020-08-14T11:00:00Z'),
        );

        // When
        final model = InspectionDetailModel.fromEntity(entity);

        // Then
        expect(model.category, 'COMPETITOR');
        expect(model.competitorName, '농심');
        expect(model.competitorTasting, true);
      });
    });

    group('round-trip conversion', () {
      test('Entity → Model → Entity 변환이 정확하다 (자사)', () {
        // Given
        final originalEntity = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트 죽전점',
          storeId: 100,
          themeName: '8월 테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 13),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          description: '냉장고 앞 본매대',
          productCode: 'P001',
          productName: '진라면',
          photos: const [
            InspectionPhoto(id: 1, url: 'https://example.com/photo1.jpg'),
          ],
          createdAt: DateTime.parse('2020-08-13T10:30:00Z'),
        );

        // When
        final model = InspectionDetailModel.fromEntity(originalEntity);
        final convertedEntity = model.toEntity();

        // Then
        expect(convertedEntity.id, originalEntity.id);
        expect(convertedEntity.category, originalEntity.category);
        expect(convertedEntity.description, originalEntity.description);
        expect(convertedEntity.photos.length, originalEntity.photos.length);
      });

      test('JSON → Model → JSON 변환이 정확하다 (경쟁사)', () {
        // Given
        final originalJson = {
          'id': 2,
          'category': 'COMPETITOR',
          'storeName': '홈플러스 강남점',
          'storeId': 200,
          'themeName': '경쟁사 모니터링',
          'themeId': 20,
          'inspectionDate': '2020-08-14',
          'fieldType': '시식',
          'fieldTypeCode': 'FT02',
          'competitorName': '농심',
          'competitorActivity': '시식 행사 진행 중',
          'competitorTasting': true,
          'competitorProductName': '신라면 블랙',
          'competitorProductPrice': 5000,
          'competitorSalesQuantity': 50,
          'photos': [],
          'createdAt': '2020-08-14T11:00:00Z',
        };

        // When
        final model = InspectionDetailModel.fromJson(originalJson);
        final convertedJson = model.toJson();

        // Then
        expect(convertedJson['id'], originalJson['id']);
        expect(convertedJson['category'], originalJson['category']);
        expect(convertedJson['competitorName'], originalJson['competitorName']);
        expect(convertedJson['competitorTasting'], originalJson['competitorTasting']);
        expect(convertedJson['competitorProductPrice'], originalJson['competitorProductPrice']);
      });
    });

    group('equality and hashCode', () {
      test('같은 값을 가진 인스턴스는 동일하다', () {
        // Given
        const model1 = InspectionDetailModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          themeName: '8월 테마',
          themeId: 10,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          photos: [],
          createdAt: '2020-08-13T10:30:00Z',
        );
        const model2 = InspectionDetailModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          themeName: '8월 테마',
          themeId: 10,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          photos: [],
          createdAt: '2020-08-13T10:30:00Z',
        );

        // Then
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('다른 값을 가진 인스턴스는 동일하지 않다', () {
        // Given
        const model1 = InspectionDetailModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          themeName: '8월 테마',
          themeId: 10,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          photos: [],
          createdAt: '2020-08-13T10:30:00Z',
        );
        const model2 = InspectionDetailModel(
          id: 2,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          themeName: '8월 테마',
          themeId: 10,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          photos: [],
          createdAt: '2020-08-13T10:30:00Z',
        );

        // Then
        expect(model1, isNot(model2));
      });
    });

    group('toString', () {
      test('문자열 표현을 반환한다', () {
        // Given
        const model = InspectionDetailModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          themeName: '8월 테마',
          themeId: 10,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          description: '냉장고 앞 본매대',
          photos: [],
          createdAt: '2020-08-13T10:30:00Z',
        );

        // When
        final result = model.toString();

        // Then
        expect(result, contains('InspectionDetailModel'));
        expect(result, contains('id: 1'));
        expect(result, contains('category: OWN'));
        expect(result, contains('description: 냉장고 앞 본매대'));
      });
    });
  });
}
