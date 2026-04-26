import '../entities/event.dart';
import '../repositories/event_repository.dart';

/// 행사 목록 조회 UseCase
///
/// 거래처별, 기간별로 행사 목록을 페이지네이션하여 조회합니다.
class GetEventsUseCase {
  final EventRepository _repository;

  GetEventsUseCase(this._repository);

  /// 행사 목록 조회 실행
  ///
  /// [customerId]: 거래처 ID 필터 (null이면 전체 거래처)
  /// [startDate]: 검색 기간 시작일 (null이면 제한 없음)
  /// [endDate]: 검색 기간 종료일 (null이면 제한 없음)
  /// [page]: 페이지 번호 (1부터 시작, 기본값 1)
  /// [size]: 페이지 크기 (기본값 10)
  ///
  /// Returns: 필터링된 행사 목록
  ///
  /// Throws:
  /// - [ArgumentError] 페이지 번호나 크기가 유효하지 않은 경우
  /// - [ArgumentError] 날짜 범위가 유효하지 않은 경우
  Future<List<Event>> call({
    String? customerId,
    DateTime? startDate,
    DateTime? endDate,
    int page = 1,
    int size = 10,
  }) async {
    // 페이지 번호 검증
    if (page < 1) {
      throw ArgumentError('페이지 번호는 1 이상이어야 합니다');
    }

    // 페이지 크기 검증
    if (size < 1 || size > 100) {
      throw ArgumentError('페이지 크기는 1~100 사이여야 합니다');
    }

    // 날짜 범위 검증
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      throw ArgumentError('시작일은 종료일보다 이전이어야 합니다');
    }

    // Repository에서 행사 목록 조회
    return await _repository.getEvents(
      customerId: customerId,
      startDate: startDate,
      endDate: endDate,
      page: page,
      size: size,
    );
  }
}
