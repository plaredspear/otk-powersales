import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';

void main() {
  group('InspectionDetailOwnWidget', () {
    test('자사 점검 상세 데이터가 올바르게 생성된다', () {
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
        photos: const [],
        createdAt: DateTime(2020, 8, 13, 10, 30),
      );

      // Then
      expect(detail.productName, '진라면');
      expect(detail.productCode, 'P001');
      expect(detail.description, '냉장고 앞 본매대');
      expect(detail.category, InspectionCategory.OWN);
    });

    test('설명이 없는 자사 점검 상세 데이터를 처리한다', () {
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
        productCode: 'P001',
        productName: '진라면',
        photos: const [],
        createdAt: DateTime(2020, 8, 13, 10, 30),
      );

      // Then
      expect(detail.description, null);
      expect(detail.productName, '진라면');
    });
  });
}
