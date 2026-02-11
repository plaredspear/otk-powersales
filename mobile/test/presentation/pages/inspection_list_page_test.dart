import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';

void main() {
  group('InspectionListPage', () {
    test('InspectionListItem 목록이 올바르게 생성된다', () {
      // Given
      final items = [
        InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: DateTime(2020, 8, 13),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        ),
        InspectionListItem(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '홈플러스 강남점',
          storeId: 200,
          inspectionDate: DateTime(2020, 8, 14),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        ),
      ];

      // Then
      expect(items.length, 2);
      expect(items[0].category, InspectionCategory.OWN);
      expect(items[1].category, InspectionCategory.COMPETITOR);
    });
  });
}
