import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';

void main() {
  group('InspectionDetailCompetitorWidget', () {
    test('경쟁사 점검 상세 데이터(시식=예)가 올바르게 생성된다', () {
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
        photos: const [],
        createdAt: DateTime(2020, 8, 14, 10, 30),
      );

      // Then
      expect(detail.competitorName, '농심');
      expect(detail.competitorActivity, '시식 행사 진행 중');
      expect(detail.competitorTasting, true);
      expect(detail.competitorProductName, '신라면 블랙');
      expect(detail.competitorProductPrice, 5000);
      expect(detail.competitorSalesQuantity, 50);
    });

    test('경쟁사 점검 상세 데이터(시식=아니요)가 올바르게 생성된다', () {
      // Given & When
      final detail = InspectionDetail(
        id: 3,
        category: InspectionCategory.COMPETITOR,
        storeName: '롯데마트 사당점',
        storeId: 300,
        themeName: '8월 테마',
        themeId: 10,
        inspectionDate: DateTime(2020, 8, 15),
        fieldType: '행사매대',
        fieldTypeCode: 'FT03',
        competitorName: '오뚜기',
        competitorActivity: '행사 진행',
        competitorTasting: false,
        photos: const [],
        createdAt: DateTime(2020, 8, 15, 10, 30),
      );

      // Then
      expect(detail.competitorName, '오뚜기');
      expect(detail.competitorActivity, '행사 진행');
      expect(detail.competitorTasting, false);
      expect(detail.competitorProductName, null);
      expect(detail.competitorProductPrice, null);
      expect(detail.competitorSalesQuantity, null);
    });
  });
}
