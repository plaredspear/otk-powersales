import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_code.dart';
import 'package:mobile/presentation/widgets/claim/claim_date_field.dart';

void main() {
  group('ClaimDateField', () {
    testWidgets('기본 렌더링이 정상적으로 동작한다', (tester) async {
      // Given
      ClaimDateType selectedDateType = ClaimDateType.expiryDate;
      DateTime selectedDate = DateTime(2026, 2, 20);

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimDateField(
              dateType: selectedDateType,
              date: selectedDate,
              onDateTypeChanged: (type) {
                selectedDateType = type;
              },
              onDateSelected: (date) {
                selectedDate = date;
              },
            ),
          ),
        ),
      );

      // Then: 라벨 표시
      expect(find.text('기한 *'), findsOneWidget);

      // Then: 기한 종류 드롭다운 표시
      expect(find.text('유통기한'), findsOneWidget);

      // Then: 날짜 표시
      expect(find.text('2026-02-20'), findsOneWidget);

      // Then: 캘린더 아이콘 표시
      expect(find.text('📅'), findsOneWidget);
    });

    testWidgets('기한 종류 변경이 동작한다', (tester) async {
      // Given
      ClaimDateType? changedDateType;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimDateField(
              dateType: ClaimDateType.expiryDate,
              date: DateTime(2026, 2, 20),
              onDateTypeChanged: (type) {
                changedDateType = type;
              },
              onDateSelected: (_) {},
            ),
          ),
        ),
      );

      // When: 드롭다운 탭
      await tester.tap(find.byType(DropdownButtonFormField<ClaimDateType>));
      await tester.pumpAndSettle();

      // When: 제조일자 선택
      await tester.tap(find.text('제조일자').last);
      await tester.pumpAndSettle();

      // Then
      expect(changedDateType, ClaimDateType.manufactureDate);
    });

  });
}
