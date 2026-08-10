import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/notice/notice_content_html.dart';

/// 렌더된 본문의 전체 텍스트.
///
/// HtmlWidget 은 텍스트를 [Text] 가 아니라 [RichText] 로 렌더하므로
/// find.text 로는 잡히지 않는다.
String _renderedText(WidgetTester tester) {
  return tester
      .widgetList<RichText>(find.byType(RichText))
      .map((w) => w.text.toPlainText())
      .join('\n');
}

/// 본문 HTML 을 렌더링하고 전체 높이를 잰다.
Future<double> _renderHeight(WidgetTester tester, String html) async {
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: SingleChildScrollView(
          child: NoticeContentHtml(html: html),
        ),
      ),
    ),
  );
  await tester.pump();
  return tester.getSize(find.byType(NoticeContentHtml)).height;
}

void main() {
  group('NoticeContentHtml 빈 줄 보존', () {
    // 웹 에디터(Quill)가 빈 줄을 내보내는 형태.
    const blank = '<p><br></p>';

    testWidgets('빈 줄을 늘린 만큼 본문 높이가 누적된다', (tester) async {
      // 빈 줄 1개 → 3개로 늘리면, 늘린 2개만큼 높이가 커져야 한다.
      // (직전 회귀: HeightPlaceholder 병합으로 개수와 무관하게 높이가 동일했음)
      final one = await _renderHeight(
        tester,
        '<p>테스트 1</p>$blank<p>테스트 2</p>',
      );
      final three = await _renderHeight(
        tester,
        '<p>테스트 1</p>$blank$blank$blank<p>테스트 2</p>',
      );

      expect(
        three,
        greaterThan(one),
        reason: '빈 줄을 3개 넣었는데 1개일 때와 높이가 같으면 줄바꿈이 합쳐진 것',
      );
      expect(three - one, closeTo(kNoticeBlankLineHeight * 2, 0.5));
    });

    testWidgets('연속 빈 줄이 개수에 비례해 커진다', (tester) async {
      final two = await _renderHeight(tester, '<p>A</p>$blank$blank<p>B</p>');
      final four = await _renderHeight(
        tester,
        '<p>A</p>$blank$blank$blank$blank<p>B</p>',
      );

      expect(four - two, closeTo(kNoticeBlankLineHeight * 2, 0.5));
    });

    testWidgets('빈 줄이 없으면 SizedBox 삽입도 없다', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: NoticeContentHtml(html: '<p>테스트 1</p><p>테스트 2</p>'),
          ),
        ),
      );
      await tester.pump();

      final text = _renderedText(tester);
      expect(text, contains('테스트 1'));
      expect(text, contains('테스트 2'));
    });

    testWidgets('내용이 있는 문단은 빈 줄로 오인하지 않는다', (tester) async {
      // <br> 를 포함하되 텍스트가 있는 문단 — 빈 줄 처리 대상이 아니어야 한다.
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: NoticeContentHtml(html: '<p>앞줄<br>뒷줄</p>'),
          ),
        ),
      );
      await tester.pump();

      final text = _renderedText(tester);
      expect(text, contains('앞줄'));
      expect(text, contains('뒷줄'));
    });

    testWidgets('빈 줄이 없으면 문단을 늘려도 빈 줄만큼 커지지 않는다', (tester) async {
      // 빈 줄 판정이 과하게 넓어 일반 문단까지 SizedBox 로 대체되면
      // 텍스트가 사라져 높이가 되레 줄어든다 — 그 회귀를 잡는다.
      final withText = await _renderHeight(tester, '<p>A</p><p>B</p>');
      final withBlank = await _renderHeight(tester, '<p>A</p>$blank<p>B</p>');

      expect(withBlank, greaterThan(withText));
      expect(_renderedText(tester), contains('B'));
    });
  });

  group('NoticeContentHtml 인라인 이미지', () {
    testWidgets('http 가 아닌 placeholder 잔존 시 깨진 이미지 박스를 표시한다',
        (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: NoticeContentHtml(
              html: '<p><img src="notice-image://abc"></p>',
            ),
          ),
        ),
      );
      await tester.pump();

      expect(find.byType(NoticeBrokenImageBox), findsOneWidget);
    });
  });
}
