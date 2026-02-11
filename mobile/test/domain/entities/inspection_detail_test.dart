import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';

void main() {
  group('InspectionPhoto Entity', () {
    group('생성 테스트', () {
      test('InspectionPhoto가 올바르게 생성된다', () {
        final photo = InspectionPhoto(
          id: 1,
          url: 'https://storage.example.com/photos/inspection-1-1.jpg',
        );

        expect(photo.id, 1);
        expect(photo.url, 'https://storage.example.com/photos/inspection-1-1.jpg');
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다', () {
        final original = InspectionPhoto(
          id: 1,
          url: 'https://storage.example.com/photos/inspection-1-1.jpg',
        );

        final copied = original.copyWith(
          id: 2,
          url: 'https://storage.example.com/photos/inspection-1-2.jpg',
        );

        expect(copied.id, 2);
        expect(copied.url, 'https://storage.example.com/photos/inspection-1-2.jpg');
      });

      test('copyWith가 원본을 변경하지 않는다 (불변성)', () {
        final original = InspectionPhoto(
          id: 1,
          url: 'https://storage.example.com/photos/inspection-1-1.jpg',
        );

        final copied = original.copyWith(url: 'https://new-url.com/photo.jpg');

        expect(original.url, 'https://storage.example.com/photos/inspection-1-1.jpg');
        expect(copied.url, 'https://new-url.com/photo.jpg');
      });
    });

    group('직렬화 테스트', () {
      test('toJson이 올바르게 동작한다', () {
        final photo = InspectionPhoto(
          id: 1,
          url: 'https://storage.example.com/photos/inspection-1-1.jpg',
        );

        final json = photo.toJson();

        expect(json['id'], 1);
        expect(json['url'], 'https://storage.example.com/photos/inspection-1-1.jpg');
      });

      test('fromJson이 올바르게 동작한다', () {
        final json = {
          'id': 1,
          'url': 'https://storage.example.com/photos/inspection-1-1.jpg',
        };

        final photo = InspectionPhoto.fromJson(json);

        expect(photo.id, 1);
        expect(photo.url, 'https://storage.example.com/photos/inspection-1-1.jpg');
      });

      test('toJson과 fromJson이 정확히 왕복 변환된다', () {
        final original = InspectionPhoto(
          id: 1,
          url: 'https://storage.example.com/photos/inspection-1-1.jpg',
        );

        final json = original.toJson();
        final restored = InspectionPhoto.fromJson(json);

        expect(restored, original);
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
        final photo1 = InspectionPhoto(
          id: 1,
          url: 'https://storage.example.com/photos/inspection-1-1.jpg',
        );

        final photo2 = InspectionPhoto(
          id: 1,
          url: 'https://storage.example.com/photos/inspection-1-1.jpg',
        );

        expect(photo1, photo2);
        expect(photo1.hashCode, photo2.hashCode);
      });

      test('다른 값을 가진 엔티티가 다르게 비교된다', () {
        final photo1 = InspectionPhoto(
          id: 1,
          url: 'https://storage.example.com/photos/inspection-1-1.jpg',
        );

        final photo2 = InspectionPhoto(
          id: 2,
          url: 'https://storage.example.com/photos/inspection-1-2.jpg',
        );

        expect(photo1, isNot(photo2));
      });
    });

    group('toString 테스트', () {
      test('toString이 모든 필드를 포함한다', () {
        final photo = InspectionPhoto(
          id: 1,
          url: 'https://storage.example.com/photos/inspection-1-1.jpg',
        );

        final str = photo.toString();

        expect(str, contains('1'));
        expect(str, contains('https://storage.example.com/photos/inspection-1-1.jpg'));
      });
    });
  });

  group('InspectionDetail Entity', () {
    group('생성 테스트 - 자사', () {
      test('자사 InspectionDetail이 올바르게 생성된다', () {
        final detail = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '(주)이마트트레이더스명지점',
          storeId: 3001,
          themeName: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          description: '자사 설명',
          productCode: '12345678',
          productName: '맛있는부대찌개라양념140G',
          photos: [
            InspectionPhoto(
              id: 1,
              url: 'https://storage.example.com/photos/inspection-1-1.jpg',
            ),
          ],
          createdAt: DateTime(2020, 8, 19, 10, 30, 0),
        );

        expect(detail.id, 1);
        expect(detail.category, InspectionCategory.OWN);
        expect(detail.storeName, '(주)이마트트레이더스명지점');
        expect(detail.storeId, 3001);
        expect(detail.themeName, '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)');
        expect(detail.themeId, 10);
        expect(detail.inspectionDate, DateTime(2020, 8, 19));
        expect(detail.fieldType, '본매대');
        expect(detail.fieldTypeCode, 'FT01');
        expect(detail.description, '자사 설명');
        expect(detail.productCode, '12345678');
        expect(detail.productName, '맛있는부대찌개라양념140G');
        expect(detail.photos.length, 1);
        expect(detail.createdAt, DateTime(2020, 8, 19, 10, 30, 0));
      });

      test('자사 점검 여부 getter가 올바르게 동작한다', () {
        final detail = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 3001,
          themeName: '테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          productName: '제품명',
          photos: [],
          createdAt: DateTime.now(),
        );

        expect(detail.isOwn, true);
        expect(detail.isCompetitor, false);
      });
    });

    group('생성 테스트 - 경쟁사', () {
      test('경쟁사 InspectionDetail이 올바르게 생성된다', () {
        final detail = InspectionDetail(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '롯데마트 사상',
          storeId: 2001,
          themeName: '8월 테마(영업지원실_영업지원1팀)',
          themeId: 11,
          inspectionDate: DateTime(2020, 8, 25),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: true,
          competitorProductName: '상품1',
          competitorProductPrice: 10000,
          competitorSalesQuantity: 1,
          photos: [
            InspectionPhoto(
              id: 1,
              url: 'https://storage.example.com/photos/inspection-2-1.jpg',
            ),
          ],
          createdAt: DateTime(2020, 8, 25, 14, 0, 0),
        );

        expect(detail.id, 2);
        expect(detail.category, InspectionCategory.COMPETITOR);
        expect(detail.storeName, '롯데마트 사상');
        expect(detail.competitorName, '경쟁사1');
        expect(detail.competitorActivity, '활동1');
        expect(detail.competitorTasting, true);
        expect(detail.competitorProductName, '상품1');
        expect(detail.competitorProductPrice, 10000);
        expect(detail.competitorSalesQuantity, 1);
      });

      test('경쟁사 점검 여부 getter가 올바르게 동작한다', () {
        final detail = InspectionDetail(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '롯데마트 사상',
          storeId: 2001,
          themeName: '테마',
          themeId: 11,
          inspectionDate: DateTime(2020, 8, 25),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: false,
          photos: [],
          createdAt: DateTime.now(),
        );

        expect(detail.isOwn, false);
        expect(detail.isCompetitor, true);
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다 - 일부 필드 변경', () {
        final original = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 3001,
          themeName: '테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          productName: '제품명',
          photos: [],
          createdAt: DateTime(2020, 8, 19, 10, 30, 0),
        );

        final copied = original.copyWith(
          storeName: '롯데마트',
          storeId: 2001,
        );

        expect(copied.id, original.id);
        expect(copied.category, original.category);
        expect(copied.storeName, '롯데마트');
        expect(copied.storeId, 2001);
        expect(copied.themeName, original.themeName);
      });

      test('copyWith가 원본을 변경하지 않는다 (불변성)', () {
        final original = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 3001,
          themeName: '테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          productName: '제품명',
          photos: [],
          createdAt: DateTime(2020, 8, 19, 10, 30, 0),
        );

        final copied = original.copyWith(storeName: '롯데마트');

        expect(original.storeName, '이마트');
        expect(copied.storeName, '롯데마트');
      });
    });

    group('직렬화 테스트 - 자사', () {
      test('자사 점검 toJson이 올바르게 동작한다', () {
        final detail = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 3001,
          themeName: '테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          description: '자사 설명',
          productCode: '12345678',
          productName: '제품명',
          photos: [
            InspectionPhoto(
              id: 1,
              url: 'https://storage.example.com/photos/inspection-1-1.jpg',
            ),
          ],
          createdAt: DateTime(2020, 8, 19, 10, 30, 0),
        );

        final json = detail.toJson();

        expect(json['id'], 1);
        expect(json['category'], 'OWN');
        expect(json['storeName'], '이마트');
        expect(json['description'], '자사 설명');
        expect(json['productCode'], '12345678');
        expect(json['productName'], '제품명');
        expect(json['photos'], isA<List>());
        expect(json['photos'].length, 1);
      });

      test('자사 점검 fromJson이 올바르게 동작한다', () {
        final json = {
          'id': 1,
          'category': 'OWN',
          'storeName': '이마트',
          'storeId': 3001,
          'themeName': '테마',
          'themeId': 10,
          'inspectionDate': '2020-08-19',
          'fieldType': '본매대',
          'fieldTypeCode': 'FT01',
          'description': '자사 설명',
          'productCode': '12345678',
          'productName': '제품명',
          'photos': [
            {'id': 1, 'url': 'https://storage.example.com/photos/inspection-1-1.jpg'},
          ],
          'createdAt': '2020-08-19T10:30:00.000',
        };

        final detail = InspectionDetail.fromJson(json);

        expect(detail.id, 1);
        expect(detail.category, InspectionCategory.OWN);
        expect(detail.description, '자사 설명');
        expect(detail.productCode, '12345678');
        expect(detail.productName, '제품명');
        expect(detail.photos.length, 1);
      });

      test('자사 점검 toJson과 fromJson이 정확히 왕복 변환된다', () {
        final original = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 3001,
          themeName: '테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          description: '자사 설명',
          productCode: '12345678',
          productName: '제품명',
          photos: [
            InspectionPhoto(
              id: 1,
              url: 'https://storage.example.com/photos/inspection-1-1.jpg',
            ),
          ],
          createdAt: DateTime(2020, 8, 19, 10, 30, 0),
        );

        final json = original.toJson();
        final restored = InspectionDetail.fromJson(json);

        expect(restored, original);
      });
    });

    group('직렬화 테스트 - 경쟁사', () {
      test('경쟁사 점검 toJson이 올바르게 동작한다', () {
        final detail = InspectionDetail(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '롯데마트',
          storeId: 2001,
          themeName: '테마',
          themeId: 11,
          inspectionDate: DateTime(2020, 8, 25),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: true,
          competitorProductName: '상품1',
          competitorProductPrice: 10000,
          competitorSalesQuantity: 1,
          photos: [],
          createdAt: DateTime(2020, 8, 25, 14, 0, 0),
        );

        final json = detail.toJson();

        expect(json['id'], 2);
        expect(json['category'], 'COMPETITOR');
        expect(json['competitorName'], '경쟁사1');
        expect(json['competitorActivity'], '활동1');
        expect(json['competitorTasting'], true);
        expect(json['competitorProductName'], '상품1');
        expect(json['competitorProductPrice'], 10000);
        expect(json['competitorSalesQuantity'], 1);
      });

      test('경쟁사 점검 fromJson이 올바르게 동작한다', () {
        final json = {
          'id': 2,
          'category': 'COMPETITOR',
          'storeName': '롯데마트',
          'storeId': 2001,
          'themeName': '테마',
          'themeId': 11,
          'inspectionDate': '2020-08-25',
          'fieldType': '시식',
          'fieldTypeCode': 'FT02',
          'competitorName': '경쟁사1',
          'competitorActivity': '활동1',
          'competitorTasting': true,
          'competitorProductName': '상품1',
          'competitorProductPrice': 10000,
          'competitorSalesQuantity': 1,
          'photos': [],
          'createdAt': '2020-08-25T14:00:00.000',
        };

        final detail = InspectionDetail.fromJson(json);

        expect(detail.id, 2);
        expect(detail.category, InspectionCategory.COMPETITOR);
        expect(detail.competitorName, '경쟁사1');
        expect(detail.competitorActivity, '활동1');
        expect(detail.competitorTasting, true);
        expect(detail.competitorProductName, '상품1');
        expect(detail.competitorProductPrice, 10000);
        expect(detail.competitorSalesQuantity, 1);
      });

      test('경쟁사 점검 toJson과 fromJson이 정확히 왕복 변환된다', () {
        final original = InspectionDetail(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '롯데마트',
          storeId: 2001,
          themeName: '테마',
          themeId: 11,
          inspectionDate: DateTime(2020, 8, 25),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: true,
          competitorProductName: '상품1',
          competitorProductPrice: 10000,
          competitorSalesQuantity: 1,
          photos: [],
          createdAt: DateTime(2020, 8, 25, 14, 0, 0),
        );

        final json = original.toJson();
        final restored = InspectionDetail.fromJson(json);

        expect(restored, original);
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
        final detail1 = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 3001,
          themeName: '테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          productName: '제품명',
          photos: [],
          createdAt: DateTime(2020, 8, 19, 10, 30, 0),
        );

        final detail2 = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 3001,
          themeName: '테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          productName: '제품명',
          photos: [],
          createdAt: DateTime(2020, 8, 19, 10, 30, 0),
        );

        expect(detail1, detail2);
        expect(detail1.hashCode, detail2.hashCode);
      });

      test('다른 값을 가진 엔티티가 다르게 비교된다', () {
        final detail1 = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 3001,
          themeName: '테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          productName: '제품명',
          photos: [],
          createdAt: DateTime(2020, 8, 19, 10, 30, 0),
        );

        final detail2 = InspectionDetail(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '롯데마트',
          storeId: 2001,
          themeName: '테마',
          themeId: 11,
          inspectionDate: DateTime(2020, 8, 25),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: false,
          photos: [],
          createdAt: DateTime(2020, 8, 25, 14, 0, 0),
        );

        expect(detail1, isNot(detail2));
      });

      test('photos 리스트가 다르면 다르게 비교된다', () {
        final detail1 = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 3001,
          themeName: '테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          productName: '제품명',
          photos: [
            InspectionPhoto(id: 1, url: 'url1'),
          ],
          createdAt: DateTime(2020, 8, 19, 10, 30, 0),
        );

        final detail2 = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 3001,
          themeName: '테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          productCode: '12345678',
          productName: '제품명',
          photos: [
            InspectionPhoto(id: 2, url: 'url2'),
          ],
          createdAt: DateTime(2020, 8, 19, 10, 30, 0),
        );

        expect(detail1, isNot(detail2));
      });
    });

    group('toString 테스트', () {
      test('toString이 모든 필드를 포함한다 - 자사', () {
        final detail = InspectionDetail(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트',
          storeId: 3001,
          themeName: '테마',
          themeId: 10,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          description: '자사 설명',
          productCode: '12345678',
          productName: '제품명',
          photos: [],
          createdAt: DateTime(2020, 8, 19, 10, 30, 0),
        );

        final str = detail.toString();

        expect(str, contains('1'));
        expect(str, contains('OWN'));
        expect(str, contains('이마트'));
        expect(str, contains('자사 설명'));
        expect(str, contains('12345678'));
        expect(str, contains('제품명'));
      });

      test('toString이 모든 필드를 포함한다 - 경쟁사', () {
        final detail = InspectionDetail(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '롯데마트',
          storeId: 2001,
          themeName: '테마',
          themeId: 11,
          inspectionDate: DateTime(2020, 8, 25),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
          competitorName: '경쟁사1',
          competitorActivity: '활동1',
          competitorTasting: true,
          photos: [],
          createdAt: DateTime(2020, 8, 25, 14, 0, 0),
        );

        final str = detail.toString();

        expect(str, contains('2'));
        expect(str, contains('COMPETITOR'));
        expect(str, contains('롯데마트'));
        expect(str, contains('경쟁사1'));
        expect(str, contains('활동1'));
      });
    });
  });
}
