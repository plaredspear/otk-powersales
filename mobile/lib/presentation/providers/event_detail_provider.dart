import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/usecases/get_event_detail.dart';
import 'event_detail_state.dart';
import 'event_sales_provider.dart';

// --- Dependency Providers ---

/// GetEventDetail UseCase Provider
final getEventDetailUseCaseProvider = Provider<GetEventDetailUseCase>((ref) {
  final repository = ref.watch(eventRepositoryProvider);
  return GetEventDetailUseCase(repository);
});

// --- EventDetailNotifier ---

/// 행사 상세 화면 상태 관리 Notifier
///
/// 행사 정보, 매출 정보, 일매출 목록을 관리합니다.
class EventDetailNotifier extends StateNotifier<EventDetailState> {
  final GetEventDetailUseCase _getEventDetail;
  final Ref _ref;

  EventDetailNotifier({
    required GetEventDetailUseCase getEventDetail,
    required Ref ref,
  })  : _getEventDetail = getEventDetail,
        _ref = ref,
        super(EventDetailState.initial());

  /// 행사 상세 로딩
  Future<void> loadEventDetail(String eventId) async {
    try {
      state = state.toLoading();

      final result = await _getEventDetail.call(eventId);

      state = state.copyWith(
        isLoading: false,
        event: result.$1,
        salesInfo: result.$2,
      );

      // 일매출 목록도 함께 로딩
      await _loadDailySales(eventId);
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// 일매출 목록 로딩
  Future<void> _loadDailySales(String eventId) async {
    try {
      state = state.copyWith(isDailySalesLoading: true);

      final repository = _ref.read(eventRepositoryProvider);
      final dailySales = await repository.getDailySales(eventId);

      state = state.copyWith(
        isDailySalesLoading: false,
        dailySalesList: dailySales,
      );
    } catch (e) {
      // 일매출 로딩 실패는 전체 에러로 처리하지 않음
      state = state.copyWith(
        isDailySalesLoading: false,
        dailySalesList: [],
      );
    }
  }

  /// 일매출 목록 새로고침
  Future<void> refreshDailySales(String eventId) async {
    await _loadDailySales(eventId);
  }

  /// 새로고침
  Future<void> refresh(String eventId) async {
    await loadEventDetail(eventId);
  }
}

/// Event Detail Provider
final eventDetailProvider =
    StateNotifierProvider<EventDetailNotifier, EventDetailState>((ref) {
  final getEventDetail = ref.watch(getEventDetailUseCaseProvider);
  return EventDetailNotifier(
    getEventDetail: getEventDetail,
    ref: ref,
  );
});
