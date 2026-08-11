import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/product_for_order_model.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/presentation/widgets/order_form/product_card_for_add.dart';

/// 최근 주문 배지 — 상단 정렬된 제품을 사용자가 식별할 수 있게 하는 표시.
void main() {
  ProductForOrder product({bool recentlyOrdered = false}) {
    return ProductForOrder(
      productCode: 'P001',
      productName: '진라면_매운맛',
      barcode: '8801234567890',
      storageType: '실온',
      shelfLife: '12개월',
      unitPrice: 1000,
      boxSize: 10,
      isFavorite: false,
      recentlyOrdered: recentlyOrdered,
    );
  }

  Widget host(ProductForOrder p) {
    return MaterialApp(
      home: Scaffold(
        body: ProductCardForAdd(
          product: p,
          isSelected: false,
          onSelectionChanged: (_) {},
          onFavoriteToggle: () {},
        ),
      ),
    );
  }

  testWidgets('recentlyOrdered=true 면 "최근 주문" 배지가 표시된다', (tester) async {
    await tester.pumpWidget(host(product(recentlyOrdered: true)));
    expect(find.text('최근 주문'), findsOneWidget);
  });

  testWidgets('recentlyOrdered=false 면 배지가 표시되지 않는다', (tester) async {
    await tester.pumpWidget(host(product()));
    expect(find.text('최근 주문'), findsNothing);
  });

  group('ProductForOrderModel - recentlyOrdered 파싱', () {
    test('서버가 true 를 내려주면 엔티티까지 전달된다', () {
      final model = ProductForOrderModel.fromJson(const {
        'productCode': 'P001',
        'productName': '진라면',
        'barcode': '8801234567890',
        'storageType': '실온',
        'shelfLife': '12개월',
        'unitPrice': 1000,
        'boxSize': 10,
        'isFavorite': false,
        'recentlyOrdered': true,
      });

      expect(model.recentlyOrdered, true);
      expect(model.toEntity().recentlyOrdered, true);
    });

    test('필드가 없으면 false 로 폴백한다(즐겨찾기/주문이력 응답 정합)', () {
      final model = ProductForOrderModel.fromJson(const {
        'productCode': 'P001',
        'productName': '진라면',
        'barcode': '8801234567890',
        'storageType': '실온',
        'shelfLife': '12개월',
        'unitPrice': 1000,
        'boxSize': 10,
        'isFavorite': false,
      });

      expect(model.recentlyOrdered, false);
      expect(model.toEntity().recentlyOrdered, false);
    });
  });
}
