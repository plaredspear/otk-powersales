import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/my_store.dart';
import 'package:mobile/presentation/widgets/my_stores/store_detail_popup.dart';

void main() {
  group('StoreDetailPopup', () {
    const store = MyStore(
      storeId: 1,
      storeName: '(유)경산식품',
      storeCode: '1025172',
      address: '전라남도 목포시',
      representativeName: '김정자',
    );

    Widget buildWidget({
      VoidCallback? onOrderStatusTap,
      VoidCallback? onSalesStatusTap,
    }) {
      return MaterialApp(
        home: Scaffold(
          body: StoreDetailPopup(
            store: store,
            onOrderStatusTap: onOrderStatusTap,
            onSalesStatusTap: onSalesStatusTap,
          ),
        ),
      );
    }

    testWidgets('거래처명이 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget());

      expect(find.text('(유)경산식품'), findsOneWidget);
    });

    testWidgets('거래처 코드가 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget());

      expect(find.text('(1025172)'), findsOneWidget);
    });

    testWidgets('주문서 현황 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget());

      expect(find.text('주문서 현황'), findsOneWidget);
    });

    testWidgets('매출 현황 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget());

      expect(find.text('매출 현황'), findsOneWidget);
    });

    testWidgets('닫기 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget());

      expect(find.byIcon(Icons.close), findsOneWidget);
    });

    testWidgets('주문서 현황 탭 시 콜백이 호출된다', (tester) async {
      var tapped = false;
      await tester.pumpWidget(buildWidget(
        onOrderStatusTap: () => tapped = true,
      ));

      await tester.tap(find.text('주문서 현황'));
      await tester.pump();

      expect(tapped, true);
    });

    testWidgets('매출 현황 탭 시 콜백이 호출된다', (tester) async {
      var tapped = false;
      await tester.pumpWidget(buildWidget(
        onSalesStatusTap: () => tapped = true,
      ));

      await tester.tap(find.text('매출 현황'));
      await tester.pump();

      expect(tapped, true);
    });

    testWidgets('show 정적 메서드로 다이얼로그가 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () => StoreDetailPopup.show(
                  context,
                  store: store,
                ),
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();

      expect(find.text('(유)경산식품'), findsOneWidget);
      expect(find.text('주문서 현황'), findsOneWidget);
      expect(find.text('매출 현황'), findsOneWidget);
    });

    testWidgets('닫기 버튼으로 다이얼로그가 닫힌다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () => StoreDetailPopup.show(
                  context,
                  store: store,
                ),
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();

      expect(find.text('(유)경산식품'), findsOneWidget);

      await tester.tap(find.byIcon(Icons.close));
      await tester.pumpAndSettle();

      expect(find.text('(유)경산식품'), findsNothing);
    });
  });
}
