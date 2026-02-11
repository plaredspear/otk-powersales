import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';

void main() {
  group('InspectionCard', () {
    test('자사 InspectionListItem이 올바르게 생성된다', () {
      // Given & When
      final item = InspectionListItem(
        id: 1,
        category: InspectionCategory.OWN,
        storeName: '이마트 죽전점',
        storeId: 100,
        inspectionDate: DateTime(2020, 8, 13),
        fieldType: '본매대',
        fieldTypeCode: 'FT01',
      );

      // Then
      expect(item.id, 1);
      expect(item.category, InspectionCategory.OWN);
      expect(item.storeName, '이마트 죽전점');
      expect(item.fieldType, '본매대');
    });

    test('경쟁사 InspectionListItem이 올바르게 생성된다', () {
      // Given & When
      final item = InspectionListItem(
        id: 2,
        category: InspectionCategory.COMPETITOR,
        storeName: '홈플러스 강남점',
        storeId: 200,
        inspectionDate: DateTime(2020, 8, 14),
        fieldType: '시식',
        fieldTypeCode: 'FT02',
      );

      // Then
      expect(item.id, 2);
      expect(item.category, InspectionCategory.COMPETITOR);
      expect(item.storeName, '홈플러스 강남점');
      expect(item.fieldType, '시식');
    });
  });
}
