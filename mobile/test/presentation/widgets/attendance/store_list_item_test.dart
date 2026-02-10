import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/store_schedule_item.dart';
import 'package:mobile/presentation/widgets/attendance/store_list_item.dart';

void main() {
  group('StoreListItem 위젯 테스트', () {
    const unregisteredStore = StoreScheduleItem(
      storeId: 1,
      storeName: '이마트 월계점',
      storeCode: 'S001',
      workCategory: '대형마트',
      address: '서울시 노원구 월계동',
      isRegistered: false,
    );

    const registeredStore = StoreScheduleItem(
      storeId: 2,
      storeName: '홈플러스 창동점',
      storeCode: 'S002',
      workCategory: '대형마트',
      address: '서울시 도봉구 창동',
      isRegistered: true,
      registeredWorkType: 'ROOM_TEMP',
    );

    testWidgets('미등록 거래처의 거래처명을 렌더링한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreListItem(
              store: unregisteredStore,
              isSelected: false,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.text('이마트 월계점'), findsOneWidget);
    });

    testWidgets('미등록 거래처의 주소를 렌더링한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreListItem(
              store: unregisteredStore,
              isSelected: false,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.text('서울시 노원구 월계동'), findsOneWidget);
    });

    testWidgets('업무 카테고리 배지를 렌더링한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreListItem(
              store: unregisteredStore,
              isSelected: false,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.text('대형마트'), findsOneWidget);
    });

    testWidgets('미등록 거래처는 라디오 버튼을 표시한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreListItem(
              store: unregisteredStore,
              isSelected: false,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.byIcon(Icons.radio_button_unchecked), findsOneWidget);
    });

    testWidgets('미등록 거래처를 탭하면 onTap이 호출된다', (tester) async {
      bool tapped = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreListItem(
              store: unregisteredStore,
              isSelected: false,
              onTap: () {
                tapped = true;
              },
            ),
          ),
        ),
      );

      await tester.tap(find.byType(StoreListItem));
      await tester.pump();

      expect(tapped, true);
    });

    testWidgets('등록된 거래처는 체크 아이콘을 표시한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreListItem(
              store: registeredStore,
              isSelected: false,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.byIcon(Icons.check_circle), findsOneWidget);
    });

    testWidgets('등록된 거래처는 근무유형 배지를 표시한다 (상온)', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreListItem(
              store: registeredStore,
              isSelected: false,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.text('상온'), findsOneWidget);
    });

    testWidgets('등록된 거래처는 근무유형 배지를 표시한다 (냉장/냉동)', (tester) async {
      const refrigeratedStore = StoreScheduleItem(
        storeId: 3,
        storeName: '롯데마트 노원점',
        storeCode: 'S003',
        workCategory: '대형마트',
        address: '서울시 노원구',
        isRegistered: true,
        registeredWorkType: 'REFRIGERATED',
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreListItem(
              store: refrigeratedStore,
              isSelected: false,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.text('냉장/냉동'), findsOneWidget);
    });

    testWidgets('선택된 거래처는 체크 표시된 라디오 버튼을 표시한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreListItem(
              store: unregisteredStore,
              isSelected: true,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.byIcon(Icons.radio_button_checked), findsOneWidget);
    });

    testWidgets('미선택 거래처는 빈 라디오 버튼을 표시한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreListItem(
              store: unregisteredStore,
              isSelected: false,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.byIcon(Icons.radio_button_unchecked), findsOneWidget);
    });
  });
}
