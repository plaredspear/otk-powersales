import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/notice.dart';
import 'package:mobile/presentation/widgets/home/notice_carousel.dart';

void main() {
  Widget buildTestWidget({
    List<Notice> notices = const [],
    void Function(Notice)? onNoticeTap,
    VoidCallback? onViewAllTap,
    double? screenWidth,
  }) {
    final widget = NoticeCarousel(
      notices: notices,
      onNoticeTap: onNoticeTap,
      onViewAllTap: onViewAllTap,
    );

    if (screenWidth != null) {
      return MaterialApp(
        home: Scaffold(
          body: SizedBox(
            width: screenWidth,
            child: widget,
          ),
        ),
      );
    }

    return MaterialApp(
      home: Scaffold(body: widget),
    );
  }

  group('NoticeCarousel', () {
    group('카드 고정 크기 (레거시 정렬)', () {
      testWidgets('카드 폭이 150dp, 높이가 146dp 고정이어야 한다',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(notices: _sampleNotices));

        final containers = tester
            .widgetList<Container>(find.byType(Container))
            .where((c) =>
                c.constraints != null &&
                (c.constraints!.maxWidth - 150).abs() < 0.01 &&
                (c.constraints!.maxHeight - 146).abs() < 0.01)
            .toList();
        expect(containers.length, greaterThanOrEqualTo(2),
            reason: '공지 카드 2개 + 전체보기 카드까지 150×146 고정');
      });
    });

    group('전체보기 카드', () {
      testWidgets('공지 1건 이상일 때 전체보기 카드가 표시되어야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(notices: _sampleNotices));

        expect(find.text('전체보기'), findsOneWidget);
        expect(find.byIcon(Icons.arrow_forward_ios), findsOneWidget);
      });

      testWidgets('전체보기 카드 탭 시 onViewAllTap 콜백이 호출되어야 한다',
          (tester) async {
        var tapped = false;
        await tester.pumpWidget(buildTestWidget(
          notices: _sampleNotices,
          onViewAllTap: () => tapped = true,
        ));

        await tester.ensureVisible(find.text('전체보기'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('전체보기'));
        expect(tapped, isTrue);
      });

      testWidgets('onViewAllTap이 null이면 탭해도 에러가 발생하지 않아야 한다',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          notices: _sampleNotices,
          onViewAllTap: null,
        ));

        await tester.ensureVisible(find.text('전체보기'));
        await tester.pumpAndSettle();
        await tester.tap(find.text('전체보기'), warnIfMissed: false);
        // no error
      });
    });

    group('공지 0건', () {
      testWidgets('공지가 없으면 "공지사항이 없습니다." 메시지만 표시하고 전체보기 카드는 미표시',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(notices: const []));

        expect(find.text('공지사항이 없습니다.'), findsOneWidget);
        expect(find.text('전체보기'), findsNothing);
      });
    });

    group('공지 카드', () {
      testWidgets('공지 카드가 올바르게 표시되어야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(notices: _sampleNotices));

        expect(find.text('테스트 공지 1'), findsOneWidget);
        expect(find.text('테스트 공지 2'), findsOneWidget);
      });

      testWidgets('본문이 3줄로 ellipsis 처리되어야 한다', (tester) async {
        await tester.pumpWidget(buildTestWidget(notices: _sampleNotices));

        final titleText =
            tester.widget<Text>(find.text('테스트 공지 1'));
        expect(titleText.maxLines, 3);
        expect(titleText.overflow, TextOverflow.ellipsis);
      });

      testWidgets('공지 카드 탭 시 onNoticeTap 콜백이 호출되어야 한다',
          (tester) async {
        Notice? tappedNotice;
        await tester.pumpWidget(buildTestWidget(
          notices: _sampleNotices,
          onNoticeTap: (notice) => tappedNotice = notice,
        ));

        await tester.tap(find.text('테스트 공지 1'));
        expect(tappedNotice?.id, 1);
      });
    });

    group('NEW 배지 (createdAt 7일 이내)', () {
      final fixedNow = DateTime(2026, 5, 1, 12, 0);

      Widget buildWithNow(List<Notice> notices) {
        return MaterialApp(
          home: Scaffold(
            body: NoticeCarousel(notices: notices, now: fixedNow),
          ),
        );
      }

      testWidgets('createdAt이 3일 전이면 NEW 배지가 표시된다', (tester) async {
        final notice = Notice(
          id: 10,
          title: '신규 공지',
          category: 'COMPANY',
          categoryName: '회사공지',
          createdAt: fixedNow.subtract(const Duration(days: 3)),
        );
        await tester.pumpWidget(buildWithNow([notice]));

        final newBadge = find.byWidgetPredicate((widget) =>
            widget is Image &&
            widget.image is AssetImage &&
            (widget.image as AssetImage).assetName ==
                'assets/images/ico_new.png');
        expect(newBadge, findsOneWidget);
      });

      testWidgets('createdAt이 8일 전이면 NEW 배지가 표시되지 않는다',
          (tester) async {
        final notice = Notice(
          id: 11,
          title: '오래된 공지',
          category: 'COMPANY',
          categoryName: '회사공지',
          createdAt: fixedNow.subtract(const Duration(days: 8)),
        );
        await tester.pumpWidget(buildWithNow([notice]));

        final newBadge = find.byWidgetPredicate((widget) =>
            widget is Image &&
            widget.image is AssetImage &&
            (widget.image as AssetImage).assetName ==
                'assets/images/ico_new.png');
        expect(newBadge, findsNothing);
      });

      testWidgets('createdAt이 정확히 7일 전이면 NEW 배지가 표시되지 않는다 (경계 케이스)',
          (tester) async {
        final notice = Notice(
          id: 12,
          title: '경계 공지',
          category: 'COMPANY',
          categoryName: '회사공지',
          createdAt: fixedNow.subtract(const Duration(days: 7)),
        );
        await tester.pumpWidget(buildWithNow([notice]));

        final newBadge = find.byWidgetPredicate((widget) =>
            widget is Image &&
            widget.image is AssetImage &&
            (widget.image as AssetImage).assetName ==
                'assets/images/ico_new.png');
        expect(newBadge, findsNothing,
            reason: '경계 7일 정각은 미만(<)이 아니므로 미표시');
      });
    });
  });
}

final _sampleNotices = [
  Notice(
    id: 1,
    title: '테스트 공지 1',
    category: 'COMPANY',
    categoryName: '회사공지',
    createdAt: DateTime(2026, 3, 1),
  ),
  Notice(
    id: 2,
    title: '테스트 공지 2',
    category: 'BRANCH',
    categoryName: '지점공지',
    createdAt: DateTime(2026, 3, 2),
  ),
];
