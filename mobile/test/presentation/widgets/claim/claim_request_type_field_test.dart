import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_code.dart';
import 'package:mobile/presentation/widgets/claim/claim_request_type_field.dart';

void main() {
  group('ClaimRequestTypeField', () {
    final testRequestTypes = [
      const ClaimRequestType(code: 'RT01', name: '교환'),
      const ClaimRequestType(code: 'RT02', name: '환불'),
      const ClaimRequestType(code: 'RT03', name: '원인 규명'),
    ];

    testWidgets('선택되지 않은 상태로 렌더링된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimRequestTypeField(
              selectedRequestType: null,
              requestTypes: testRequestTypes,
              onRequestTypeSelected: (_) {},
            ),
          ),
        ),
      );

      // Then: 라벨 표시
      expect(find.text('요청사항'), findsOneWidget);

      // Then: 플레이스홀더 표시
      expect(find.text('요청사항 선택'), findsOneWidget);
    });

    testWidgets('선택된 요청사항이 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimRequestTypeField(
              selectedRequestType: testRequestTypes[0],
              requestTypes: testRequestTypes,
              onRequestTypeSelected: (_) {},
            ),
          ),
        ),
      );

      // Then: 선택된 요청사항명 표시
      expect(find.text('교환'), findsOneWidget);
    });

    testWidgets('ListTile 탭 시 바텀시트가 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimRequestTypeField(
              selectedRequestType: null,
              requestTypes: testRequestTypes,
              onRequestTypeSelected: (_) {},
            ),
          ),
        ),
      );

      // When: ListTile 탭
      await tester.tap(find.byType(ListTile));
      await tester.pumpAndSettle();

      // Then: 바텀시트 표시
      expect(find.text('요청사항 선택'), findsNWidgets(2)); // 헤더에도 있음
      expect(find.text('교환'), findsOneWidget);
      expect(find.text('환불'), findsOneWidget);
      expect(find.text('원인 규명'), findsOneWidget);
    });
  });
}
