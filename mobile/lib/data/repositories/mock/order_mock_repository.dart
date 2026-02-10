import 'dart:math';

import '../../../domain/entities/order.dart';
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
}
