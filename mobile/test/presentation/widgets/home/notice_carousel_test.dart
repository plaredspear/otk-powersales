import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/notice.dart';
import 'package:mobile/presentation/widgets/home/notice_carousel.dart';

void main() {
  Widget buildTestWidget({
    List<Notice> notices = const [],
    void Function(Notice)? onNoticeTap,
    VoidCallback? onViewAllTap,
  }) {
    return MaterialApp(
      home: Scaffold(
        body: NoticeCarousel(
          notices: notices,
          onNoticeTap: onNoticeTap,
          onViewAllTap: onViewAllTap,
        ),
      ),
    );
  }

  group('NoticeCarousel', () {
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

        await tester.tap(find.text('전체보기'));
        expect(tapped, isTrue);
      });

      testWidgets('onViewAllTap이 null이면 탭해도 에러가 발생하지 않아야 한다',
          (tester) async {
        await tester.pumpWidget(buildTestWidget(
          notices: _sampleNotices,
          onViewAllTap: null,
        ));

        await tester.tap(find.text('전체보기'));
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
    type: 'ALL',
    createdAt: DateTime(2026, 3, 1),
  ),
  Notice(
    id: 2,
    title: '테스트 공지 2',
    type: 'BRANCH',
    createdAt: DateTime(2026, 3, 2),
  ),
];
