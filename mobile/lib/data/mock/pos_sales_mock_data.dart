import '../../domain/entities/pos_sales.dart';

/// POS 매출 Mock 데이터
///
/// 대형마트 3대 (이마트, 홈플러스, 롯데마트)의 다양한 제품 판매 데이터
/// UI 개발 및 테스트용 샘플 데이터
class PosSalesMockData {
  static final List<PosSales> data = [
    // 이마트 - 진라면
    PosSales(
      storeName: '이마트 강남점',
      productName: '진라면 매운맛',
      salesDate: DateTime(2026, 1, 15),
      quantity: 120,
      amount: 96000,
      productCode: 'PROD001',
      category: '라면',
    ),
    PosSales(
      storeName: '이마트 강남점',
      productName: '진라면 순한맛',
      salesDate: DateTime(2026, 1, 16),
      quantity: 80,
      amount: 64000,
      productCode: 'PROD002',
      category: '라면',
    ),
    PosSales(
      storeName: '이마트 잠실점',
      productName: '진라면 매운맛',
      salesDate: DateTime(2026, 1, 17),
      quantity: 150,
      amount: 120000,
      productCode: 'PROD001',
      category: '라면',
    ),

    // 이마트 - 케찹/소스
    PosSales(
      storeName: '이마트 강남점',
      productName: '오뚜기 케찹',
      salesDate: DateTime(2026, 1, 15),
      quantity: 60,
      amount: 180000,
      productCode: 'PROD003',
      category: '소스',
    ),
    PosSales(
      storeName: '이마트 수원점',
      productName: '오뚜기 마요네즈',
      salesDate: DateTime(2026, 1, 18),
      quantity: 45,
      amount: 135000,
      productCode: 'PROD004',
      category: '소스',
    ),

    // 홈플러스 - 라면
    PosSales(
      storeName: '홈플러스 강남점',
      productName: '진라면 매운맛',
      salesDate: DateTime(2026, 1, 19),
      quantity: 100,
      amount: 80000,
      productCode: 'PROD001',
      category: '라면',
    ),
    PosSales(
      storeName: '홈플러스 잠실점',
      productName: '진라면 순한맛',
      salesDate: DateTime(2026, 1, 20),
      quantity: 70,
      amount: 56000,
      productCode: 'PROD002',
      category: '라면',
    ),
    PosSales(
      storeName: '홈플러스 분당점',
      productName: '참깨라면',
      salesDate: DateTime(2026, 1, 21),
      quantity: 90,
      amount: 72000,
      productCode: 'PROD005',
      category: '라면',
    ),

    // 홈플러스 - 카레/소스
    PosSales(
      storeName: '홈플러스 강남점',
      productName: '3분 카레',
      salesDate: DateTime(2026, 1, 22),
      quantity: 50,
      amount: 175000,
      productCode: 'PROD006',
      category: '카레',
    ),
    PosSales(
      storeName: '홈플러스 수원점',
      productName: '오뚜기 카레',
      salesDate: DateTime(2026, 1, 23),
      quantity: 40,
      amount: 140000,
      productCode: 'PROD007',
      category: '카레',
    ),

    // 롯데마트 - 라면
    PosSales(
      storeName: '롯데마트 서울역점',
      productName: '진라면 매운맛',
      salesDate: DateTime(2026, 1, 24),
      quantity: 130,
      amount: 104000,
      productCode: 'PROD001',
      category: '라면',
    ),
    PosSales(
      storeName: '롯데마트 잠실점',
      productName: '진라면 순한맛',
      salesDate: DateTime(2026, 1, 25),
      quantity: 85,
      amount: 68000,
      productCode: 'PROD002',
      category: '라면',
    ),
    PosSales(
      storeName: '롯데마트 강남점',
      productName: '참깨라면',
      salesDate: DateTime(2026, 1, 26),
      quantity: 75,
      amount: 60000,
      productCode: 'PROD005',
      category: '라면',
    ),

    // 롯데마트 - 냉동식품
    PosSales(
      storeName: '롯데마트 서울역점',
      productName: '오뚜기 냉동 만두',
      salesDate: DateTime(2026, 1, 27),
      quantity: 35,
      amount: 245000,
      productCode: 'PROD008',
      category: '냉동',
    ),
    PosSales(
      storeName: '롯데마트 잠실점',
      productName: '오뚜기 냉동 피자',
      salesDate: DateTime(2026, 1, 28),
      quantity: 25,
      amount: 200000,
      productCode: 'PROD009',
      category: '냉동',
    ),

    // 추가 데이터 - 다양한 날짜/매장/제품 조합
    PosSales(
      storeName: '이마트 강남점',
      productName: '진라면 매운맛',
      salesDate: DateTime(2026, 1, 10),
      quantity: 110,
      amount: 88000,
      productCode: 'PROD001',
      category: '라면',
    ),
    PosSales(
      storeName: '이마트 강남점',
      productName: '오뚜기 케찹',
      salesDate: DateTime(2026, 1, 12),
      quantity: 55,
      amount: 165000,
      productCode: 'PROD003',
      category: '소스',
    ),
    PosSales(
      storeName: '홈플러스 강남점',
      productName: '3분 카레',
      salesDate: DateTime(2026, 1, 14),
      quantity: 48,
      amount: 168000,
      productCode: 'PROD006',
      category: '카레',
    ),
    PosSales(
      storeName: '롯데마트 강남점',
      productName: '진라면 매운맛',
      salesDate: DateTime(2026, 1, 11),
      quantity: 125,
      amount: 100000,
      productCode: 'PROD001',
      category: '라면',
    ),

    // 2월 데이터 (날짜 필터링 테스트용)
    PosSales(
      storeName: '이마트 강남점',
      productName: '진라면 매운맛',
      salesDate: DateTime(2026, 2, 1),
      quantity: 95,
      amount: 76000,
      productCode: 'PROD001',
      category: '라면',
    ),
    PosSales(
      storeName: '홈플러스 강남점',
      productName: '진라면 순한맛',
      salesDate: DateTime(2026, 2, 5),
      quantity: 72,
      amount: 57600,
      productCode: 'PROD002',
      category: '라면',
    ),
  ];

  /// 특정 매장의 데이터만 필터링
  static List<PosSales> getByStoreName(String storeName) {
    return data.where((sales) => sales.storeName == storeName).toList();
  }

  /// 특정 제품의 데이터만 필터링
  static List<PosSales> getByProductName(String productName) {
    return data.where((sales) => sales.productName == productName).toList();
  }

  /// 날짜 범위로 필터링
  static List<PosSales> getByDateRange(DateTime startDate, DateTime endDate) {
    return data.where((sales) {
      return sales.salesDate.isAfter(startDate.subtract(const Duration(days: 1))) &&
          sales.salesDate.isBefore(endDate.add(const Duration(days: 1)));
    }).toList();
  }

  /// 카테고리로 필터링
  static List<PosSales> getByCategory(String category) {
    return data.where((sales) => sales.category == category).toList();
  }
}
