import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_code.dart';
import 'package:mobile/presentation/widgets/claim/claim_purchase_section.dart';

void main() {
  group('ClaimPurchaseSection', () {
    final testMethods = [
      const PurchaseMethod(code: 'PM01', name: '대형마트'),
      const PurchaseMethod(code: 'PM02', name: '편의점'),
    ];

    testWidgets('섹션 구분선과 필드들이 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimPurchaseSection(
              purchaseAmount: null,
              purchaseMethods: testMethods,
              selectedPurchaseMethod: null,
              receiptPhoto: null,
              onPurchaseAmountChanged: (_) {},
              onPurchaseMethodSelected: (_) {},
              onReceiptPhotoSelected: (_) {},
              onReceiptPhotoRemoved: () {},
            ),
          ),
        ),
      );

      // Then: 섹션 제목
      expect(find.text('구매 정보 (선택)'), findsOneWidget);

      // Then: 구매 금액 필드
      expect(find.text('구매 금액'), findsOneWidget);

      // Then: 구매 방법 필드
      expect(find.text('구매 방법'), findsOneWidget);

      // Then: 영수증 사진 필드
      expect(find.text('구매 영수증 사진'), findsOneWidget);
    });

    testWidgets('구매 금액 입력 시 필수 표시가 추가된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimPurchaseSection(
              purchaseAmount: 5000, // 금액 입력됨
              purchaseMethods: testMethods,
              selectedPurchaseMethod: null,
              receiptPhoto: null,
              onPurchaseAmountChanged: (_) {},
              onPurchaseMethodSelected: (_) {},
              onReceiptPhotoSelected: (_) {},
              onReceiptPhotoRemoved: () {},
            ),
          ),
        ),
      );

      // Then: 구매 방법 필수 표시
      expect(find.text('구매 방법 *'), findsOneWidget);

      // Then: 영수증 사진 필수 표시
      expect(find.text('구매 영수증 사진 *'), findsOneWidget);
    });

    testWidgets('선택된 구매 방법이 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimPurchaseSection(
              purchaseAmount: 5000,
              purchaseMethods: testMethods,
              selectedPurchaseMethod: testMethods[0],
              receiptPhoto: null,
              onPurchaseAmountChanged: (_) {},
              onPurchaseMethodSelected: (_) {},
              onReceiptPhotoSelected: (_) {},
              onReceiptPhotoRemoved: () {},
            ),
          ),
        ),
      );

      // Then: 선택된 방법명 표시
      expect(find.text('대형마트'), findsOneWidget);
    });
  });
}
