import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/electronic_sales_model.dart';

void main() {
  group('ElectronicSalesModel.resultFromJson', () {
    test('data.items 를 제품별 ElectronicSales 목록으로 펼치고 합계금액·기간을 담는다', () {
      final data = {
        'customerId': 1,
        'customerName': '사과마을',
        'sapAccountCode': '12345',
        'startDate': '2026-06-01',
        'endDate': '2026-06-09',
        'totalAmount': 5000,
        'items': [
          {
            'productCode': '01101123',
            'productName': '갈릭 아이올리소스 240g',
            'barcode': '8801234500011',
            'amount': 3500,
            'quantity': 10,
          },
          {
            'productCode': '01101222',
            'productName': '오뚜기 3분 카레 100g',
            'barcode': '8801234500028',
            'amount': 1500,
            'quantity': 5,
          },
        ],
      };

      final result = ElectronicSalesModel.resultFromJson(data);

      expect(result.customerName, '사과마을');
      expect(result.startDate, '2026-06-01');
      expect(result.endDate, '2026-06-09');
      expect(result.totalAmount, 5000);
      expect(result.totalQuantity, 15);
      expect(result.items, hasLength(2));
      expect(result.items[0].productCode, '01101123');
      expect(result.items[0].productName, '갈릭 아이올리소스 240g');
      expect(result.items[0].barcode, '8801234500011');
      expect(result.items[0].amount, 3500);
      expect(result.items[0].quantity, 10);
    });

    test('items 가 없거나 비어 있으면 빈 목록 + 합계금액만 반환한다 (레거시 abcSumAmount)', () {
      final sumOnly = {
        'customerName': '사과마을',
        'startDate': '2026-06-01',
        'endDate': '2026-06-09',
        'totalAmount': 123456,
        'items': <dynamic>[],
      };
      final result = ElectronicSalesModel.resultFromJson(sumOnly);
      expect(result.items, isEmpty);
      expect(result.totalAmount, 123456);

      final noItemsKey = {
        'customerName': '사과마을',
        'startDate': '2026-06-01',
        'endDate': '2026-06-09',
        'totalAmount': 0,
      };
      expect(ElectronicSalesModel.resultFromJson(noItemsKey).items, isEmpty);
    });
  });
}
