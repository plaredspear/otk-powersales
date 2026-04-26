import '../../domain/entities/target.dart';

/// 목표/진도율 Mock 데이터
///
/// 거래처별 월 목표금액 및 실적금액
/// 알라딘/SAP 연동 데이터 시뮬레이션
/// UI 개발 및 테스트용 샘플 데이터
class TargetMockData {
  static final List<Target> data = [
    // 2026년 1월 - 목표 초과 케이스 (농협)
    Target(
      id: 'T001',
      customerName: '농협',
      customerCode: 'CUST001',
      yearMonth: '202601',
      targetAmount: 100000000,
      actualAmount: 113000000,
      category: '전산매출',
      note: '신제품 출시 효과',
      createdAt: DateTime(2026, 1, 1),
      updatedAt: DateTime(2026, 1, 25),
    ),

    // 2026년 1월 - 목표 초과 케이스 (GS25)
    Target(
      id: 'T002',
      customerName: 'GS25',
      customerCode: 'CUST002',
      yearMonth: '202601',
      targetAmount: 75000000,
      actualAmount: 79000000,
      category: '전산매출',
      createdAt: DateTime(2026, 1, 1),
      updatedAt: DateTime(2026, 1, 25),
    ),

    // 2026년 1월 - 목표 미달 케이스 (CU)
    Target(
      id: 'T003',
      customerName: 'CU',
      customerCode: 'CUST003',
      yearMonth: '202601',
      targetAmount: 90000000,
      actualAmount: 81000000,
      category: '전산매출',
      note: '경쟁사 프로모션 영향',
      createdAt: DateTime(2026, 1, 1),
      updatedAt: DateTime(2026, 1, 25),
    ),

    // 2026년 1월 - 목표 미달 케이스 (세븐일레븐)
    Target(
      id: 'T004',
      customerName: '세븐일레븐',
      customerCode: 'CUST004',
      yearMonth: '202601',
      targetAmount: 60000000,
      actualAmount: 50000000,
      category: '전산매출',
      createdAt: DateTime(2026, 1, 1),
      updatedAt: DateTime(2026, 1, 25),
    ),

    // 2026년 1월 - 목표 정확히 달성 케이스 (이마트24)
    Target(
      id: 'T005',
      customerName: '이마트24',
      customerCode: 'CUST005',
      yearMonth: '202601',
      targetAmount: 30000000,
      actualAmount: 30000000,
      category: '전산매출',
      createdAt: DateTime(2026, 1, 1),
      updatedAt: DateTime(2026, 1, 25),
    ),

    // 2026년 1월 - 물류매출 목표 (농협)
    Target(
      id: 'T006',
      customerName: '농협',
      customerCode: 'CUST001',
      yearMonth: '202601',
      targetAmount: 180000000,
      actualAmount: 180000000,
      category: '물류매출',
      createdAt: DateTime(2026, 1, 1),
      updatedAt: DateTime(2026, 1, 25),
    ),

    // 2026년 1월 - 신규 거래처 (실적 없음)
    Target(
      id: 'T007',
      customerName: '미니스톱',
      customerCode: 'CUST006',
      yearMonth: '202601',
      targetAmount: 20000000,
      actualAmount: 0,
      category: '전산매출',
      note: '신규 거래처 - 2월부터 납품 예정',
      createdAt: DateTime(2026, 1, 1),
      updatedAt: DateTime(2026, 1, 25),
    ),

    // 2025년 12월 - 목표 초과 케이스 (농협)
    Target(
      id: 'T008',
      customerName: '농협',
      customerCode: 'CUST001',
      yearMonth: '202512',
      targetAmount: 95000000,
      actualAmount: 108000000,
      category: '전산매출',
      note: '연말 특수',
      createdAt: DateTime(2025, 12, 1),
      updatedAt: DateTime(2025, 12, 31),
    ),

    // 2025년 12월 - 목표 미달 케이스 (GS25)
    Target(
      id: 'T009',
      customerName: 'GS25',
      customerCode: 'CUST002',
      yearMonth: '202512',
      targetAmount: 80000000,
      actualAmount: 73000000,
      category: '전산매출',
      createdAt: DateTime(2025, 12, 1),
      updatedAt: DateTime(2025, 12, 31),
    ),

    // 2025년 11월 - 목표 초과 케이스 (농협)
    Target(
      id: 'T010',
      customerName: '농협',
      customerCode: 'CUST001',
      yearMonth: '202511',
      targetAmount: 92000000,
      actualAmount: 101000000,
      category: '전산매출',
      createdAt: DateTime(2025, 11, 1),
      updatedAt: DateTime(2025, 11, 30),
    ),

    // 2026년 2월 - 미래 데이터 (진행 중)
    Target(
      id: 'T011',
      customerName: '농협',
      customerCode: 'CUST001',
      yearMonth: '202602',
      targetAmount: 105000000,
      actualAmount: 52000000,
      category: '전산매출',
      note: '월 중순 기준 50% 달성',
      createdAt: DateTime(2026, 2, 1),
      updatedAt: DateTime(2026, 2, 15),
    ),

    // 2026년 2월 - 미래 데이터 (진행 중)
    Target(
      id: 'T012',
      customerName: 'GS25',
      customerCode: 'CUST002',
      yearMonth: '202602',
      targetAmount: 78000000,
      actualAmount: 30000000,
      category: '전산매출',
      createdAt: DateTime(2026, 2, 1),
      updatedAt: DateTime(2026, 2, 15),
    ),

    // 2026년 1월 - 대형 거래처 (이마트)
    Target(
      id: 'T013',
      customerName: '이마트',
      customerCode: 'CUST007',
      yearMonth: '202601',
      targetAmount: 250000000,
      actualAmount: 268000000,
      category: 'POS매출',
      note: '대형마트 프로모션 성공',
      createdAt: DateTime(2026, 1, 1),
      updatedAt: DateTime(2026, 1, 25),
    ),

    // 2026년 1월 - 대형 거래처 (홈플러스)
    Target(
      id: 'T014',
      customerName: '홈플러스',
      customerCode: 'CUST008',
      yearMonth: '202601',
      targetAmount: 220000000,
      actualAmount: 210000000,
      category: 'POS매출',
      createdAt: DateTime(2026, 1, 1),
      updatedAt: DateTime(2026, 1, 25),
    ),

    // 2026년 1월 - 대형 거래처 (롯데마트)
    Target(
      id: 'T015',
      customerName: '롯데마트',
      customerCode: 'CUST009',
      yearMonth: '202601',
      targetAmount: 200000000,
      actualAmount: 195000000,
      category: 'POS매출',
      note: '일부 점포 재고 과다',
      createdAt: DateTime(2026, 1, 1),
      updatedAt: DateTime(2026, 1, 25),
    ),
  ];

