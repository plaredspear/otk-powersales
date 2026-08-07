import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/claim/claim_status_info_sheet.dart';

/// 클레임 상태 뱃지 값은 고정 목록이 아니라(코스모스 회신 원문) 안내가 필요해서 붙인 시트.
/// 세 갈래(미확인 / 코스모스 회신값 / 전송실패) 설명이 모두 보이는지 확인한다.
void main() {
  testWidgets('info 아이콘 탭 시 상태 설명 3종이 표시된다', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) => IconButton(
              icon: const Icon(Icons.info_outline),
              onPressed: () => ClaimStatusInfoSheet.show(context),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.byIcon(Icons.info_outline));
    await tester.pumpAndSettle();

    expect(find.text('클레임 상태 안내'), findsOneWidget);
    expect(find.text('미확인'), findsOneWidget);
    expect(find.text('조치중'), findsOneWidget);
    expect(find.text('상태는 매시간 자동으로 갱신됩니다.'), findsOneWidget);

    // 시트 목록은 스크롤 영역 — 작은 화면에서는 마지막 항목이 스크롤 뒤에 있다.
    await tester.scrollUntilVisible(find.text('전송실패'), 100,
        scrollable: find.byType(Scrollable).last);
    expect(find.text('전송실패'), findsOneWidget);
    // 등록 자체는 완료됐다는 점이 전송실패 설명의 핵심.
    expect(find.textContaining('클레임 등록 자체는 완료'), findsOneWidget);

    await tester.tap(find.text('닫기'));
    await tester.pumpAndSettle();
    expect(find.text('클레임 상태 안내'), findsNothing);
  });
}
