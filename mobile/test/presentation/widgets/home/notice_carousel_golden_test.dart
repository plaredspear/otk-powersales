import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/notice.dart';
import 'package:mobile/presentation/widgets/home/notice_carousel.dart';

/// NoticeCarousel 골든 테스트
void main() {
  final fixedNow = DateTime(2026, 5, 4, 12, 0);

  Widget wrap(Widget child) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        backgroundColor: const Color(0xFFFFFFFF),
        body: SizedBox(
          width: 393,
          child: child,
        ),
      ),
    );
  }

  testWidgets('empty (0개)', (tester) async {
    await tester.pumpWidget(wrap(const NoticeCarousel(notices: [])));
    await expectLater(
      find.byType(NoticeCarousel),
      matchesGoldenFile('../../../goldens/home/notice_empty.png'),
    );
  });

  testWidgets('single (1개)', (tester) async {
    final notices = [
      Notice(
        id: 1,
        title: '5월 신제품 진열 가이드 공지입니다.',
        category: 'COMPANY',
        categoryName: '회사공지',
        createdAt: fixedNow.subtract(const Duration(days: 2)),
      ),
    ];
    await tester.pumpWidget(wrap(NoticeCarousel(
      notices: notices,
      now: fixedNow,
    )));
    await expectLater(
      find.byType(NoticeCarousel),
      matchesGoldenFile('../../../goldens/home/notice_single.png'),
    );
  });

  testWidgets('three_with_new (3개 - 중간 1개 NEW)', (tester) async {
    final notices = [
      Notice(
        id: 1,
        title: '4월 마감 실적 공유',
        category: 'COMPANY',
        categoryName: '회사공지',
        createdAt: fixedNow.subtract(const Duration(days: 30)),
      ),
      Notice(
        id: 2,
        title: '5월 신제품 진열 가이드 공지 본문 길게 길게 길게 작성',
        category: 'BRANCH',
        categoryName: '지점공지',
        createdAt: fixedNow.subtract(const Duration(days: 3)),
      ),
      Notice(
        id: 3,
        title: '안전 점검 일정 안내',
        category: 'COMPANY',
        categoryName: '회사공지',
        createdAt: fixedNow.subtract(const Duration(days: 60)),
      ),
    ];
    await tester.pumpWidget(wrap(NoticeCarousel(
      notices: notices,
      now: fixedNow,
    )));
    await expectLater(
      find.byType(NoticeCarousel),
      matchesGoldenFile('../../../goldens/home/notice_three_with_new.png'),
    );
  });
}
