import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/order_form/delivery_date_picker.dart';

Widget _host({DateTime? selectedDate, required DateTime now}) {
  return MaterialApp(
    home: Scaffold(
      body: DeliveryDatePicker(
        selectedDate: selectedDate,
        onTap: () {},
        now: now,
      ),
    ),
  );
}

void main() {
  group('DeliveryDatePicker 마감 안내', () {
    testWidgets('납기일 미선택 → 마감 규칙만 안내', (tester) async {
      await tester.pumpWidget(_host(now: DateTime(2026, 7, 25, 10, 0)));

      expect(find.text('납기일 하루 전 13:50까지 주문할 수 있습니다.'), findsOneWidget);
      expect(find.byIcon(Icons.info_outline), findsOneWidget);
    });

    testWidgets('마감 전 → 해당 납기일의 마감 시각 안내', (tester) async {
      await tester.pumpWidget(
        _host(
          selectedDate: DateTime(2026, 7, 26),
          now: DateTime(2026, 7, 25, 10, 0),
        ),
      );

      expect(find.text('주문 마감: 7/25(토) 13:50 까지'), findsOneWidget);
      expect(find.byIcon(Icons.info_outline), findsOneWidget);
      expect(find.byIcon(Icons.error_outline), findsNothing);
    });

    testWidgets('마감 후 → 붉은 경고 안내 + 마감 시각 병기', (tester) async {
      await tester.pumpWidget(
        _host(
          selectedDate: DateTime(2026, 7, 26),
          now: DateTime(2026, 7, 25, 14, 0),
        ),
      );

      expect(find.text('이 납기일은 주문할 수 없습니다. (주문 마감: 7/25(토) 13:50)'), findsOneWidget);
      expect(find.byIcon(Icons.error_outline), findsOneWidget);
    });

    testWidgets('납기일이 오늘이면 시각 무관 마감 안내', (tester) async {
      await tester.pumpWidget(
        _host(
          selectedDate: DateTime(2026, 7, 25),
          now: DateTime(2026, 7, 25, 9, 0),
        ),
      );

      expect(find.byIcon(Icons.error_outline), findsOneWidget);
    });
  });
}
