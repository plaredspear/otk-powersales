import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/common/date_range_filter_field.dart';

/// 기간 필드는 `라벨 + 기간 + 달력 아이콘` 이 한 줄에 들어가는지 실측해
/// 1줄/2단 배치를 자동 전환한다. 날짜가 ellipsis 로 잘리지 않는 것이 목적.
///
/// 주의: 테스트 환경에는 앱 폰트가 없어 글리프 폭이 실제보다 넓게 측정된다.
/// 그래서 "한 줄로 충분한 폭"/"부족한 폭" 을 폭 값으로 직접 만들어 분기를 검증한다.
void main() {
  Widget wrap(String label, double width) => MaterialApp(
        home: Scaffold(
          body: Center(
            child: SizedBox(
              width: width,
              child: DateRangeFilterField(
                label: label,
                startDate: DateTime(2026, 7, 31),
                endDate: DateTime(2026, 8, 7),
                onChanged: (_, _) {},
              ),
            ),
          ),
        ),
      );

  double labelFontSize(WidgetTester tester, String label) =>
      tester.widget<Text>(find.text(label)).style!.fontSize!;

  testWidgets('한 줄에 들어가면 라벨을 기간 왼쪽에 붙여 배치한다', (tester) async {
    await tester.pumpWidget(wrap('기간', 600));

    expect(find.text('기간'), findsOneWidget);
    expect(find.text('2026-07-31 ~ 2026-08-07'), findsOneWidget);
    // 한 줄 배치의 라벨은 본문과 같은 14sp.
    expect(labelFontSize(tester, '기간'), 14);
    // 같은 줄에 나란히.
    expect(tester.getTopLeft(find.text('기간')).dy,
        tester.getTopLeft(find.text('2026-07-31 ~ 2026-08-07')).dy);
  });

  testWidgets('한 줄에 안 들어가면 2단 배치로 전환되고 날짜가 잘리지 않는다', (tester) async {
    await tester.pumpWidget(wrap('클레임 발생일', 300));

    expect(find.text('클레임 발생일'), findsOneWidget);
    expect(find.text('2026-07-31 ~ 2026-08-07'), findsOneWidget);
    // 2단 배치의 라벨은 보조 텍스트 12sp.
    expect(labelFontSize(tester, '클레임 발생일'), 12);

    // 2단으로 쌓였는지: 라벨이 날짜보다 위에 위치.
    final labelY = tester.getTopLeft(find.text('클레임 발생일')).dy;
    final valueY = tester.getTopLeft(find.text('2026-07-31 ~ 2026-08-07')).dy;
    expect(labelY, lessThan(valueY));

    // 필드 높이는 다른 화면과 동일하게 유지.
    expect(
      tester.getSize(find.byType(DateRangeFilterField)).height,
      DateRangeFilterField.fieldHeight,
    );
  });
}
