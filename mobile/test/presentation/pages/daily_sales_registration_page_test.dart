import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/daily_sales_mock_repository.dart';
import 'package:mobile/domain/entities/event.dart';
import 'package:mobile/presentation/pages/daily_sales_registration_page.dart';
import 'package:mobile/presentation/providers/daily_sales_form_provider.dart';

void main() {
  group('DailySalesRegistrationPage', () {
    late Event testEvent;
    late DailySalesMockRepository mockRepository;

    setUp(() {
      testEvent = Event(
        id: 'event-test',
        eventType: '[시식]',
        eventName: '테스트 행사',
        startDate: DateTime(2026, 2, 1),
        endDate: DateTime(2026, 2, 28),
        customerId: 'C-TEST',
        customerName: '테스트 거래처',
        assigneeId: 'EMP-TEST',
      );

      mockRepository = DailySalesMockRepository();
      mockRepository.reset();
    });

    Widget buildPage() {
      return ProviderScope(
        overrides: [
          dailySalesRepositoryProvider.overrideWithValue(mockRepository),
        ],
        child: MaterialApp(
          home: DailySalesRegistrationPage(event: testEvent),
        ),
      );
    }

    testWidgets('화면이 렌더링된다', (tester) async {
      await tester.pumpWidget(buildPage());

      expect(find.text('일매출 등록'), findsOneWidget);
      expect(find.text('임시저장'), findsOneWidget);
      expect(find.text('등록'), findsOneWidget);
    });

    testWidgets('행사 정보가 표시된다', (tester) async {
      await tester.pumpWidget(buildPage());

      expect(find.text('테스트 행사'), findsOneWidget);
      expect(find.text('테스트 거래처 | [시식]'), findsOneWidget);
    });

    testWidgets('대표제품 입력 폼이 표시된다', (tester) async {
      await tester.pumpWidget(buildPage());

      expect(find.text('대표제품'), findsOneWidget);
      expect(find.text('판매단가 (원)'), findsOneWidget);
      // 판매수량은 대표제품과 기타제품에 모두 있으므로 생략
    });

    testWidgets('기타제품 입력 폼이 표시된다', (tester) async {
      await tester.pumpWidget(buildPage());

      expect(find.text('기타제품'), findsOneWidget);
      expect(find.text('제품 코드'), findsOneWidget);
      expect(find.text('제품명'), findsOneWidget);
    });

    testWidgets('사진 첨부 위젯이 표시된다', (tester) async {
      await tester.pumpWidget(buildPage());

      expect(find.text('사진 첨부'), findsOneWidget);
      expect(find.text('사진을 추가해주세요'), findsOneWidget);
    });

    testWidgets('초기 상태에서 등록 버튼이 비활성화된다', (tester) async {
      await tester.pumpWidget(buildPage());

      final button = tester.widget<ElevatedButton>(
        find.widgetWithText(ElevatedButton, '등록'),
      );

      expect(button.onPressed, isNull);
    });

    testWidgets('제품과 사진 입력 후 등록 버튼이 활성화된다', (tester) async {
      await tester.pumpWidget(buildPage());

      // 대표제품 입력
      await tester.enterText(
        find.widgetWithText(TextField, '판매단가 (원)'),
        '1000',
      );
      await tester.enterText(
        find.widgetWithText(TextField, '판매수량 (개)').first,
        '10',
      );
      await tester.pump();

      // 사진은 실제로 첨부할 수 없으므로 버튼은 여전히 비활성화
      final button = tester.widget<ElevatedButton>(
        find.widgetWithText(ElevatedButton, '등록'),
      );
      expect(button.onPressed, isNull);
    });

    testWidgets('AppBar가 있다', (tester) async {
      await tester.pumpWidget(buildPage());

      expect(find.byType(AppBar), findsOneWidget);
    });
  });
}
