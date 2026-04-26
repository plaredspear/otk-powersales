/// 행사 매출 정보 엔티티
///
/// 행사의 목표/달성 금액 및 진행율 정보를 담는 도메인 엔티티입니다.
class EventSalesInfo {
  /// 행사 ID
  final String eventId;

  /// 목표 금액 (원)
  final int targetAmount;

  /// 달성 금액 (원)
  final int achievedAmount;

  /// 달성율 (%)
  final double achievementRate;

  /// 기간 진행율 (%)
  final double progressRate;

  const EventSalesInfo({
    required this.eventId,
    required this.targetAmount,
    required this.achievedAmount,
    required this.achievementRate,
    required this.progressRate,
  });

  EventSalesInfo copyWith({
    String? eventId,
    int? targetAmount,
    int? achievedAmount,
    double? achievementRate,
    double? progressRate,
  }) {
    return EventSalesInfo(
      eventId: eventId ?? this.eventId,
      targetAmount: targetAmount ?? this.targetAmount,
      achievedAmount: achievedAmount ?? this.achievedAmount,
      achievementRate: achievementRate ?? this.achievementRate,
      progressRate: progressRate ?? this.progressRate,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'eventId': eventId,
      'targetAmount': targetAmount,
      'achievedAmount': achievedAmount,
      'achievementRate': achievementRate,
      'progressRate': progressRate,
    };
  }

  factory EventSalesInfo.fromJson(Map<String, dynamic> json) {
    return EventSalesInfo(
      eventId: json['eventId'] as String,
      targetAmount: json['targetAmount'] as int,
      achievedAmount: json['achievedAmount'] as int,
      achievementRate: (json['achievementRate'] as num).toDouble(),
      progressRate: (json['progressRate'] as num).toDouble(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EventSalesInfo &&
        other.eventId == eventId &&
        other.targetAmount == targetAmount &&
        other.achievedAmount == achievedAmount &&
        other.achievementRate == achievementRate &&
        other.progressRate == progressRate;
  }

  @override
  int get hashCode {
    return Object.hash(
      eventId,
      targetAmount,
      achievedAmount,
      achievementRate,
      progressRate,
    );
  }

  @override
  String toString() {
    return 'EventSalesInfo(eventId: $eventId, targetAmount: $targetAmount, '
        'achievedAmount: $achievedAmount, achievementRate: $achievementRate, '
        'progressRate: $progressRate)';
  }
}
