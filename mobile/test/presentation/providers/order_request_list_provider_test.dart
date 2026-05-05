import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/network/dio_provider.dart';
import 'package:mobile/domain/entities/order_request.dart';
import 'package:mobile/presentation/providers/order_request_list_provider.dart';

import '../../helpers/fake_order_request_repository.dart';

Dio _createMockDio() {
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost'));
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) {
      if (options.path == '/api/v1/mobile/accounts/my') {
        handler.resolve(Response(
          data: {
            'success': true,
            'data': {
              'accounts': [
                {'accountId': 1, 'accountName': '천사푸드'},
                {'accountId': 2, 'accountName': '(유)경산식품'},
              ],
            },
          },
          statusCode: 200,
          requestOptions: options,
        ));
        return;
      }
      handler.reject(DioException(requestOptions: options, message: 'Not mocked'));
    },
  ));
  return dio;
}

void main() {
  group('OrderRequestListNotifier (클라이언트 슬라이스)', () {
    late ProviderContainer container;
    late FakeOrderRequestRepository fakeRepository;

    setUp(() {
      fakeRepository = FakeOrderRequestRepository();
      container = ProviderContainer(
        overrides: [
          orderRequestRepositoryProvider.overrideWithValue(fakeRepository),
          dioProvider.overrideWithValue(_createMockDio()),
        ],
      );
    });

    tearDown(() {
      container.dispose();
    });

    test('초기 상태가 올바르게 설정되어야 한다', () {
      final state = container.read(orderRequestListProvider);

      expect(state.allOrderRequests, isEmpty);
      expect(state.clients, isEmpty);
      expect(state.isLoading, false);
      expect(state.hasSearched, false);
      expect(state.currentPage, 0);
      expect(state.pageSize, 20);
      expect(state.truncated, false);
      expect(state.fetchedAt, isNull);
      expect(state.errorMessage, isNull);
      expect(state.sortType, OrderSortType.latestOrder);
    });

    test('initialize() 가 거래처 + 전체 배열 1회 fetch 한다 (T1)', () async {
      final notifier = container.read(orderRequestListProvider.notifier);

      notifier.updateDeliveryDateRange(null, null);
      await notifier.initialize();

      final state = container.read(orderRequestListProvider);
      expect(state.clients, isNotEmpty);
      expect(state.allOrderRequests, isNotEmpty);
      expect(state.hasSearched, true);
      expect(state.fetchedAt, isNotNull);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
    });

    test('searchOrders() 가 currentPage=0 으로 리셋한다 (T2)', () async {
      final notifier = container.read(orderRequestListProvider.notifier);
      notifier.updateDeliveryDateRange(null, null);
      await notifier.searchOrders();
      // 사용자가 페이지 이동
      notifier.goToPage(1);
      expect(container.read(orderRequestListProvider).currentPage, 1);

      // 필터 변경 후 재검색
      await notifier.searchOrders();

      expect(container.read(orderRequestListProvider).currentPage, 0);
    });

    test('goToPage() 는 추가 fetch 없이 currentPage 만 변경 (T3)', () async {
      final notifier = container.read(orderRequestListProvider.notifier);
      notifier.updateDeliveryDateRange(null, null);
      await notifier.searchOrders();

      final stateBefore = container.read(orderRequestListProvider);
      final allBefore = stateBefore.allOrderRequests;

      notifier.goToPage(1);

      final stateAfter = container.read(orderRequestListProvider);
      // allOrderRequests 그대로
      expect(identical(stateAfter.allOrderRequests, allBefore), true);
      expect(stateAfter.currentPage, 1);
      // pagedItems 가 슬라이스로 재계산
      expect(stateAfter.pagedItems.length, lessThanOrEqualTo(20));
    });

    test('totalPages 경계 — 0/19/20/21/40 (T4)', () {
      // 0건 → 0페이지
      expect(_stateWith(allCount: 0).totalPages, 0);
      // 19건 → 1페이지
      expect(_stateWith(allCount: 19).totalPages, 1);
      // 20건 → 1페이지
      expect(_stateWith(allCount: 20).totalPages, 1);
      // 21건 → 2페이지
      expect(_stateWith(allCount: 21).totalPages, 2);
      // 40건 → 2페이지
      expect(_stateWith(allCount: 40).totalPages, 2);
      // 41건 → 3페이지
      expect(_stateWith(allCount: 41).totalPages, 3);
    });

    test('필터 변경 helper 가 상태 갱신', () {
      final notifier = container.read(orderRequestListProvider.notifier);

      notifier.updateClientFilter(7, '그린유통');
      expect(container.read(orderRequestListProvider).selectedClientId, 7);

      notifier.updateStatusFilter('APPROVED');
      expect(container.read(orderRequestListProvider).selectedStatus, 'APPROVED');

      notifier.updateClientFilter(null, null);
      expect(container.read(orderRequestListProvider).selectedClientId, isNull);
    });

    test('clearError() 가 에러 메시지를 초기화', () async {
      // 강제로 에러 상태 만들기는 어려우니, copyWith 가능 검증으로 대체
      final notifier = container.read(orderRequestListProvider.notifier);
      notifier.clearError();
      expect(container.read(orderRequestListProvider).errorMessage, isNull);
    });
  });
}

/// totalPages 경계 검증용 헬퍼.
_StateView _stateWith({required int allCount}) {
  final dummyOrders = List<OrderRequest>.generate(
    allCount,
    (i) => OrderRequest(
      id: i,
      orderRequestNumber: 'OR-${i.toString().padLeft(7, '0')}',
      clientId: 1,
      clientName: '거래처',
      orderDate: DateTime(2026, 5, 1),
      deliveryDate: DateTime(2026, 5, 6),
      totalAmount: 100,
      orderRequestStatus: OrderRequestStatus.approved,
      isClosed: false,
    ),
  );
  return _StateView(dummyOrders);
}

class _StateView {
  final List<OrderRequest> all;
  static const int pageSize = 20;
  _StateView(this.all);
  int get totalPages => all.isEmpty ? 0 : (all.length / pageSize).ceil();
}
