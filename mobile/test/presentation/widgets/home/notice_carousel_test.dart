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
    group('카드 가로폭 비율', () {
      testWidgets('일반 디바이스(393dp)에서 카드 폭이 부모의 40%여야 한다',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          notices: _sampleNotices,
          screenWidth: 393,
        ));

        // 공지 카드의 Container width 확인
        final containers = tester.widgetList<Container>(find.byType(Container))
            .where((c) => c.constraints != null &&
                (c.constraints!.maxWidth - 393 * 0.4).abs() < 0.01)
            .toList();
        expect(containers, isNotEmpty,
            reason: '카드 폭이 393 * 0.4 = 157.2dp이어야 한다');
      });

      testWidgets('소형 디바이스(320dp)에서 카드 폭이 부모의 40%여야 한다',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          notices: _sampleNotices,
          screenWidth: 320,
        ));

        final containers = tester.widgetList<Container>(find.byType(Container))
            .where((c) => c.constraints != null &&
                (c.constraints!.maxWidth - 320 * 0.4).abs() < 0.01)
            .toList();
        expect(containers, isNotEmpty,
            reason: '카드 폭이 320 * 0.4 = 128dp이어야 한다');
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
