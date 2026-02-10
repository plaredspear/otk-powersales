import '../models/order_model.dart';

/// 주문 API 응답 데이터를 담는 모델
///
/// 페이지네이션을 포함한 주문 목록 응답을 파싱합니다.
class OrderListResponseModel {
  final List<OrderModel> content;
  final int totalElements;
  final int totalPages;
  final int number;
  final int size;
  final bool first;
  final bool last;

  const OrderListResponseModel({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.number,
    required this.size,
    required this.first,
    required this.last,
  });

  /// API 응답 JSON에서 파싱
  factory OrderListResponseModel.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;
    final contentJson = data['content'] as List<dynamic>? ?? [];
    final content = contentJson
        .map((e) => OrderModel.fromJson(e as Map<String, dynamic>))
        .toList();

    return OrderListResponseModel(
      content: content,
      totalElements: data['total_elements'] as int,
      totalPages: data['total_pages'] as int,
      number: data['number'] as int,
      size: data['size'] as int,
      first: data['first'] as bool,
      last: data['last'] as bool,
    );
  }
}

/// 주문 API DataSource 인터페이스
///
/// 주문 관련 API 호출을 추상화합니다.
abstract class OrderRemoteDataSource {
  /// GET /api/v1/me/orders
  ///
  /// 내 주문 목록을 조회합니다.
  Future<OrderListResponseModel> getMyOrders({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
    int page = 0,
    int size = 20,
  });
}
