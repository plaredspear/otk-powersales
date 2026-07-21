import '../../domain/entities/product_order_history_group.dart';
import 'product_for_order_model.dart';

/// 거래처 주문이력 그룹 응답 모델.
///
/// 백엔드 `OrderHistoryGroupResponse` 와 1:1 정합. 제품은 제품검색/즐겨찾기와
/// 동일한 `OrderProductDto`(→ [ProductForOrderModel]) 형상으로 내려온다.
class OrderHistoryGroupModel {
  final String orderDate;
  final List<ProductForOrderModel> products;

  const OrderHistoryGroupModel({
    required this.orderDate,
    required this.products,
  });

  factory OrderHistoryGroupModel.fromJson(Map<String, dynamic> json) {
    final rawProducts = (json['products'] as List<dynamic>?) ?? const [];
    return OrderHistoryGroupModel(
      orderDate: json['orderDate'] as String? ?? '',
      products: rawProducts
          .map((e) => ProductForOrderModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  ProductOrderHistoryGroup toEntity() => ProductOrderHistoryGroup(
        orderDate: orderDate,
        products: products.map((m) => m.toEntity()).toList(),
      );
}
