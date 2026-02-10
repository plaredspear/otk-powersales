import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/my_stores/store_count_header.dart';

void main() {
  group('StoreCountHeader', () {
    Widget buildWidget({required int count}) {
      return MaterialApp(
        home: Scaffold(
          body: StoreCountHeader(count: count),
        ),
      );
    }

    testWidgets('건수가 올바르게 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget(count: 100));

      expect(find.text('거래처 (100)'), findsOneWidget);
    });

    testWidgets('건수가 0일 때 올바르게 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget(count: 0));

      expect(find.text('거래처 (0)'), findsOneWidget);
    });

    testWidgets('건수가 변경되면 업데이트된다', (tester) async {
      await tester.pumpWidget(buildWidget(count: 50));
      expect(find.text('거래처 (50)'), findsOneWidget);

      await tester.pumpWidget(buildWidget(count: 3));
      expect(find.text('거래처 (3)'), findsOneWidget);
    });
  });
}
