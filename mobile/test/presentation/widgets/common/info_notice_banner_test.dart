import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/common/info_notice_banner.dart';

void main() {
  group('InfoNoticeBanner', () {
    testWidgets('전달받은 message 와 info 아이콘을 렌더링한다', (tester) async {
      const message = '승인처리에 최대 5분이 걸릴 수 있습니다.';
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: InfoNoticeBanner(message: message),
          ),
        ),
      );

      expect(find.text(message), findsOneWidget);
      expect(find.byIcon(Icons.info_outline), findsOneWidget);
    });

    testWidgets('취소 화면 안내 워딩도 그대로 표시한다', (tester) async {
      const message = '품목이 많은 경우, 최대 5분이 걸릴 수 있습니다.';
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: InfoNoticeBanner(message: message),
          ),
        ),
      );

      expect(find.text(message), findsOneWidget);
    });
  });
}
