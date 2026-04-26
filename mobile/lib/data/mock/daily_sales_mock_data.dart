import '../../domain/entities/daily_sales.dart';

/// 일매출 Mock 데이터
///
/// 행사별 일매출 등록 데이터 (대표제품, 기타제품, 사진, 상태별)
/// UI 개발 및 테스트용 샘플 데이터
class DailySalesMockData {
  static final List<DailySales> data = [
    // REGISTERED 상태 - 대표제품만
    DailySales(
      id: 'ds-001',
      eventId: 'event-001',
      salesDate: DateTime(2026, 2, 10),
      mainProductPrice: 1000,
      mainProductQuantity: 50,
      mainProductAmount: 50000,
      photoUrl: 'https://example.com/photos/ds-001.jpg',
      status: DailySalesStatus.registered,
      registeredAt: DateTime(2026, 2, 10, 18, 30),
    ),

    // REGISTERED 상태 - 기타제품만
    DailySales(
      id: 'ds-002',
      eventId: 'event-001',
      salesDate: DateTime(2026, 2, 11),
      subProductCode: 'P001',
      subProductName: '진라면 순한맛',
      subProductQuantity: 30,
      subProductAmount: 24000,
      photoUrl: 'https://example.com/photos/ds-002.jpg',
      status: DailySalesStatus.registered,
      registeredAt: DateTime(2026, 2, 11, 19, 00),
    ),

    // REGISTERED 상태 - 대표제품 + 기타제품
    DailySales(
      id: 'ds-003',
      eventId: 'event-001',
      salesDate: DateTime(2026, 2, 12),
      mainProductPrice: 1200,
      mainProductQuantity: 40,
      mainProductAmount: 48000,
      subProductCode: 'P002',
      subProductName: '참깨라면',
      subProductQuantity: 20,
      subProductAmount: 18000,
      photoUrl: 'https://example.com/photos/ds-003.jpg',
      status: DailySalesStatus.registered,
      registeredAt: DateTime(2026, 2, 12, 17, 45),
    ),

    // DRAFT 상태 - 대표제품만 (사진 없음)
    DailySales(
      id: 'ds-004',
      eventId: 'event-002',
      salesDate: DateTime(2026, 2, 12),
      mainProductPrice: 800,
      mainProductQuantity: 25,
      mainProductAmount: 20000,
      status: DailySalesStatus.draft,
    ),

    // DRAFT 상태 - 기타제품만
    DailySales(
      id: 'ds-005',
      eventId: 'event-002',
      salesDate: DateTime(2026, 2, 11),
      subProductCode: 'P003',
      subProductName: '오뚜기 카레',
      subProductQuantity: 15,
      subProductAmount: 45000,
      status: DailySalesStatus.draft,
    ),

    // REGISTERED 상태 - event-002
    DailySales(
      id: 'ds-006',
      eventId: 'event-002',
      salesDate: DateTime(2026, 2, 10),
      mainProductPrice: 1500,
      mainProductQuantity: 60,
      mainProductAmount: 90000,
      photoUrl: 'https://example.com/photos/ds-006.jpg',
      status: DailySalesStatus.registered,
      registeredAt: DateTime(2026, 2, 10, 20, 15),
    ),

    // REGISTERED 상태 - event-003 (대표제품 + 기타제품)
    DailySales(
      id: 'ds-007',
      eventId: 'event-003',
      salesDate: DateTime(2026, 2, 9),
      mainProductPrice: 2000,
      mainProductQuantity: 35,
      mainProductAmount: 70000,
      subProductCode: 'P004',
      subProductName: '오뚜기 케찹',
      subProductQuantity: 10,
      subProductAmount: 30000,
      photoUrl: 'https://example.com/photos/ds-007.jpg',
      status: DailySalesStatus.registered,
      registeredAt: DateTime(2026, 2, 9, 16, 00),
    ),

    // DRAFT 상태 - 대표제품만 (일부 필드만 입력)
    DailySales(
      id: 'ds-008',
      eventId: 'event-003',
      salesDate: DateTime(2026, 2, 12),
      mainProductPrice: 1800,
      // mainProductQuantity, mainProductAmount 미입력
      status: DailySalesStatus.draft,
    ),

    // REGISTERED 상태 - 소량 판매
    DailySales(
      id: 'ds-009',
      eventId: 'event-001',
      salesDate: DateTime(2026, 2, 8),
      mainProductPrice: 900,
      mainProductQuantity: 10,
      mainProductAmount: 9000,
      photoUrl: 'https://example.com/photos/ds-009.jpg',
      status: DailySalesStatus.registered,
      registeredAt: DateTime(2026, 2, 8, 21, 30),
    ),

    // REGISTERED 상태 - 대량 판매
    DailySales(
      id: 'ds-010',
      eventId: 'event-001',
      salesDate: DateTime(2026, 2, 7),
      mainProductPrice: 1100,
      mainProductQuantity: 200,
      mainProductAmount: 220000,
      subProductCode: 'P005',
      subProductName: '진라면 매운맛',
      subProductQuantity: 100,
      subProductAmount: 80000,
      photoUrl: 'https://example.com/photos/ds-010.jpg',
      status: DailySalesStatus.registered,
      registeredAt: DateTime(2026, 2, 7, 22, 00),
    ),
  ];

  /// 특정 행사의 일매출 목록 조회
  static List<DailySales> getByEventId(String eventId) {
    return data.where((sales) => sales.eventId == eventId).toList();
  }

  /// 특정 상태의 일매출 목록 조회
  static List<DailySales> getByStatus(DailySalesStatus status) {
    return data.where((sales) => sales.status == status).toList();
  }

  /// 특정 날짜의 일매출 목록 조회
  static List<DailySales> getByDate(DateTime date) {
    return data.where((sales) {
      return sales.salesDate.year == date.year &&
          sales.salesDate.month == date.month &&
          sales.salesDate.day == date.day;
    }).toList();
  }

  /// ID로 일매출 조회
  static DailySales? getById(String id) {
    try {
      return data.firstWhere((sales) => sales.id == id);
    } catch (e) {
      return null;
    }
  }
}
