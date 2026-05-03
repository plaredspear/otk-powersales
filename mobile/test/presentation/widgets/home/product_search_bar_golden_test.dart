import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/home/product_search_bar.dart';

/// ProductSearchBar 골든 테스트
void main() {
  testWidgets('default', (tester) async {
    await tester.pumpWidget(MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        backgroundColor: const Color(0xFFFFFFFF),
        body: Padding(
          padding: const EdgeInsets.all(20),
          child: SizedBox(
            width: 353, // 393 - 20*2 = 353
            child: ProductSearchBar(onTap: () {}),
          ),
        ),
      ),
    ));
    await expectLater(
      find.byType(ProductSearchBar),
      matchesGoldenFile('../../../goldens/home/product_search_bar_default.png'),
    );
  });
}
