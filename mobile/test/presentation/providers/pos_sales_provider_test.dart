import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/providers/pos_sales_provider.dart';
import 'package:mobile/presentation/providers/pos_sales_state.dart';

void main() {
  group('PosSalesProvider', () {
    late ProviderContainer container;

    setUp(() {
      container = ProviderContainer();
    });

    tearDown(() {
      container.dispose();
    });

    test('초기 상태가 올바르게 설정된다', () {
      final state = container.read(posSalesProvider);

      expect(state.sales, isEmpty);
      expect(state.isLoading, false);
      expect(state.errorMessage, null);
      expect(state.totalAmount, 0);
      expect(state.totalQuantity, 0);
    });

    test('fetchSales 호출 시 로딩 상태로 전환된다', () async {
      final notifier = container.read(posSalesProvider.notifier);

      // 비동기 작업 시작
      final future = notifier.fetchSales(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
      );

      // 첫 프레임에서 로딩 상태 확인
      await Future.microtask(() {});
      final loadingState = container.read(posSalesProvider);
      expect(loadingState.isLoading, true);

      // 작업 완료 대기
      await future;
    });

    test('fetchSales 성공 시 데이터와 합계가 올바르게 설정된다', () async {
      final notifier = container.read(posSalesProvider.notifier);

      await notifier.fetchSales(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
      );

      final state = container.read(posSalesProvider);

      expect(state.isLoading, false);
      expect(state.errorMessage, null);
      expect(state.sales, isNotEmpty);
      expect(state.totalAmount, greaterThan(0));
      expect(state.totalQuantity, greaterThan(0));
    });

    test('fetchSales로 필터가 적용된다', () async {
      final notifier = container.read(posSalesProvider.notifier);

      await notifier.fetchSales(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        storeName: '이마트',
      );

      final state = container.read(posSalesProvider);

      expect(state.filter.startDate, DateTime(2026, 1, 1));
      expect(state.filter.endDate, DateTime(2026, 1, 31));
      expect(state.filter.storeName, '이마트');
    });

    test('fetchSalesByStore로 매장별 조회가 동작한다', () async {
      final notifier = container.read(posSalesProvider.notifier);

      await notifier.fetchSalesByStore(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        storeName: '이마트 강남점',
      );

      final state = container.read(posSalesProvider);

      expect(state.isLoading, false);
      expect(state.sales, isNotEmpty);
      expect(
        state.sales.every((sale) => sale.storeName.contains('이마트')),
        true,
      );
      expect(state.filter.storeName, '이마트 강남점');
    });

    test('fetchSalesByProduct로 제품별 조회가 동작한다', () async {
      final notifier = container.read(posSalesProvider.notifier);

      await notifier.fetchSalesByProduct(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        productName: '진라면',
      );

      final state = container.read(posSalesProvider);

      expect(state.isLoading, false);
      expect(state.sales, isNotEmpty);
      expect(
        state.sales.every((sale) => sale.productName.contains('진라면')),
        true,
      );
      expect(state.filter.productName, '진라면');
    });

    test('updateFilter로 필터를 업데이트하고 재조회한다', () async {
      final notifier = container.read(posSalesProvider.notifier);

      final newFilter = PosSalesFilter(
        startDate: DateTime(2026, 1, 15),
        endDate: DateTime(2026, 1, 20),
        storeName: '홈플러스',
      );

      await notifier.updateFilter(newFilter);

      final state = container.read(posSalesProvider);

      expect(state.filter.startDate, DateTime(2026, 1, 15));
      expect(state.filter.endDate, DateTime(2026, 1, 20));
      expect(state.filter.storeName, '홈플러스');
      expect(state.sales, isNotEmpty);
    });

    test('resetFilter로 필터를 초기화한다', () async {
      final notifier = container.read(posSalesProvider.notifier);

      // 먼저 필터를 변경
      await notifier.fetchSales(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        storeName: '이마트',
      );

      // 필터 초기화
      await notifier.resetFilter();

      final state = container.read(posSalesProvider);

      // 기본 필터(최근 30일)로 돌아감
      expect(state.filter.storeName, null);
      expect(state.filter.productName, null);
    });

    test('에러 발생 시 errorMessage가 설정된다', () async {
      final notifier = container.read(posSalesProvider.notifier);

      // 잘못된 날짜 범위로 에러 유발
      await notifier.fetchSales(
        startDate: DateTime(2026, 1, 31),
        endDate: DateTime(2026, 1, 1), // startDate > endDate
      );

      final state = container.read(posSalesProvider);

      expect(state.isLoading, false);
      expect(state.errorMessage, isNotNull);
    });

    test('합계 계산이 정확하다', () async {
      final notifier = container.read(posSalesProvider.notifier);

      await notifier.fetchSales(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
      );

      final state = container.read(posSalesProvider);

      // 수동으로 합계 계산하여 검증
      final expectedAmount = state.sales.fold<int>(
        0,
        (sum, sale) => sum + sale.amount,
      );
      final expectedQuantity = state.sales.fold<int>(
        0,
        (sum, sale) => sum + sale.quantity,
      );

      expect(state.totalAmount, expectedAmount);
      expect(state.totalQuantity, expectedQuantity);
    });

    test('연속 조회 시 상태가 올바르게 업데이트된다', () async {
      final notifier = container.read(posSalesProvider.notifier);

      // 첫 번째 조회
      await notifier.fetchSales(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 15),
      );

      final firstState = container.read(posSalesProvider);
      final firstSalesCount = firstState.sales.length;

      // 두 번째 조회
      await notifier.fetchSales(
        startDate: DateTime(2026, 1, 16),
        endDate: DateTime(2026, 1, 31),
      );

      final secondState = container.read(posSalesProvider);

      // 두 번째 조회 결과가 첫 번째와 다름
      expect(secondState.sales.length != firstSalesCount, true);
      expect(secondState.filter.startDate, DateTime(2026, 1, 16));
    });
  });

  group('PosSalesFilter', () {
    test('defaultFilter가 최근 30일을 반환한다', () {
      final filter = PosSalesFilter.defaultFilter();

      final now = DateTime.now();
      final expectedStart = DateTime(now.year, now.month - 1, now.day);

      expect(filter.startDate.year, expectedStart.year);
      expect(filter.startDate.month, expectedStart.month);
      expect(filter.startDate.day, expectedStart.day);
      expect(filter.endDate.day, now.day);
    });

    test('PosSalesFilter 생성이 올바르게 동작한다', () {
      final filter = PosSalesFilter(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        storeName: '이마트',
        productName: '진라면',
      );

      expect(filter.startDate, DateTime(2026, 1, 1));
      expect(filter.endDate, DateTime(2026, 1, 31));
      expect(filter.storeName, '이마트');
      expect(filter.productName, '진라면');
    });
  });

  group('PosSalesState', () {
    test('initial 상태가 올바르게 생성된다', () {
      final state = PosSalesState.initial();

      expect(state.sales, isEmpty);
      expect(state.isLoading, false);
      expect(state.errorMessage, null);
      expect(state.totalAmount, 0);
      expect(state.totalQuantity, 0);
      expect(state.filter, isA<PosSalesFilter>());
    });

    test('copyWith로 상태를 부분 업데이트할 수 있다', () {
      final state = PosSalesState.initial();

      final updatedState = state.copyWith(
        isLoading: true,
        errorMessage: '에러 발생',
      );

      expect(updatedState.isLoading, true);
      expect(updatedState.errorMessage, '에러 발생');
      // 다른 필드는 유지
      expect(updatedState.sales, state.sales);
      expect(updatedState.filter, state.filter);
    });
  });
}
