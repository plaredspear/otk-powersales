import '../entities/daily_sales_summary.dart';
import '../entities/event.dart';
import '../entities/event_sales_info.dart';

/// 행사 Repository 인터페이스
///
/// 행사(프로모션) 목록 조회, 상세 조회, 일별 매출 조회 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class EventRepository {
  /// 행사 목록 조회
  ///
  /// [customerId]: 거래처 ID 필터 (null이면 전체 거래처)
  /// [startDate]: 검색 기간 시작일 (null이면 제한 없음)
  /// [endDate]: 검색 기간 종료일 (null이면 제한 없음)
  /// [page]: 페이지 번호 (1부터 시작, 기본값 1)
  /// [size]: 페이지 크기 (기본값 10)
  ///
  /// Returns: 필터링된 행사 목록
  Future<List<Event>> getEvents({
    String? customerId,
    DateTime? startDate,
    DateTime? endDate,
    int page = 1,
    int size = 10,
  });

  /// 행사 상세 조회
  ///
  /// 행사 기본 정보, 매출 정보, 제품 정보를 함께 조회합니다.
  ///
  /// [eventId]: 행사 ID
  ///
  /// Returns: 행사 상세 정보 (Event + EventSalesInfo)
  Future<(Event event, EventSalesInfo salesInfo)> getEventDetail(
    String eventId,
  );

  /// 행사 일별 매출 목록 조회
  ///
  /// 특정 행사에 등록된 일별 판매금액 목록을 최신순으로 조회합니다.
  ///
  /// [eventId]: 행사 ID
  ///
  /// Returns: 일별 매출 요약 목록 (최신순 정렬)
  Future<List<DailySalesSummary>> getDailySales(String eventId);
}
