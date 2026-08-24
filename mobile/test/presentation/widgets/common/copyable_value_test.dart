import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/common/copyable_value.dart';

/// 클립보드 채널을 가로채 마지막으로 복사된 문자열을 기록한다.
class _ClipboardSpy {
  String? lastCopied;

  void install(WidgetTester tester) {
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
      SystemChannels.platform,
      (call) async {
        if (call.method == 'Clipboard.setData') {
          lastCopied = (call.arguments as Map)['text'] as String?;
        }
        return null;
      },
    );
    addTearDown(() {
      tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        SystemChannels.platform,
        null,
      );
    });
  }
}

Widget _host(Widget child) => MaterialApp(
      home: Scaffold(body: Center(child: child)),
    );

void main() {
  group('CopyableValue', () {
    testWidgets('값을 탭하면 클립보드로 복사되고 안내 토스트가 뜬다', (tester) async {
      final spy = _ClipboardSpy()..install(tester);

      await tester.pumpWidget(
        _host(
          const CopyableValue(
            value: '8801045123456',
            copyLabel: '바코드',
            displayText: '바코드: 8801045123456',
          ),
        ),
      );

      await tester.tap(find.text('바코드: 8801045123456'));
      await tester.pump();

      expect(spy.lastCopied, '8801045123456');
      expect(find.textContaining('바코드 복사됨'), findsOneWidget);

      // 토스트는 잠시 후 사라진다.
      await tester.pump(const Duration(seconds: 2));
      expect(find.textContaining('바코드 복사됨'), findsNothing);
    });

    testWidgets('복사 아이콘 탭도 동일하게 복사한다', (tester) async {
      final spy = _ClipboardSpy()..install(tester);

      await tester.pumpWidget(
        _host(const CopyableValue(value: '8801045999999', copyLabel: '바코드')),
      );

      await tester.tap(find.byIcon(Icons.copy_rounded));
      await tester.pump();

      expect(spy.lastCopied, '8801045999999');

      await tester.pump(const Duration(seconds: 2));
    });

    testWidgets('iconOnly 면 값 텍스트 탭으로는 복사되지 않는다', (tester) async {
      final spy = _ClipboardSpy()..install(tester);
      var cardTapped = false;

      await tester.pumpWidget(
        _host(
          GestureDetector(
            onTap: () => cardTapped = true,
            child: const CopyableValue(
              value: '8801045123456',
              copyLabel: '바코드',
              displayText: '바코드: 8801045123456',
              iconOnly: true,
            ),
          ),
        ),
      );

      await tester.tap(find.text('바코드: 8801045123456'));
      await tester.pump();

      expect(spy.lastCopied, isNull);
      expect(cardTapped, isTrue, reason: '카드 탭(선택) 동작이 유지되어야 한다');
    });

    testWidgets('값이 비었거나 "-" 면 복사 아이콘을 노출하지 않는다', (tester) async {
      await tester.pumpWidget(
        _host(const CopyableValue(value: '-', copyLabel: '바코드')),
      );

      expect(find.byIcon(Icons.copy_rounded), findsNothing);
      expect(find.text('-'), findsOneWidget);
    });
  });
}
