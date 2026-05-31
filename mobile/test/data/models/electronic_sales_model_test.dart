import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/electronic_sales_model.dart';

void main() {
  group('ElectronicSalesModel.listFromJson', () {
    test('data.items 를 제품별 ElectronicSales 목록으로 펼치고 공통 필드를 복제한다', () {
      final data = {
        'customerId': 1,
        'customerName': '사과마을',
        'sapAccountCode': '12345',
        'yearMonth': '202602',
        'items': [
          {
            'productCode': '01101123',
            'productName': '갈릭 아이올리소스 240g',
            'amount': 3500,
            'quantity': 10,
          },
          {
            'productCode': '01101222',
            'productName': '오뚜기 3분 카레 100g',
            'amount': 1500,
            'quantity': 5,
          },
        ],
      };

      final result = ElectronicSalesModel.listFromJson(data);

      expect(result, hasLength(2));
      expect(result[0].yearMonth, '202602');
      expect(result[0].customerName, '사과마을');
      expect(result[0].productCode, '01101123');
      expect(result[0].productName, '갈릭 아이올리소스 240g');
      expect(result[0].amount, 3500);
      expect(result[0].quantity, 10);
      // 레거시 ABC 제품 명세에는 전년 비교 없음
      expect(result[0].previousYearAmount, isNull);
      expect(result[0].growthRate, isNull);
    });

    test('items 가 없거나 비어 있으면 빈 목록을 반환한다', () {
      final empty = {
        'customerName': '사과마을',
        'yearMonth': '202602',
        'items': <dynamic>[],
      };
      expect(ElectronicSalesModel.listFromJson(empty), isEmpty);

      final noItemsKey = {
        'customerName': '사과마을',
        'yearMonth': '202602',
      };
      expect(ElectronicSalesModel.listFromJson(noItemsKey), isEmpty);
    });
  });
}
