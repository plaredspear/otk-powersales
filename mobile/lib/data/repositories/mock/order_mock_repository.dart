import 'dart:math';

import '../../../domain/entities/client_order.dart';
import '../../../domain/entities/order.dart';
import '../../../domain/entities/order_cancel.dart';
import '../../../domain/entities/order_detail.dart';
import '../../../domain/entities/order_draft.dart';
import '../../../domain/entities/product_for_order.dart';
import '../../../domain/entities/validation_error.dart';
import '../../../domain/repositories/order_repository.dart';

/// 주문 Mock Repository
///
/// Backend API 개발 전까지 사용하는 Mock 데이터 기반 Repository입니다.
class OrderMockRepository implements OrderRepository {
  /// 네트워크 지연 시뮬레이션 (300ms)
  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 300));
  }

  /// Mock 주문 데이터
  static final List<Order> _mockOrders = [
    Order(
      id: 1,
      orderRequestNumber: 'OP00000074',
      clientId: 1,
      clientName: '천사푸드',
      orderDate: DateTime(2026, 2, 5),
      deliveryDate: DateTime(2026, 2, 8),
      totalAmount: 612000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 2,
      orderRequestNumber: 'OP00000073',
      clientId: 2,
      clientName: '(유)경산식품',
      orderDate: DateTime(2026, 2, 4),
      deliveryDate: DateTime(2026, 2, 7),
      totalAmount: 245000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 3,
      orderRequestNumber: 'OP00000072',
      clientId: 3,
      clientName: '대한식품유통',
      orderDate: DateTime(2026, 2, 4),
      deliveryDate: DateTime(2026, 2, 7),
      totalAmount: 180500000,
      approvalStatus: ApprovalStatus.pending,
      isClosed: false,
    ),
    Order(
      id: 4,
      orderRequestNumber: 'OP00000071',
      clientId: 1,
      clientName: '천사푸드',
      orderDate: DateTime(2026, 2, 3),
      deliveryDate: DateTime(2026, 2, 6),
      totalAmount: 95000000,
      approvalStatus: ApprovalStatus.sendFailed,
      isClosed: false,
    ),
    Order(
      id: 5,
      orderRequestNumber: 'OP00000070',
      clientId: 4,
      clientName: '행복마트',
      orderDate: DateTime(2026, 2, 3),
      deliveryDate: DateTime(2026, 2, 6),
      totalAmount: 320000000,
      approvalStatus: ApprovalStatus.resend,
      isClosed: false,
    ),
    Order(
      id: 6,
      orderRequestNumber: 'OP00000069',
      clientId: 5,
      clientName: '명품식자재',
      orderDate: DateTime(2026, 2, 2),
      deliveryDate: DateTime(2026, 2, 5),
      totalAmount: 150000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 7,
      orderRequestNumber: 'OP00000068',
      clientId: 2,
      clientName: '(유)경산식품',
      orderDate: DateTime(2026, 2, 1),
      deliveryDate: DateTime(2026, 2, 4),
      totalAmount: 89000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 8,
      orderRequestNumber: 'OP00000067',
      clientId: 6,
      clientName: '서울종합식품',
      orderDate: DateTime(2026, 1, 31),
      deliveryDate: DateTime(2026, 2, 3),
      totalAmount: 450000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 9,
      orderRequestNumber: 'OP00000066',
      clientId: 3,
      clientName: '대한식품유통',
      orderDate: DateTime(2026, 1, 30),
      deliveryDate: DateTime(2026, 2, 2),
      totalAmount: 275000000,
      approvalStatus: ApprovalStatus.pending,
      isClosed: false,
    ),
    Order(
      id: 10,
      orderRequestNumber: 'OP00000065',
      clientId: 4,
      clientName: '행복마트',
      orderDate: DateTime(2026, 1, 29),
      deliveryDate: DateTime(2026, 2, 1),
      totalAmount: 128000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 11,
      orderRequestNumber: 'OP00000064',
      clientId: 7,
      clientName: '그린유통',
      orderDate: DateTime(2026, 1, 28),
      deliveryDate: DateTime(2026, 1, 31),
      totalAmount: 67000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 12,
      orderRequestNumber: 'OP00000063',
      clientId: 1,
      clientName: '천사푸드',
      orderDate: DateTime(2026, 1, 27),
      deliveryDate: DateTime(2026, 1, 30),
      totalAmount: 530000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 13,
      orderRequestNumber: 'OP00000062',
      clientId: 5,
      clientName: '명품식자재',
      orderDate: DateTime(2026, 1, 26),
      deliveryDate: DateTime(2026, 1, 29),
      totalAmount: 198000000,
      approvalStatus: ApprovalStatus.sendFailed,
      isClosed: false,
    ),
    Order(
      id: 14,
      orderRequestNumber: 'OP00000061',
      clientId: 8,
      clientName: '삼성식품',
      orderDate: DateTime(2026, 1, 25),
      deliveryDate: DateTime(2026, 1, 28),
      totalAmount: 412000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 15,
      orderRequestNumber: 'OP00000060',
      clientId: 6,
      clientName: '서울종합식품',
      orderDate: DateTime(2026, 1, 24),
      deliveryDate: DateTime(2026, 1, 27),
      totalAmount: 88000000,
      approvalStatus: ApprovalStatus.resend,
      isClosed: false,
    ),
    Order(
      id: 16,
      orderRequestNumber: 'OP00000059',
      clientId: 2,
      clientName: '(유)경산식품',
      orderDate: DateTime(2026, 1, 23),
      deliveryDate: DateTime(2026, 1, 26),
      totalAmount: 345000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 17,
      orderRequestNumber: 'OP00000058',
      clientId: 7,
      clientName: '그린유통',
      orderDate: DateTime(2026, 1, 22),
      deliveryDate: DateTime(2026, 1, 25),
      totalAmount: 156000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 18,
      orderRequestNumber: 'OP00000057',
      clientId: 3,
      clientName: '대한식품유통',
      orderDate: DateTime(2026, 1, 21),
      deliveryDate: DateTime(2026, 1, 24),
      totalAmount: 720000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 19,
      orderRequestNumber: 'OP00000056',
      clientId: 8,
      clientName: '삼성식품',
      orderDate: DateTime(2026, 1, 20),
      deliveryDate: DateTime(2026, 1, 23),
      totalAmount: 43000000,
      approvalStatus: ApprovalStatus.pending,
      isClosed: false,
    ),
    Order(
      id: 20,
      orderRequestNumber: 'OP00000055',
      clientId: 4,
      clientName: '행복마트',
      orderDate: DateTime(2026, 1, 19),
      deliveryDate: DateTime(2026, 1, 22),
      totalAmount: 267000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 21,
      orderRequestNumber: 'OP00000054',
      clientId: 1,
      clientName: '천사푸드',
      orderDate: DateTime(2026, 1, 18),
      deliveryDate: DateTime(2026, 1, 21),
      totalAmount: 183000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 22,
      orderRequestNumber: 'OP00000053',
      clientId: 5,
      clientName: '명품식자재',
      orderDate: DateTime(2026, 1, 17),
      deliveryDate: DateTime(2026, 1, 20),
      totalAmount: 510000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 23,
      orderRequestNumber: 'OP00000052',
      clientId: 6,
      clientName: '서울종합식품',
      orderDate: DateTime(2026, 1, 16),
      deliveryDate: DateTime(2026, 1, 19),
      totalAmount: 92000000,
      approvalStatus: ApprovalStatus.sendFailed,
      isClosed: false,
    ),
    Order(
      id: 24,
      orderRequestNumber: 'OP00000051',
      clientId: 7,
      clientName: '그린유통',
      orderDate: DateTime(2026, 1, 15),
      deliveryDate: DateTime(2026, 1, 18),
      totalAmount: 378000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
    Order(
      id: 25,
      orderRequestNumber: 'OP00000050',
      clientId: 8,
      clientName: '삼성식품',
      orderDate: DateTime(2026, 1, 14),
      deliveryDate: DateTime(2026, 1, 17),
      totalAmount: 145000000,
      approvalStatus: ApprovalStatus.approved,
      isClosed: true,
    ),
  ];

  /// Mock 거래처 목록 (필터 드롭다운에서 사용)
  static final Map<int, String> _mockClients = {
    1: '천사푸드',
    2: '(유)경산식품',
    3: '대한식품유통',
    4: '행복마트',
    5: '명품식자재',
    6: '서울종합식품',
    7: '그린유통',
    8: '삼성식품',
  };

  /// 거래처 목록 조회 (필터 드롭다운용)
  Map<int, String> get mockClients => Map.unmodifiable(_mockClients);

  @override
  Future<OrderListResult> getMyOrders({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
    int page = 0,
    int size = 20,
  }) async {
    await _simulateDelay();

    // 필터링
    var filtered = List<Order>.from(_mockOrders);

    // 거래처 필터
    if (clientId != null) {
      filtered = filtered.where((o) => o.clientId == clientId).toList();
    }

    // 상태 필터
    if (status != null && status.isNotEmpty) {
      filtered = filtered
          .where((o) => o.approvalStatus.code == status)
          .toList();
    }

    // 납기일 범위 필터
    if (deliveryDateFrom != null) {
      final from = DateTime.parse(deliveryDateFrom);
      filtered = filtered.where((o) {
        final deliveryOnly = DateTime(
            o.deliveryDate.year, o.deliveryDate.month, o.deliveryDate.day);
        return !deliveryOnly.isBefore(from);
      }).toList();
    }
    if (deliveryDateTo != null) {
      final to = DateTime.parse(deliveryDateTo);
      filtered = filtered.where((o) {
        final deliveryOnly = DateTime(
            o.deliveryDate.year, o.deliveryDate.month, o.deliveryDate.day);
        return !deliveryOnly.isAfter(to);
      }).toList();
    }

    // 정렬
    filtered.sort((a, b) {
      int compare;
      switch (sortBy) {
        case 'orderDate':
          compare = a.orderDate.compareTo(b.orderDate);
          break;
        case 'deliveryDate':
          compare = a.deliveryDate.compareTo(b.deliveryDate);
          break;
        case 'totalAmount':
          compare = a.totalAmount.compareTo(b.totalAmount);
          break;
        default:
          compare = a.orderDate.compareTo(b.orderDate);
      }
      return sortDir == 'ASC' ? compare : -compare;
    });

    // 페이지네이션
    final totalElements = filtered.length;
    final totalPages = (totalElements / size).ceil();
    final startIndex = page * size;
    final endIndex = min(startIndex + size, totalElements);

    final pagedOrders = startIndex < totalElements
        ? filtered.sublist(startIndex, endIndex)
        : <Order>[];

    return OrderListResult(
      orders: List.unmodifiable(pagedOrders),
      totalElements: totalElements,
      totalPages: totalPages == 0 ? 1 : totalPages,
      currentPage: page,
      pageSize: size,
      isFirst: page == 0,
      isLast: page >= totalPages - 1 || totalPages == 0,
    );
  }

  /// Mock 주문 상세 데이터 (거래처 마감시간)
  static final Map<int, String> _mockDeadlineTimes = {
    1: '13:40',
    2: '14:00',
    3: '15:30',
    4: '13:00',
    5: '14:30',
    6: '16:00',
    7: '13:30',
    8: '15:00',
  };

  /// Mock 주문한 제품 데이터
  static List<OrderedItem> _getMockOrderedItems(int orderId) {
    final random = Random(orderId); // orderId 기반 시드로 일관된 데이터 생성
    final itemCount = random.nextInt(8) + 2; // 2~9개

    final products = [
      ('01101123', '갈릭 아이올리소스 240g'),
      ('01101222', '오뚜기 3분 카레 100g'),
      ('13310002', '미향500ML'),
      ('23010011', '오감포차_크림새우180G'),
      ('11110003', '토마토케찹500G'),
      ('01202001', '진라면 매운맛 120g'),
      ('01302010', '참깨라면 120g'),
      ('02101003', '오뚜기밥 210g'),
      ('03201001', '3분짜장 200g'),
      ('04101002', '마요네즈 500g'),
    ];

    return List.generate(itemCount, (index) {
      final product = products[index % products.length];
      final boxes = (random.nextDouble() * 50 + 1).roundToDouble();
      final pieces = (boxes * (random.nextInt(20) + 5)).toInt();
      // orderId 4 (전송실패)의 경우 첫 번째 제품만 취소
      final isCancelled =
          orderId == 4 && index == 0 || orderId == 9 && index < 2;

      return OrderedItem(
        productCode: product.$1,
        productName: product.$2,
        totalQuantityBoxes: boxes,
        totalQuantityPieces: pieces,
        isCancelled: isCancelled,
      );
    });
  }

  /// Mock 주문 처리 현황 (마감후 주문에만 적용)
  static OrderProcessingStatus? _getMockProcessingStatus(Order order) {
    if (!order.isClosed) return null;

    final items = _getMockOrderedItems(order.id);
    final statuses = [
      DeliveryStatus.waiting,
      DeliveryStatus.shipping,
      DeliveryStatus.delivered,
    ];
    final random = Random(order.id);

    return OrderProcessingStatus(
      sapOrderNumber: '030001${3650 + order.id}',
      items: items.map((item) {
        final statusIndex = random.nextInt(3);
        final deliveredQty =
            statusIndex == 2 ? '${item.totalQuantityPieces} EA' : '0 EA';
        return ProcessingItem(
          productCode: item.productCode,
          productName: item.productName,
          deliveredQuantity: deliveredQty,
          deliveryStatus: statuses[statusIndex],
        );
      }).toList(),
    );
  }

  /// Mock 반려 제품 (특정 주문에만 적용)
  static List<RejectedItem>? _getMockRejectedItems(Order order) {
    // id 1, 7 주문만 반려 제품 있음 (마감 완료된 주문 중 일부)
    if (!order.isClosed || (order.id != 1 && order.id != 7)) return null;

    final rejectedProducts = [
      ('23010011', '오감포차_크림새우180G', 1, '납품일자가 업무일이 아닙니다.'),
      ('11110003', '토마토케찹500G', 2, '납품일자가 업무일이 아닙니다.'),
      ('01202001', '진라면 매운맛 120g', 3, '재고 부족'),
      ('04101002', '마요네즈 500g', 5, '단가 불일치'),
      ('03201001', '3분짜장 200g', 1, '최소 주문 수량 미달'),
    ];

    final count = order.id == 1 ? 5 : 2;
    return rejectedProducts
        .take(count)
        .map((p) => RejectedItem(
              productCode: p.$1,
              productName: p.$2,
              orderQuantityBoxes: p.$3,
              rejectionReason: p.$4,
            ))
        .toList();
  }

  @override
  Future<OrderDetail> getOrderDetail({required int orderId}) async {
    await _simulateDelay();

    final order = _mockOrders.firstWhere(
      (o) => o.id == orderId,
      orElse: () => throw Exception('ORDER_NOT_FOUND'),
    );

    final orderedItems = _getMockOrderedItems(orderId);
    final processingStatus = _getMockProcessingStatus(order);
    final rejectedItems = _getMockRejectedItems(order);

    return OrderDetail(
      id: order.id,
      orderRequestNumber: order.orderRequestNumber,
      clientId: order.clientId,
      clientName: order.clientName,
      clientDeadlineTime: _mockDeadlineTimes[order.clientId],
      orderDate: order.orderDate,
      deliveryDate: order.deliveryDate,
      totalAmount: order.totalAmount,
      totalApprovedAmount: order.isClosed ? (order.totalAmount * 0.85).toInt() : null,
      approvalStatus: order.approvalStatus,
      isClosed: order.isClosed,
      orderedItemCount: orderedItems.length,
      orderedItems: orderedItems,
      orderProcessingStatus: processingStatus,
      rejectedItems: rejectedItems,
    );
  }

  @override
  Future<void> resendOrder({required int orderId}) async {
    await _simulateDelay();

    final order = _mockOrders.firstWhere(
      (o) => o.id == orderId,
      orElse: () => throw Exception('ORDER_NOT_FOUND'),
    );

    if (order.approvalStatus != ApprovalStatus.sendFailed) {
      throw Exception('INVALID_STATUS');
    }

    // Mock: 재전송 성공 시뮬레이션
    // 실제 구현에서는 API 호출 후 상태가 변경됨
  }

  @override
  Future<OrderCancelResult> cancelOrder({
    required int orderId,
    required List<String> productCodes,
  }) async {
    await _simulateDelay();

    // 주문 존재 확인
    final order = _mockOrders.firstWhere(
      (o) => o.id == orderId,
      orElse: () => throw Exception('ORDER_NOT_FOUND'),
    );

    // 마감된 주문은 취소 불가
    if (order.isClosed) {
      throw Exception('ORDER_ALREADY_CLOSED');
    }

    // 제품코드 유효성 확인
    final orderedItems = _getMockOrderedItems(orderId);
    final validCodes = orderedItems
        .where((item) => !item.isCancelled)
        .map((item) => item.productCode)
        .toSet();

    for (final code in productCodes) {
      if (!validCodes.contains(code)) {
        throw Exception('ALREADY_CANCELLED');
      }
    }

    // Mock: 취소 성공 시뮬레이션
    return OrderCancelResult(
      cancelledCount: productCodes.length,
      cancelledProductCodes: productCodes,
    );
  }

  // ─── 주문서 작성 관련 Mock 구현 (F22) ─────────────────────────────

  /// Mock 임시저장 데이터
  OrderDraft? _savedDraft;

  /// Mock 즐겨찾기 제품 코드 목록
  final Set<String> _favoriteProductCodes = {
    '01101123',
    '01101222',
    '13310002',
    '23010011',
    '01202001',
  };

  /// Mock 제품 데이터 (주문용)
  static final List<ProductForOrder> _mockProducts = [
    const ProductForOrder(
      productCode: '01101123',
      productName: '갈릭 아이올리소스 240g',
      barcode: '8801045123456',
      storageType: '상온',
      shelfLife: '12개월',
      unitPrice: 3500,
      boxSize: 20,
      isFavorite: true,
      categoryMid: '소스류',
      categorySub: '마요/드레싱',
    ),
    const ProductForOrder(
      productCode: '01101222',
      productName: '오뚜기 3분 카레 100g',
      barcode: '8801045234567',
      storageType: '상온',
      shelfLife: '18개월',
      unitPrice: 1500,
      boxSize: 24,
      isFavorite: true,
      categoryMid: '즉석식품',
      categorySub: '3분요리',
    ),
    const ProductForOrder(
      productCode: '13310002',
      productName: '미향500ML',
      barcode: '8801045345678',
      storageType: '상온',
      shelfLife: '24개월',
      unitPrice: 2800,
      boxSize: 12,
      isFavorite: true,
      categoryMid: '조미료',
      categorySub: '참기름/식용유',
    ),
    const ProductForOrder(
      productCode: '23010011',
      productName: '오감포차_크림새우180G',
      barcode: '8801045456789',
      storageType: '냉동',
      shelfLife: '12개월',
      unitPrice: 5900,
      boxSize: 16,
      isFavorite: true,
      categoryMid: '냉동식품',
      categorySub: '냉동안주',
    ),
    const ProductForOrder(
      productCode: '11110003',
      productName: '토마토케찹500G',
      barcode: '8801045567890',
      storageType: '상온',
      shelfLife: '18개월',
      unitPrice: 3200,
      boxSize: 20,
      isFavorite: false,
      categoryMid: '소스류',
      categorySub: '케찹/소스',
    ),
    const ProductForOrder(
      productCode: '01202001',
      productName: '진라면 매운맛 120g',
      barcode: '8801045678901',
      storageType: '상온',
      shelfLife: '6개월',
      unitPrice: 850,
      boxSize: 40,
      isFavorite: true,
      categoryMid: '라면',
      categorySub: '봉지라면',
    ),
    const ProductForOrder(
      productCode: '01302010',
      productName: '참깨라면 120g',
      barcode: '8801045789012',
      storageType: '상온',
      shelfLife: '6개월',
      unitPrice: 900,
      boxSize: 40,
      isFavorite: false,
      categoryMid: '라면',
      categorySub: '봉지라면',
    ),
    const ProductForOrder(
      productCode: '02101003',
      productName: '오뚜기밥 210g',
      barcode: '8801045890123',
      storageType: '상온',
      shelfLife: '12개월',
      unitPrice: 1800,
      boxSize: 12,
      isFavorite: false,
      categoryMid: '즉석식품',
      categorySub: '즉석밥',
    ),
    const ProductForOrder(
      productCode: '03201001',
      productName: '3분짜장 200g',
      barcode: '8801045901234',
      storageType: '상온',
      shelfLife: '18개월',
      unitPrice: 1600,
      boxSize: 24,
      isFavorite: false,
      categoryMid: '즉석식품',
      categorySub: '3분요리',
    ),
    const ProductForOrder(
      productCode: '04101002',
      productName: '마요네즈 500g',
      barcode: '8801045012345',
      storageType: '냉장',
      shelfLife: '7개월',
      unitPrice: 4200,
      boxSize: 10,
      isFavorite: false,
      categoryMid: '소스류',
      categorySub: '마요/드레싱',
    ),
  ];

  /// Mock 여신 잔액 데이터
  static final Map<int, int> _mockCreditBalances = {
    1: 100000000,
    2: 50000000,
    3: 75000000,
    4: 30000000,
    5: 120000000,
    6: 85000000,
    7: 45000000,
    8: 60000000,
  };

  @override
  Future<int> getCreditBalance({required int clientId}) async {
    await _simulateDelay();
    final balance = _mockCreditBalances[clientId];
    if (balance == null) {
      throw Exception('STORE_NOT_FOUND');
    }
    return balance;
  }

  @override
  Future<List<ProductForOrder>> getFavoriteProducts() async {
    await _simulateDelay();
    return _mockProducts
        .where((p) => _favoriteProductCodes.contains(p.productCode))
        .map((p) => p.copyWith(isFavorite: true))
        .toList();
  }

  @override
  Future<List<ProductForOrder>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) async {
    await _simulateDelay();
    var results = _mockProducts.where((p) {
      final q = query.toLowerCase();
      return p.productName.toLowerCase().contains(q) ||
          p.productCode.toLowerCase().contains(q);
    }).toList();

    if (categoryMid != null && categoryMid.isNotEmpty) {
      results = results.where((p) => p.categoryMid == categoryMid).toList();
    }
    if (categorySub != null && categorySub.isNotEmpty) {
      results = results.where((p) => p.categorySub == categorySub).toList();
    }

    // 즐겨찾기 상태 반영
    return results
        .map((p) => p.copyWith(
            isFavorite: _favoriteProductCodes.contains(p.productCode)))
        .toList();
  }

  @override
  Future<ProductForOrder> getProductByBarcode({required String barcode}) async {
    await _simulateDelay();
    final product = _mockProducts.firstWhere(
      (p) => p.barcode == barcode,
      orElse: () => throw Exception('PRODUCT_NOT_FOUND'),
    );
    return product.copyWith(
        isFavorite: _favoriteProductCodes.contains(product.productCode));
  }

  @override
  Future<void> saveDraftOrder({required OrderDraft orderDraft}) async {
    await _simulateDelay();
    _savedDraft = orderDraft;
  }

  @override
  Future<OrderDraft?> loadDraftOrder() async {
    await _simulateDelay();
    return _savedDraft;
  }

  @override
  Future<void> deleteDraftOrder() async {
    await _simulateDelay();
    _savedDraft = null;
  }

  @override
  Future<ValidationResult> validateOrder({
    required OrderDraft orderDraft,
  }) async {
    await _simulateDelay();

    // Mock: 모든 제품의 유효성을 통과시킴
    // 특정 제품코드 '23010011'은 최소수량 미달로 실패하도록 시뮬레이션
    final errors = <String, ValidationError>{};

    for (final item in orderDraft.items) {
      if (item.productCode == '23010011' && item.quantityBoxes < 5) {
        errors[item.productCode] = const ValidationError(
          errorType: ValidationErrorType.minOrderQuantity,
          message: '최소 주문 수량은 5박스입니다',
          minOrderQuantity: 5,
          supplyQuantity: 100,
          dcQuantity: 10,
        );
      }
    }

    return ValidationResult(
      isValid: errors.isEmpty,
      errors: errors,
    );
  }

  @override
  Future<OrderSubmitResult> submitOrder({
    required OrderDraft orderDraft,
  }) async {
    await _simulateDelay();

    // Mock: 주문서 전송 성공
    final newId = _mockOrders.length + 1;
    return OrderSubmitResult(
      orderId: newId,
      orderRequestNumber: 'OP${(50 + newId).toString().padLeft(8, '0')}',
      status: 'PENDING',
    );
  }

  @override
  Future<OrderSubmitResult> updateOrder({
    required int orderId,
    required OrderDraft orderDraft,
  }) async {
    await _simulateDelay();

    // 주문 존재 확인
    _mockOrders.firstWhere(
      (o) => o.id == orderId,
      orElse: () => throw Exception('ORDER_NOT_FOUND'),
    );

    return OrderSubmitResult(
      orderId: orderId,
      orderRequestNumber: 'OP${(50 + orderId).toString().padLeft(8, '0')}',
      status: 'PENDING',
    );
  }

  @override
  Future<void> addToFavorites({required String productCode}) async {
    await _simulateDelay();
    _favoriteProductCodes.add(productCode);
  }

  @override
  Future<void> removeFromFavorites({required String productCode}) async {
    await _simulateDelay();
    _favoriteProductCodes.remove(productCode);
  }

  // ─── 거래처별 주문 Mock 구현 (F28) ─────────────────────────────

  /// Mock 거래처별 주문 데이터
  static final List<ClientOrder> _mockClientOrders = [
    const ClientOrder(
      sapOrderNumber: '300011396',
      clientId: 2,
      clientName: '(유)경산식품',
      totalAmount: 3763740,
    ),
    const ClientOrder(
      sapOrderNumber: '300011397',
      clientId: 2,
      clientName: '(유)경산식품',
      totalAmount: 182820,
    ),
    const ClientOrder(
      sapOrderNumber: '300011398',
      clientId: 2,
      clientName: '(유)경산식품',
      totalAmount: 199800,
    ),
    const ClientOrder(
      sapOrderNumber: '300011399',
      clientId: 1,
      clientName: '천사푸드',
      totalAmount: 5210000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011400',
      clientId: 1,
      clientName: '천사푸드',
      totalAmount: 1250000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011401',
      clientId: 3,
      clientName: '대한식품유통',
      totalAmount: 8900000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011402',
      clientId: 4,
      clientName: '행복마트',
      totalAmount: 450000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011403',
      clientId: 5,
      clientName: '명품식자재',
      totalAmount: 2340000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011404',
      clientId: 6,
      clientName: '서울종합식품',
      totalAmount: 6700000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011405',
      clientId: 7,
      clientName: '그린유통',
      totalAmount: 890000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011406',
      clientId: 8,
      clientName: '삼성식품',
      totalAmount: 3210000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011407',
      clientId: 2,
      clientName: '(유)경산식품',
      totalAmount: 1540000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011408',
      clientId: 1,
      clientName: '천사푸드',
      totalAmount: 780000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011409',
      clientId: 3,
      clientName: '대한식품유통',
      totalAmount: 4560000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011410',
      clientId: 4,
      clientName: '행복마트',
      totalAmount: 2100000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011411',
      clientId: 5,
      clientName: '명품식자재',
      totalAmount: 990000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011412',
      clientId: 6,
      clientName: '서울종합식품',
      totalAmount: 1870000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011413',
      clientId: 7,
      clientName: '그린유통',
      totalAmount: 5600000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011414',
      clientId: 8,
      clientName: '삼성식품',
      totalAmount: 430000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011415',
      clientId: 1,
      clientName: '천사푸드',
      totalAmount: 7800000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011416',
      clientId: 2,
      clientName: '(유)경산식품',
      totalAmount: 2450000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011417',
      clientId: 3,
      clientName: '대한식품유통',
      totalAmount: 1320000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011418',
      clientId: 4,
      clientName: '행복마트',
      totalAmount: 6100000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011419',
      clientId: 5,
      clientName: '명품식자재',
      totalAmount: 3780000,
    ),
    const ClientOrder(
      sapOrderNumber: '300011420',
      clientId: 6,
      clientName: '서울종합식품',
      totalAmount: 920000,
    ),
  ];

  @override
  Future<ClientOrderListResult> getClientOrders({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  }) async {
    await _simulateDelay();

    // 거래처 ID로 필터링
    final filtered = _mockClientOrders
        .where((o) => o.clientId == clientId)
        .toList();

    // 페이지네이션
    final totalElements = filtered.length;
    final totalPages = (totalElements / size).ceil();
    final startIndex = page * size;
    final endIndex = min(startIndex + size, totalElements);

    final pagedOrders = startIndex < totalElements
        ? filtered.sublist(startIndex, endIndex)
        : <ClientOrder>[];

    return ClientOrderListResult(
      orders: List.unmodifiable(pagedOrders),
      totalElements: totalElements,
      totalPages: totalPages == 0 ? 1 : totalPages,
      currentPage: page,
      pageSize: size,
      isFirst: page == 0,
      isLast: page >= totalPages - 1 || totalPages == 0,
    );
  }

  @override
  Future<ClientOrderDetail> getClientOrderDetail({
    required String sapOrderNumber,
  }) async {
    await _simulateDelay();

    final order = _mockClientOrders.firstWhere(
      (o) => o.sapOrderNumber == sapOrderNumber,
      orElse: () => throw Exception('ORDER_NOT_FOUND'),
    );

    final random = Random(int.parse(sapOrderNumber.substring(
        sapOrderNumber.length - 4)));
    final itemCount = random.nextInt(12) + 5; // 5~16개

    final products = [
      ('26310001', '프레스코_포도씨유0.5L'),
      ('26110007', '옥수수유0.9L'),
      ('20010007', '옛날_구수한끓여먹는누룽지240G'),
      ('19710009', '통단팥죽(상온)285G'),
      ('19610002', '맛있는미역국18G'),
      ('19610001', '맛있는북엇국34G'),
      ('01101123', '갈릭 아이올리소스 240g'),
      ('01101222', '오뚜기 3분 카레 100g'),
      ('13310002', '미향500ML'),
      ('23010011', '오감포차_크림새우180G'),
      ('11110003', '토마토케찹500G'),
      ('01202001', '진라면 매운맛 120g'),
      ('01302010', '참깨라면 120g'),
      ('02101003', '오뚜기밥 210g'),
      ('03201001', '3분짜장 200g'),
      ('04101002', '마요네즈 500g'),
    ];

    final statuses = [
      DeliveryStatus.waiting,
      DeliveryStatus.shipping,
      DeliveryStatus.delivered,
    ];

    final orderedItems = List.generate(itemCount, (index) {
      final product = products[index % products.length];
      final statusIndex = random.nextInt(3);
      final deliveredQty = statusIndex == 2
          ? '${random.nextInt(50) + 1} BOX'
          : '0 BOX';

      return ClientOrderItem(
        productCode: product.$1,
        productName: product.$2,
        deliveredQuantity: deliveredQty,
        deliveryStatus: statuses[statusIndex],
      );
    });

    return ClientOrderDetail(
      sapOrderNumber: order.sapOrderNumber,
      clientId: order.clientId,
      clientName: order.clientName,
      clientDeadlineTime: _mockDeadlineTimes[order.clientId],
      orderDate: DateTime(2026, 2, 14),
      deliveryDate: DateTime(2026, 2, 19),
      totalApprovedAmount: order.totalAmount,
      orderedItemCount: orderedItems.length,
      orderedItems: orderedItems,
    );
  }
}
