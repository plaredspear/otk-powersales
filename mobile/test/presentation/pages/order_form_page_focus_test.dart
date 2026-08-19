import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/presentation/pages/order_form_page.dart';
import 'package:mobile/presentation/widgets/account/account_selector_field.dart';
import 'package:mobile/presentation/providers/order_form_provider.dart';
import 'package:mobile/presentation/widgets/order_form/delivery_date_picker.dart';

import '../../helpers/fake_order_form_repository.dart';

/// 비활성 승인요청 버튼을 눌렀을 때 "고쳐야 하는 항목" 으로 데려가는지 확인한다.
///
/// 토스트만 띄우면 사용자는 어디를 고쳐야 할지 찾지 못한다 — 수량 미입력만 줄로
/// 이동시키고 납기일/거래처는 그대로 있던 것이 이 테스트가 막는 회귀다.
void main() {
  late ProviderContainer container;
  late FakeOrderFormRepository fakeRepo;

  setUp(() {
    fakeRepo = FakeOrderFormRepository();
    container = ProviderContainer(
      overrides: [orderFormRepositoryProvider.overrideWithValue(fakeRepo)],
    );
    // autoDispose provider 가 구독 없이 폐기되지 않도록 붙잡아 둔다.
    container.listen(orderFormProvider, (_, __) {});
  });

  tearDown(() => container.dispose());

  Widget host() {
    return UncontrolledProviderScope(
      container: container,
      child: const MaterialApp(home: OrderFormPage()),
    );
  }

  /// 마감이 지난 납기일(= 오늘) + 제품 여러 건인 상태를 만든다.
  void seedPastDeadlineForm() {
    final notifier = container.read(orderFormProvider.notifier);
    notifier.state = notifier.state.copyWith(
      selectedAccountId: 5678,
      orderDraft: notifier.state.orderDraft.copyWith(
        clientId: 5678,
        clientName: '(주)일동',
        deliveryDate: DateTime.now(),
        items: List.generate(
          20,
          (i) => OrderDraftItem(
            productCode: 'P${i.toString().padLeft(3, '0')}',
            productName: '테스트제품 $i',
            quantityBoxes: 1,
            quantityPieces: 0,
            unitPrice: 1000,
            boxSize: 10,
            totalPrice: 10000,
          ),
        ),
      ),
    );
  }

  testWidgets('납기일 확인필요 → 탭하면 납기일 입력으로 이동한다', (tester) async {
    await tester.pumpWidget(host());
    await tester.pumpAndSettle();

    seedPastDeadlineForm();
    await tester.pumpAndSettle();

    // 납기일이 화면 밖으로 나가도록 목록을 아래로 스크롤한다.
    await tester.drag(find.byType(CustomScrollView), const Offset(0, -600));
    await tester.pumpAndSettle();

    final screenHeight =
        tester.view.physicalSize.height / tester.view.devicePixelRatio;
    // 전제: 납기일 입력이 화면 밖으로 벗어나 있다 (멀리 밀리면 sliver 가 폐기돼 아예 사라진다).
    final beforeTap = find.byType(DeliveryDatePicker);
    if (beforeTap.evaluate().isNotEmpty) {
      expect(tester.getRect(beforeTap).bottom, lessThan(0));
    }

    await tester.tap(find.text('납기일 확인필요'));
    await tester.pumpAndSettle();

    final rect = tester.getRect(find.byType(DeliveryDatePicker));
    expect(rect.top, greaterThanOrEqualTo(0));
    expect(rect.bottom, lessThanOrEqualTo(screenHeight));
  });

  testWidgets('거래처 미선택 → 탭하면 거래처 입력으로 이동한다', (tester) async {
    await tester.pumpWidget(host());
    await tester.pumpAndSettle();

    seedPastDeadlineForm();
    // 거래처만 비운다 — 첫 차단 사유가 거래처가 된다.
    final notifier = container.read(orderFormProvider.notifier);
    notifier.state = notifier.state.copyWith(clearSelectedAccountId: true);
    await tester.pumpAndSettle();

    await tester.drag(find.byType(CustomScrollView), const Offset(0, -600));
    await tester.pumpAndSettle();

    await tester.tap(find.text('승인요청'));
    await tester.pumpAndSettle();

    final screenHeight =
        tester.view.physicalSize.height / tester.view.devicePixelRatio;
    final rect = tester.getRect(find.byType(AccountSelectorField));
    expect(rect.top, greaterThanOrEqualTo(0));
    expect(rect.bottom, lessThanOrEqualTo(screenHeight));
  });
}
