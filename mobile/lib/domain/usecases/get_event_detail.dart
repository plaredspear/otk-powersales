import '../entities/event.dart';
import '../entities/event_sales_info.dart';
import '../repositories/event_repository.dart';

/// 행사 상세 조회 UseCase
///
/// 행사 기본 정보와 매출 정보를 함께 조회합니다.
class GetEventDetailUseCase {
  final EventRepository _repository;

  GetEventDetailUseCase(this._repository);

  /// 행사 상세 조회 실행
  ///
  /// [eventId]: 조회할 행사 ID
  ///
  /// Returns: (Event, EventSalesInfo) 튜플
  ///   - Event: 행사 기본 정보 (행사명, 기간, 제품 등)
  ///   - EventSalesInfo: 매출 정보 (목표/달성/진행율)
  ///
  /// Throws:
  /// - [ArgumentError] eventId가 빈 문자열인 경우
  Future<(Event event, EventSalesInfo salesInfo)> call(
    String eventId,
  ) async {
    // eventId 검증
    if (eventId.trim().isEmpty) {
      throw ArgumentError('행사 ID는 빈 문자열일 수 없습니다');
    }

    // Repository에서 행사 상세 조회
    return await _repository.getEventDetail(eventId);
  }
}
