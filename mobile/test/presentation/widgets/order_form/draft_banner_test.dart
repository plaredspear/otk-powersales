import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/order_form/draft_banner.dart';

/// 좁은 단말에서 배너가 넘치지 않는지 확인한다.
///
/// 안내 문구가 고정폭이라 '불러오기 / 새로 작성' 버튼을 밀어내며 RenderFlex overflow
/// (3.6px)가 났다 — 문구가 줄어드는 쪽이 맞다.
void main() {
  Widget host(double width) {
    return MaterialApp(
      home: Scaffold(
        body: Center(
          child: SizedBox(
            width: width,
            child: DraftBanner(onLoadDraft: () {}, onNewOrder: () {}),
          ),
        ),
      ),
    );
  }

  // 370: 오버플로가 보고된 iPhone 폭(=콘텐츠 338), 320: 구형 소형 단말.
  for (final width in [414.0, 370.0, 320.0]) {
    testWidgets('폭 ${width.toInt()} 에서 넘치지 않는다', (tester) async {
      await tester.pumpWidget(host(width));

      // 오버플로가 나면 RenderFlex 가 FlutterError 를 던져 여기서 잡힌다.
      expect(tester.takeException(), isNull);
      expect(find.text('불러오기'), findsOneWidget);
      expect(find.text('새로 작성'), findsOneWidget);
    });
  }

  testWidgets('두 액션은 항상 온전히 보이고 안내 문구가 줄어든다', (tester) async {
    await tester.pumpWidget(host(320));

    // 배너는 화면 가운데 놓이므로 전역 좌표의 오른쪽 끝과 비교한다.
    final bannerRight = tester.getBottomRight(find.byType(DraftBanner)).dx;
    final loadRight = tester.getBottomRight(find.text('불러오기')).dx;
    final newRight = tester.getBottomRight(find.text('새로 작성')).dx;

    expect(loadRight, lessThanOrEqualTo(bannerRight));
    expect(newRight, lessThanOrEqualTo(bannerRight));
  });
}
