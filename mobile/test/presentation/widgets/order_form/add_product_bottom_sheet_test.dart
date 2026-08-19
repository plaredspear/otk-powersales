import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/presentation/providers/order_request_list_provider.dart';
import 'package:mobile/presentation/widgets/order_form/add_product_bottom_sheet.dart';

import '../../../helpers/fake_order_request_repository.dart';

/// 시트 헤더의 "이미 추가됨 N개" 표시.
///
/// 시트는 목록만 보여줘 사용자가 지금까지 몇 개를 담았는지 알 수 없었다 —
/// 호출 화면이 넘긴 개수를 헤더에 노출하는지 확인한다.
void main() {
  Widget host({int? addedCount, bool highlight = false}) {
    return ProviderScope(
      overrides: [
        orderRequestRepositoryProvider
            .overrideWithValue(FakeOrderRequestRepository()),
      ],
      child: MaterialApp(
        home: Scaffold(
          body: AddProductBottomSheet(
            addedCount: addedCount,
            highlightAddedCount: highlight,
          ),
        ),
      ),
    );
  }

  testWidgets('addedCount 미전달이면 개수 문구가 없다 (공용 화면 기본값)', (tester) async {
    await tester.pumpWidget(host());
    // Fake 저장소가 300ms 지연을 흉내내므로 타이머를 소진시킨 뒤 검증한다.
    await tester.pump(const Duration(milliseconds: 400));

    expect(find.textContaining('이미 추가됨'), findsNothing);
  });

  testWidgets('addedCount 전달 시 헤더에 개수를 노출한다', (tester) async {
    await tester.pumpWidget(host(addedCount: 108));
    // Fake 저장소가 300ms 지연을 흉내내므로 타이머를 소진시킨 뒤 검증한다.
    await tester.pump(const Duration(milliseconds: 400));

    expect(find.text('이미 추가됨 108개'), findsOneWidget);
  });

  testWidgets('강조 지정 시 경고색, 아니면 보조색으로 표시한다', (tester) async {
    await tester.pumpWidget(host(addedCount: 108, highlight: true));
    // Fake 저장소가 300ms 지연을 흉내내므로 타이머를 소진시킨 뒤 검증한다.
    await tester.pump(const Duration(milliseconds: 400));
    expect(
      tester.widget<Text>(find.text('이미 추가됨 108개')).style!.color,
      AppColors.error,
    );

    await tester.pumpWidget(host(addedCount: 42));
    // Fake 저장소가 300ms 지연을 흉내내므로 타이머를 소진시킨 뒤 검증한다.
    await tester.pump(const Duration(milliseconds: 400));
    expect(
      tester.widget<Text>(find.text('이미 추가됨 42개')).style!.color,
      AppColors.textSecondary,
    );
  });
}
