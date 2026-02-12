import '../../domain/entities/daily_sales_summary.dart';
import '../../domain/entities/event.dart';
import '../../domain/entities/event_sales_info.dart';

/// 행사 상세 화면 상태
///
/// 행사 정보, 매출 정보, 일매출 목록, 로딩/에러 상태를 포함합니다.
class EventDetailState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 행사 기본 정보
  final Event? event;

  /// 매출 정보
  final EventSalesInfo? salesInfo;

  /// 일별 판매금액 목록
  final List<DailySalesSummary> dailySalesList;

  /// 일별 판매금액 로딩 상태
  final bool isDailySalesLoading;

  const EventDetailState({
    this.isLoading = false,
    this.errorMessage,
    this.event,
    this.salesInfo,
    this.dailySalesList = const [],
    this.isDailySalesLoading = false,
  });

  /// 초기 상태
  factory EventDetailState.initial() {
    return const EventDetailState();
  }

  /// 로딩 상태로 전환
  EventDetailState toLoading() {
    return copyWith(
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  EventDetailState toError(String message) {
    return copyWith(
      isLoading: false,
      errorMessage: message,
    );
  }

  /// 일매출 등록 가능 여부 (오늘 날짜가 행사 기간 내에 포함되는지)
  bool get canRegisterDailySales {
    if (event == null) return false;
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    return !today.isBefore(event!.startDate) &&
        !today.isAfter(event!.endDate);
  }

  /// 오늘 일매출 등록 완료 여부
  bool get isTodayRegistered {
    if (dailySalesList.isEmpty) return false;
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    return dailySalesList.any((sale) {
      final saleDate =
          DateTime(sale.salesDate.year, sale.salesDate.month, sale.salesDate.day);
      return saleDate == today && sale.status == 'REGISTERED';
    });
  }

  EventDetailState copyWith({
    bool? isLoading,
    String? errorMessage,
    Event? event,
    EventSalesInfo? salesInfo,
    List<DailySalesSummary>? dailySalesList,
    bool? isDailySalesLoading,
  }) {
    return EventDetailState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      event: event ?? this.event,
      salesInfo: salesInfo ?? this.salesInfo,
      dailySalesList: dailySalesList ?? this.dailySalesList,
      isDailySalesLoading: isDailySalesLoading ?? this.isDailySalesLoading,
    );
  }
}
