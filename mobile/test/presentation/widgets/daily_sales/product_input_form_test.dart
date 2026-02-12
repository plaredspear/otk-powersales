import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/daily_sales/product_input_form.dart';

void main() {
  group('ProductInputForm', () {
    group('대표제품 (ProductType.main)', () {
      testWidgets('대표제품 입력 필드가 렌더링된다', (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ProductInputForm(
                type: ProductType.main,
                onChanged: ({price, quantity, amount, code, name}) {},
              ),
            ),
          ),
        );

        expect(find.text('대표제품'), findsOneWidget);
        expect(find.text('판매단가 (원)'), findsOneWidget);
        expect(find.text('판매수량 (개)'), findsOneWidget);
        expect(find.text('총 판매금액 (원)'), findsOneWidget);

        // 기타제품 필드는 없어야 함
        expect(find.text('제품 코드'), findsNothing);
        expect(find.text('제품명'), findsNothing);
      });

      testWidgets('초기값이 표시된다', (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ProductInputForm(
                type: ProductType.main,
                initialPrice: 1000,
                initialQuantity: 10,
                initialAmount: 10000,
                onChanged: ({price, quantity, amount, code, name}) {},
              ),
            ),
          ),
        );

        expect(find.text('1000'), findsOneWidget);
        expect(find.text('10'), findsOneWidget);
        expect(find.text('10000'), findsOneWidget);
      });

      testWidgets('단가와 수량을 입력하면 금액이 자동 계산된다', (tester) async {
        int? calculatedPrice;
        int? calculatedQuantity;
        int? calculatedAmount;

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ProductInputForm(
                type: ProductType.main,
                onChanged: ({price, quantity, amount, code, name}) {
                  calculatedPrice = price;
                  calculatedQuantity = quantity;
                  calculatedAmount = amount;
                },
              ),
            ),
          ),
        );

        // 단가 입력
        await tester.enterText(
          find.widgetWithText(TextField, '판매단가 (원)'),
          '1200',
        );
        await tester.pump();

        // 수량 입력
        await tester.enterText(
          find.widgetWithText(TextField, '판매수량 (개)'),
          '5',
        );
        await tester.pump();

        // 금액 자동 계산 확인
        expect(calculatedPrice, 1200);
        expect(calculatedQuantity, 5);
        expect(calculatedAmount, 6000);
        expect(find.text('6000'), findsOneWidget);
      });

      testWidgets('금액 필드는 비활성화되어 있다', (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ProductInputForm(
                type: ProductType.main,
                onChanged: ({price, quantity, amount, code, name}) {},
              ),
            ),
          ),
        );

        final amountField = tester.widget<TextField>(
          find.widgetWithText(TextField, '총 판매금액 (원)'),
        );

        expect(amountField.enabled, false);
      });

      testWidgets('값 변경 시 onChanged가 호출된다', (tester) async {
        int? receivedPrice;
        int? receivedQuantity;

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ProductInputForm(
                type: ProductType.main,
                onChanged: ({
                  int? price,
                  int? quantity,
                  int? amount,
                  String? code,
                  String? name,
                }) {
                  receivedPrice = price;
                  receivedQuantity = quantity;
                },
              ),
            ),
          ),
        );

        await tester.enterText(
          find.widgetWithText(TextField, '판매단가 (원)'),
          '2000',
        );
        await tester.pump();

        expect(receivedPrice, 2000);
      });
    });

    group('기타제품 (ProductType.sub)', () {
      testWidgets('기타제품 입력 필드가 렌더링된다', (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ProductInputForm(
                type: ProductType.sub,
                onChanged: ({price, quantity, amount, code, name}) {},
              ),
            ),
          ),
        );

        expect(find.text('기타제품'), findsOneWidget);
        expect(find.text('제품 코드'), findsOneWidget);
        expect(find.text('제품명'), findsOneWidget);
        expect(find.text('판매수량 (개)'), findsOneWidget);
        expect(find.text('총 판매금액 (원)'), findsOneWidget);

        // 대표제품 필드는 없어야 함
        expect(find.text('판매단가 (원)'), findsNothing);
      });

      testWidgets('초기값이 표시된다', (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ProductInputForm(
                type: ProductType.sub,
                initialCode: 'P001',
                initialName: '라면',
                initialQuantity: 5,
                initialAmount: 5000,
                onChanged: ({price, quantity, amount, code, name}) {},
              ),
            ),
          ),
        );

        expect(find.text('P001'), findsOneWidget);
        expect(find.text('라면'), findsOneWidget);
        expect(find.text('5'), findsOneWidget);
        expect(find.text('5000'), findsOneWidget);
      });

      testWidgets('값 변경 시 onChanged가 호출된다', (tester) async {
        String? receivedCode;
        String? receivedName;
        int? receivedQuantity;
        int? receivedAmount;

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ProductInputForm(
                type: ProductType.sub,
                onChanged: ({
                  int? price,
                  int? quantity,
                  int? amount,
                  String? code,
                  String? name,
                }) {
                  receivedCode = code;
                  receivedName = name;
                  receivedQuantity = quantity;
                  receivedAmount = amount;
                },
              ),
            ),
          ),
        );

        await tester.enterText(
          find.widgetWithText(TextField, '제품 코드'),
          'P002',
        );
        await tester.pump();
        expect(receivedCode, 'P002');

        await tester.enterText(
          find.widgetWithText(TextField, '제품명'),
          '카레',
        );
        await tester.pump();
        expect(receivedName, '카레');

        await tester.enterText(
          find.widgetWithText(TextField, '판매수량 (개)'),
          '10',
        );
        await tester.pump();
        expect(receivedQuantity, 10);

        await tester.enterText(
          find.widgetWithText(TextField, '총 판매금액 (원)'),
          '20000',
        );
        await tester.pump();
        expect(receivedAmount, 20000);
      });

      testWidgets('빈 문자열은 null로 변환된다', (tester) async {
        String? receivedCode = 'initial';
        String? receivedName = 'initial';

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ProductInputForm(
                type: ProductType.sub,
                initialCode: 'P001',
                initialName: '라면',
                onChanged: ({
                  int? price,
                  int? quantity,
                  int? amount,
                  String? code,
                  String? name,
                }) {
                  receivedCode = code;
                  receivedName = name;
                },
              ),
            ),
          ),
        );

        // 코드 삭제
        await tester.enterText(
          find.widgetWithText(TextField, '제품 코드'),
          '',
        );
        await tester.pump();
        expect(receivedCode, isNull);

        // 제품명 삭제
        await tester.enterText(
          find.widgetWithText(TextField, '제품명'),
          '',
        );
        await tester.pump();
        expect(receivedName, isNull);
      });

      testWidgets('금액 필드는 활성화되어 있다', (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ProductInputForm(
                type: ProductType.sub,
                onChanged: ({price, quantity, amount, code, name}) {},
              ),
            ),
          ),
        );

        final amountField = tester.widget<TextField>(
          find.widgetWithText(TextField, '총 판매금액 (원)'),
        );

        expect(amountField.enabled, true);
      });
    });

    group('입력 검증', () {
      testWidgets('숫자 필드는 숫자만 입력된다', (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ProductInputForm(
                type: ProductType.main,
                onChanged: ({price, quantity, amount, code, name}) {},
              ),
            ),
          ),
        );

        // 숫자가 아닌 문자 입력 시도
        await tester.enterText(
          find.widgetWithText(TextField, '판매단가 (원)'),
          'abc123',
        );
        await tester.pump();

        // 숫자만 입력됨
        expect(find.text('123'), findsOneWidget);
        expect(find.text('abc123'), findsNothing);
      });
    });
  });
}
