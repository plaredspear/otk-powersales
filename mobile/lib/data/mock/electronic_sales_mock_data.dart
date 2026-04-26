import '../../domain/entities/electronic_sales.dart';

/// 전산매출 Mock 데이터
///
/// Orora 영업 고객품목실적일 연동 데이터
/// 물류배부 실적 제외 전산실적
/// UI 개발 및 테스트용 샘플 데이터
class ElectronicSalesMockData {
  static final List<ElectronicSales> data = [
    // 2026년 1월 - 농협 거래처
    ElectronicSales(
      yearMonth: '202601',
      customerName: '농협',
      productName: '진라면 매운맛',
      productCode: 'PROD001',
      amount: 50000000,
      quantity: 62500,
      previousYearAmount: 45000000,
      growthRate: 11.1,
    ),
    ElectronicSales(
      yearMonth: '202601',
      customerName: '농협',
      productName: '진라면 순한맛',
      productCode: 'PROD002',
      amount: 35000000,
      quantity: 43750,
      previousYearAmount: 32000000,
      growthRate: 9.4,
    ),
    ElectronicSales(
      yearMonth: '202601',
      customerName: '농협',
      productName: '오뚜기 카레',
      productCode: 'PROD007',
      amount: 28000000,
      quantity: 8000,
      previousYearAmount: 30000000,
      growthRate: -6.7,
    ),

    // 2026년 1월 - GS25 거래처
    ElectronicSales(
      yearMonth: '202601',
      customerName: 'GS25',
      productName: '진라면 매운맛',
      productCode: 'PROD001',
      amount: 42000000,
      quantity: 52500,
      previousYearAmount: 38000000,
      growthRate: 10.5,
    ),
    ElectronicSales(
      yearMonth: '202601',
      customerName: 'GS25',
      productName: '오뚜기 케찹',
      productCode: 'PROD003',
      amount: 15000000,
      quantity: 5000,
      previousYearAmount: 14000000,
      growthRate: 7.1,
    ),
    ElectronicSales(
      yearMonth: '202601',
      customerName: 'GS25',
      productName: '3분 카레',
      productCode: 'PROD006',
      amount: 22000000,
      quantity: 6286,
      previousYearAmount: 20000000,
      growthRate: 10.0,
    ),

    // 2026년 1월 - CU 거래처
    ElectronicSales(
      yearMonth: '202601',
      customerName: 'CU',
      productName: '진라면 매운맛',
      productCode: 'PROD001',
      amount: 38000000,
      quantity: 47500,
      previousYearAmount: 40000000,
      growthRate: -5.0,
    ),
    ElectronicSales(
      yearMonth: '202601',
      customerName: 'CU',
      productName: '참깨라면',
      productCode: 'PROD005',
      amount: 25000000,
      quantity: 31250,
      previousYearAmount: 22000000,
      growthRate: 13.6,
    ),
    ElectronicSales(
      yearMonth: '202601',
      customerName: 'CU',
      productName: '오뚜기 마요네즈',
      productCode: 'PROD004',
      amount: 18000000,
      quantity: 6000,
      previousYearAmount: 17000000,
      growthRate: 5.9,
    ),

    // 2026년 1월 - 세븐일레븐 거래처
    ElectronicSales(
      yearMonth: '202601',
      customerName: '세븐일레븐',
      productName: '진라면 순한맛',
      productCode: 'PROD002',
      amount: 30000000,
      quantity: 37500,
      previousYearAmount: 28000000,
      growthRate: 7.1,
    ),
    ElectronicSales(
      yearMonth: '202601',
      customerName: '세븐일레븐',
      productName: '오뚜기 냉동 만두',
      productCode: 'PROD008',
      amount: 20000000,
      quantity: 2857,
      previousYearAmount: 18000000,
      growthRate: 11.1,
    ),

    // 2025년 12월 - 농협 거래처 (이전 월 데이터)
    ElectronicSales(
      yearMonth: '202512',
      customerName: '농협',
      productName: '진라면 매운맛',
      productCode: 'PROD001',
      amount: 48000000,
      quantity: 60000,
      previousYearAmount: 44000000,
      growthRate: 9.1,
    ),
    ElectronicSales(
      yearMonth: '202512',
      customerName: '농협',
      productName: '진라면 순한맛',
      productCode: 'PROD002',
      amount: 33000000,
      quantity: 41250,
      previousYearAmount: 31000000,
      growthRate: 6.5,
    ),

    // 2025년 12월 - GS25 거래처
    ElectronicSales(
      yearMonth: '202512',
      customerName: 'GS25',
      productName: '진라면 매운맛',
      productCode: 'PROD001',
      amount: 40000000,
      quantity: 50000,
      previousYearAmount: 37000000,
      growthRate: 8.1,
    ),

    // 2026년 2월 - 농협 거래처 (미래 데이터)
    ElectronicSales(
      yearMonth: '202602',
      customerName: '농협',
      productName: '진라면 매운맛',
      productCode: 'PROD001',
      amount: 52000000,
      quantity: 65000,
      previousYearAmount: 46000000,
      growthRate: 13.0,
    ),
    ElectronicSales(
      yearMonth: '202602',
      customerName: '농협',
      productName: '오뚜기 케찹',
      productCode: 'PROD003',
      amount: 16000000,
      quantity: 5333,
      previousYearAmount: 15000000,
      growthRate: 6.7,
    ),

    // 전년 대비 데이터 없는 케이스 (신규 거래처/제품)
    ElectronicSales(
      yearMonth: '202601',
      customerName: '이마트24',
      productName: '진라면 매운맛',
      productCode: 'PROD001',
      amount: 15000000,
      quantity: 18750,
    ),
    ElectronicSales(
      yearMonth: '202601',
      customerName: '이마트24',
      productName: '참깨라면',
      productCode: 'PROD005',
      amount: 12000000,
      quantity: 15000,
    ),
  ];

  /// 년월로 필터링
  static List<ElectronicSales> getByYearMonth(String yearMonth) {
    return data.where((sales) => sales.yearMonth == yearMonth).toList();
  }

  /// 거래처명으로 필터링
  static List<ElectronicSales> getByCustomerName(String customerName) {
    return data.where((sales) => sales.customerName == customerName).toList();
  }

  /// 제품명으로 필터링
  static List<ElectronicSales> getByProductName(String productName) {
    return data.where((sales) => sales.productName == productName).toList();
  }

  /// 제품 코드로 필터링
  static List<ElectronicSales> getByProductCode(String productCode) {
    return data.where((sales) => sales.productCode == productCode).toList();
  }

  /// 년월 + 거래처로 필터링
  static List<ElectronicSales> getByYearMonthAndCustomer(
    String yearMonth,
    String customerName,
  ) {
    return data
        .where((sales) =>
            sales.yearMonth == yearMonth && sales.customerName == customerName)
        .toList();
  }

  /// 년월 + 거래처 + 제품으로 필터링
  static List<ElectronicSales> getByYearMonthCustomerAndProduct(
    String yearMonth,
    String customerName,
    String productName,
  ) {
    return data
        .where((sales) =>
            sales.yearMonth == yearMonth &&
            sales.customerName == customerName &&
            sales.productName == productName)
        .toList();
  }
}
