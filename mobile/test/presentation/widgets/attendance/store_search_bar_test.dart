import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/attendance/store_search_bar.dart';

void main() {
  group('StoreSearchBar 위젯 테스트', () {
    late TextEditingController controller;

    setUp(() {
      controller = TextEditingController();
    });

    tearDown(() {
      controller.dispose();
    });

    testWidgets('힌트 텍스트를 렌더링한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreSearchBar(
              controller: controller,
              onChanged: (value) {},
            ),
          ),
        ),
      );

      expect(find.text('거래처명, 주소, 거래처코드 검색'), findsOneWidget);
    });

    testWidgets('검색 아이콘을 렌더링한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreSearchBar(
              controller: controller,
              onChanged: (value) {},
            ),
          ),
        ),
      );

      expect(find.byIcon(Icons.search), findsOneWidget);
    });

    testWidgets('텍스트 입력 시 onChanged 콜백이 호출된다', (tester) async {
      String? changedValue;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: StoreSearchBar(
              controller: controller,
              onChanged: (value) {
                changedValue = value;
              },
            ),
          ),
        ),
      );

      await tester.enterText(find.byType(TextField), '이마트');
      await tester.pump();

      expect(changedValue, '이마트');
      expect(controller.text, '이마트');
    });

    testWidgets('텍스트가 입력되면 클리어 버튼이 나타난다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: _TestWrapper(
              controller: controller,
              onChanged: (value) {},
            ),
          ),
        ),
      );

      // 초기 상태에서는 클리어 버튼이 없음
      expect(find.byIcon(Icons.clear), findsNothing);

      // 텍스트 입력
      await tester.enterText(find.byType(TextField), '홈플러스');
      await tester.pump();

      // 클리어 버튼이 나타남
      expect(find.byIcon(Icons.clear), findsOneWidget);
    });

    testWidgets('클리어 버튼을 탭하면 텍스트가 지워지고 onChanged가 호출된다', (tester) async {
      String? changedValue;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: _TestWrapper(
              controller: controller,
              onChanged: (value) {
                changedValue = value;
              },
            ),
          ),
        ),
      );

      // 텍스트 입력
      await tester.enterText(find.byType(TextField), '롯데마트');
      await tester.pump();

      expect(controller.text, '롯데마트');

      // 클리어 버튼 탭
      await tester.tap(find.byIcon(Icons.clear));
      await tester.pump();

      expect(controller.text, isEmpty);
      expect(changedValue, '');
    });
  });
}

/// StatefulWidget wrapper to make StoreSearchBar rebuild when controller changes
class _TestWrapper extends StatefulWidget {
  final TextEditingController controller;
  final ValueChanged<String> onChanged;

  const _TestWrapper({
    required this.controller,
    required this.onChanged,
  });

  @override
  State<_TestWrapper> createState() => _TestWrapperState();
}

class _TestWrapperState extends State<_TestWrapper> {
  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onControllerChanged);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onControllerChanged);
    super.dispose();
  }

  void _onControllerChanged() {
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return StoreSearchBar(
      controller: widget.controller,
      onChanged: widget.onChanged,
    );
  }
}
