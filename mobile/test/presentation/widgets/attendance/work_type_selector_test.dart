import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/attendance/work_type_selector.dart';

void main() {
  group('WorkTypeSelector 위젯 테스트', () {
    testWidgets('근무유형 라벨을 렌더링한다', (tester) async {
      String selectedType = 'ROOM_TEMP';

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: WorkTypeSelector(
              selectedWorkType: selectedType,
              onChanged: (value) {},
            ),
          ),
        ),
      );

      expect(find.text('근무유형'), findsOneWidget);
    });

    testWidgets('상온과 냉장/냉동 옵션을 렌더링한다', (tester) async {
      String selectedType = 'ROOM_TEMP';

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: WorkTypeSelector(
              selectedWorkType: selectedType,
              onChanged: (value) {},
            ),
          ),
        ),
      );

      expect(find.text('상온'), findsOneWidget);
      expect(find.text('냉장/냉동'), findsOneWidget);
    });

    testWidgets('초기 선택값이 ROOM_TEMP일 때 상온이 선택된 상태로 표시된다', (tester) async {
      String selectedType = 'ROOM_TEMP';

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: WorkTypeSelector(
              selectedWorkType: selectedType,
              onChanged: (value) {},
            ),
          ),
        ),
      );

      // radio_button_checked 아이콘이 2개 있어야 함 (상온 옵션에 선택됨)
      final checkedIcons = find.byIcon(Icons.radio_button_checked);
      expect(checkedIcons, findsOneWidget);

      // radio_button_unchecked 아이콘이 1개 있어야 함 (냉장/냉동 옵션에 미선택)
      final uncheckedIcons = find.byIcon(Icons.radio_button_unchecked);
      expect(uncheckedIcons, findsOneWidget);
    });

    testWidgets('냉장/냉동 옵션을 탭하면 onChanged가 REFRIGERATED와 함께 호출된다',
        (tester) async {
      String selectedType = 'ROOM_TEMP';
      String? changedValue;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: WorkTypeSelector(
              selectedWorkType: selectedType,
              onChanged: (value) {
                changedValue = value;
              },
            ),
          ),
        ),
      );

      await tester.tap(find.text('냉장/냉동'));
      await tester.pump();

      expect(changedValue, 'REFRIGERATED');
    });

    testWidgets('상온 옵션을 탭하면 onChanged가 ROOM_TEMP와 함께 호출된다', (tester) async {
      String selectedType = 'REFRIGERATED';
      String? changedValue;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: WorkTypeSelector(
              selectedWorkType: selectedType,
              onChanged: (value) {
                changedValue = value;
              },
            ),
          ),
        ),
      );

      await tester.tap(find.text('상온'));
      await tester.pump();

      expect(changedValue, 'ROOM_TEMP');
    });
  });
}
