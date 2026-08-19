import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/providers/add_product_provider.dart';
import 'package:mobile/presentation/providers/order_request_list_provider.dart';
import 'package:mobile/presentation/widgets/order_form/add_product_bottom_sheet.dart';

import '../../../helpers/fake_order_request_repository.dart';

/// 확정 버튼의 `선택 수 / 추가 후 총 수` 표시.
///
/// 시트는 목록만 보여줘 사용자가 지금까지 몇 개를 담았는지 알 수 없었다 —
/// 누르면 몇 개가 되는지를 버튼이 직접 알리는지 확인한다.
void main() {
  late ProviderContainer container;

  Widget host({Set<String>? addedProductCodes}) {
    return UncontrolledProviderScope(
      container: container,
      child: MaterialApp(
        home: Scaffold(
          body: AddProductBottomSheet(addedProductCodes: addedProductCodes),
        ),
      ),
    );
  }

  setUp(() {
    container = ProviderContainer(
      overrides: [
        orderRequestRepositoryProvider
            .overrideWithValue(FakeOrderRequestRepository()),
      ],
    );
  });

  tearDown(() => container.dispose());

  /// 시트 밖에서 선택 상태를 만든다 (탭 목록 로딩에 의존하지 않기 위함).
  void select(List<String> productCodes) {
    for (final code in productCodes) {
      container.read(addProductProvider.notifier).toggleProductSelection(code);
    }
  }

  /// Fake 저장소가 300ms 지연을 흉내내므로 타이머를 소진시킨 뒤 검증한다.
  Future<void> settle(WidgetTester tester) =>
      tester.pump(const Duration(milliseconds: 400));

  testWidgets('addedProductCodes 미전달이면 종전 라벨을 유지한다 (공용 화면 기본값)', (tester) async {
    await tester.pumpWidget(host());
    await settle(tester);
    // 헤더 제목도 '제품 추가' 라 버튼 안에서 찾는다.
    expect(
      find.widgetWithText(ElevatedButton, '제품 추가'),
      findsOneWidget,
    );

    select(['P001', 'P002']);
    await tester.pump();
    expect(find.text('제품 추가 (2개)'), findsOneWidget);
  });

  testWidgets('추가 후 총 수를 함께 보여준다 — 담김 64 + 선택 2 → 66', (tester) async {
    await tester.pumpWidget(
      host(addedProductCodes: {for (var i = 0; i < 64; i++) 'A$i'}),
    );
    await settle(tester);
    // 선택 전에도 현재 담긴 수를 알 수 있다.
    expect(find.text('제품 추가 (0 / 64)'), findsOneWidget);

    select(['P001', 'P002']);
    await tester.pump();
    expect(find.text('제품 추가 (2 / 66)'), findsOneWidget);
  });

  testWidgets('이미 담긴 제품을 다시 고르면 총 수에 더해지지 않는다 (중복 무시 정합)',
      (tester) async {
    await tester.pumpWidget(host(addedProductCodes: {'A1', 'A2'}));
    await settle(tester);

    // A1 은 이미 담겨 있어 추가되지 않는다 — 선택 2건이지만 결과는 3건.
    select(['A1', 'P001']);
    await tester.pump();

    expect(find.text('제품 추가 (2 / 3)'), findsOneWidget);
  });
}
