import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/my_store.dart';
import 'package:mobile/presentation/widgets/my_stores/my_store_card.dart';

void main() {
  group('MyStoreCard', () {
    const storeWithPhone = MyStore(
      storeId: 1,
      storeName: '(유)경산식품',
      storeCode: '1025172',
      address: '전라남도 목포시 임암로20번길 6',
      representativeName: '김정자',
      phoneNumber: '061-123-4567',
    );

    const storeWithoutPhone = MyStore(
      storeId: 2,
      storeName: '(주)새롬유통',
      storeCode: '1034302',
      address: '광주광역시 북구 첨단과기로 234',
      representativeName: '최새롬',
    );

    Widget buildWidget({
      required MyStore store,
      VoidCallback? onTap,
      VoidCallback? onPhoneTap,
    }) {
      return MaterialApp(
        home: Scaffold(
          body: MyStoreCard(
            store: store,
            onTap: onTap ?? () {},
            onPhoneTap: onPhoneTap,
          ),
        ),
      );
    }

    testWidgets('거래처명(코드)이 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget(store: storeWithPhone));

      expect(find.text('(유)경산식품(1025172)'), findsOneWidget);
    });

    testWidgets('주소가 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget(store: storeWithPhone));

      expect(
          find.text('전라남도 목포시 임암로20번길 6'), findsOneWidget);
    });

    testWidgets('대표자가 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget(store: storeWithPhone));

      expect(find.text('대표자: 김정자'), findsOneWidget);
    });

    testWidgets('전화번호가 있으면 전화 아이콘이 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget(store: storeWithPhone));

      expect(find.byIcon(Icons.phone), findsOneWidget);
    });

    testWidgets('전화번호가 없으면 전화 아이콘이 표시되지 않는다', (tester) async {
      await tester.pumpWidget(buildWidget(store: storeWithoutPhone));

      expect(find.byIcon(Icons.phone), findsNothing);
    });

    testWidgets('카드 탭 시 onTap 콜백이 호출된다', (tester) async {
      var tapped = false;
      await tester.pumpWidget(buildWidget(
        store: storeWithPhone,
        onTap: () => tapped = true,
      ));

      await tester.tap(find.text('(유)경산식품(1025172)'));
      await tester.pump();

      expect(tapped, true);
    });

    testWidgets('전화 버튼 탭 시 onPhoneTap 콜백이 호출된다', (tester) async {
      var phoneTapped = false;
      await tester.pumpWidget(buildWidget(
        store: storeWithPhone,
        onPhoneTap: () => phoneTapped = true,
      ));

      await tester.tap(find.byIcon(Icons.phone));
      await tester.pump();

      expect(phoneTapped, true);
    });
  });
}