  /// 년월로 필터링
  static List<Target> getByYearMonth(String yearMonth) {
    return data.where((target) => target.yearMonth == yearMonth).toList();
  }

  /// 거래처로 필터링
  static List<Target> getByCustomer(String customerName) {
    return data.where((target) => target.customerName == customerName).toList();
  }

  /// 거래처 코드로 필터링
  static List<Target> getByCustomerCode(String customerCode) {
    return data.where((target) => target.customerCode == customerCode).toList();
  }

  /// 년월 + 거래처로 필터링
  static Target? getByYearMonthAndCustomer(
    String yearMonth,
    String customerName,
  ) {
    try {
      return data.firstWhere((target) =>
          target.yearMonth == yearMonth && target.customerName == customerName);
    } catch (e) {
      return null;
    }
  }

  /// 카테고리로 필터링
  static List<Target> getByCategory(String category) {
    return data
        .where((target) =>
            target.category != null && target.category == category)
        .toList();
  }

  /// 목표 미달 항목만 필터링 (진도율 < 100%)
  static List<Target> getInsufficientTargets(String yearMonth) {
    return data
        .where((target) =>
            target.yearMonth == yearMonth &&
            target.actualAmount < target.targetAmount)
        .toList();
  }

  /// 목표 초과 항목만 필터링 (진도율 > 100%)
  static List<Target> getExceededTargets(String yearMonth) {
    return data
        .where((target) =>
            target.yearMonth == yearMonth &&
            target.actualAmount > target.targetAmount)
        .toList();
  }

  /// 목표 정확히 달성 항목 필터링 (진도율 = 100%)
  static List<Target> getAchievedTargets(String yearMonth) {
    return data
        .where((target) =>
            target.yearMonth == yearMonth &&
            target.actualAmount == target.targetAmount)
        .toList();
  }

  /// 특정 거래처의 월별 트렌드 데이터
  static List<Target> getCustomerTrend(
    String customerName,
    String startYearMonth,
    String endYearMonth,
  ) {
    return data
        .where((target) =>
            target.customerName == customerName &&
            target.yearMonth.compareTo(startYearMonth) >= 0 &&
            target.yearMonth.compareTo(endYearMonth) <= 0)
        .toList();
  }

  /// ID로 조회
  static Target? getById(String id) {
    try {
      return data.firstWhere((target) => target.id == id);
    } catch (e) {
      return null;
    }
  }
}
