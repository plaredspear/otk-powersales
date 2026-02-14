import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/my_schedule/schedule_store_item.dart';

void main() {
  group('ScheduleStoreItem', () {
    testWidgets('거래처명과 근무 유형을 렌더링한다', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ScheduleStoreItem(
              storeName: '이마트',
              workType1: '진열',
              workType2: '전담',
              workType3: '순회',
            ),
          ),
        ),
      );

      expect(find.text('이마트'), findsOneWidget);
      expect(find.text('진열 / 전담 / 순회'), findsOneWidget);
    });

    testWidgets('등록 상태를 표시하지 않으면 등록 상태가 보이지 않는다', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ScheduleStoreItem(
              storeName: '이마트',
              workType1: '진열',
              workType2: '전담',
              workType3: '순회',
              isRegistered: false,
              showRegistrationStatus: false,
            ),
          ),
        ),
      );

      expect(find.text('등록 전'), findsNothing);
      expect(find.text('등록 완료'), findsNothing);
    });

    testWidgets('등록 전 상태를 표시한다', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ScheduleStoreItem(
              storeName: '롯데마트',
              workType1: '진열',
              workType2: '전담',
              workType3: '격고',
              isRegistered: false,
              showRegistrationStatus: true,
            ),
          ),
        ),
      );

      expect(find.text('롯데마트'), findsOneWidget);
      expect(find.text('등록 전'), findsOneWidget);
    });

    testWidgets('등록 완료 상태를 표시한다', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: ScheduleStoreItem(
              storeName: '홈플러스',
              workType1: '진열',
              workType2: '전담',
              workType3: '고정',
              isRegistered: true,
              showRegistrationStatus: true,
            ),
          ),
        ),
      );

      expect(find.text('홈플러스'), findsOneWidget);
      expect(find.text('등록 완료'), findsOneWidget);
    });
  });
}
