/// 주문 등록/취소 마감 시각 규칙 (백엔드 `OrderDeadlineCalculator` 동등).
///
/// 룰: `now + 1일 < 납기일 13:50` 이면 마감 전.
/// 즉 **실질 마감 시각 = (납기일 - 1일) 13:50** — 납기일이 내일이면 오늘 13:50 까지 주문할 수 있다.
///
/// 최종 판정은 서버(`ORD_DEADLINE_PASSED`)가 하고, 이 클래스는 주문서 작성 화면의 사전 안내용이다.
/// 화면 안내와 서버 거부가 어긋나지 않도록 규칙을 한 곳에 모아 둔다.
class OrderDeadline {
  const OrderDeadline._();

  /// 마감 시각(시) — 레거시 `dateConfirm` 13:50 룰.
  static const int cutoffHour = 13;

  /// 마감 시각(분).
  static const int cutoffMinute = 50;

  /// 해당 납기일의 실질 주문 마감 시각 = (납기일 - 1일) 13:50.
  static DateTime deadlineFor(DateTime deliveryDate) {
    // day - 1 은 DateTime 이 월/연 경계를 알아서 넘겨준다 (예: 3/1 → 2/28).
    return DateTime(
      deliveryDate.year,
      deliveryDate.month,
      deliveryDate.day - 1,
      cutoffHour,
      cutoffMinute,
    );
  }

  /// 지금(또는 [now]) 기준 이 납기일로 주문할 수 있는지 여부.
  static bool isWithinDeadline(DateTime deliveryDate, {DateTime? now}) {
    return (now ?? DateTime.now()).isBefore(deadlineFor(deliveryDate));
  }

  /// 지금(또는 [now]) 기준으로 **아직 주문할 수 있는 가장 이른 납기일** (시각 성분 없음).
  ///
  /// 13:50 이전이면 내일, 13:50 이후면 모레다 — 오늘 13:50 이 지나면 내일 납기 주문도
  /// 이미 마감이기 때문. 오늘 이전(= 오늘 포함)은 어떤 시각에도 주문할 수 없으므로
  /// 애초에 고를 수 없어야 한다 (레거시 `write.jsp:21` `min = 오늘+1일` 정합).
  static DateTime earliestDeliveryDate({DateTime? now}) {
    final current = now ?? DateTime.now();
    final tomorrow = DateTime(current.year, current.month, current.day + 1);
    // 내일 납기의 마감(= 오늘 13:50)이 이미 지났으면 하루 더 미룬다.
    return isWithinDeadline(tomorrow, now: current)
        ? tomorrow
        : DateTime(tomorrow.year, tomorrow.month, tomorrow.day + 1);
  }
}
