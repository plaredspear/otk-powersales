import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';

void main() {
  group('InspectionDetailPage', () {
    test('자사 점검 상세 데이터가 올바르게 구성된다', () {
      // Given & When
      final detail = InspectionDetail(
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
          InspectionPhoto(id: 1, url: 'photo1.jpg'),
          InspectionPhoto(id: 2, url: 'photo2.jpg'),
        ],
        createdAt: DateTime(2020, 8, 13, 10, 30),
      );

      // Then
      expect(detail.id, 1);
      expect(detail.category, InspectionCategory.OWN);
      expect(detail.storeName, '이마트 죽전점');
      expect(detail.productName, '진라면');
      expect(detail.photos.length, 2);
    });

    test('경쟁사 점검 상세 데이터가 올바르게 구성된다', () {
      // Given & When
      final detail = InspectionDetail(
        id: 2,
        category: InspectionCategory.COMPETITOR,
        storeName: '홈플러스 강남점',
        storeId: 200,
        themeName: '8월 테마',
        themeId: 10,
        inspectionDate: DateTime(2020, 8, 14),
        fieldType: '시식',
        fieldTypeCode: 'FT02',
        competitorName: '농심',
        competitorActivity: '시식 행사 진행 중',
        competitorTasting: true,
        competitorProductName: '신라면 블랙',
        competitorProductPrice: 5000,
        competitorSalesQuantity: 50,
        photos: const [
          InspectionPhoto(id: 1, url: 'photo1.jpg'),
        ],
        createdAt: DateTime(2020, 8, 14, 10, 30),
      );

      // Then
      expect(detail.id, 2);
      expect(detail.category, InspectionCategory.COMPETITOR);
      expect(detail.storeName, '홈플러스 강남점');
      expect(detail.competitorName, '농심');
      expect(detail.competitorTasting, true);
      expect(detail.photos.length, 1);
    });
  });
}
