import '../../domain/entities/event_sales_info.dart';

/// EventSalesInfo API 모델 (DTO)
///
/// API 응답의 JSON을 Domain Entity로 변환한다.
class EventSalesInfoModel {
  final String eventId;
  final int targetAmount;
  final int achievedAmount;
  final double achievementRate;
  final double progressRate;

  const EventSalesInfoModel({
    required this.eventId,
    required this.targetAmount,
    required this.achievedAmount,
    required this.achievementRate,
    required this.progressRate,
  });

  factory EventSalesInfoModel.fromJson(Map<String, dynamic> json) {
    return EventSalesInfoModel(
      eventId: json['eventId'] as String,
      targetAmount: json['targetAmount'] as int,
      achievedAmount: json['achievedAmount'] as int,
      achievementRate: (json['achievementRate'] as num).toDouble(),
      progressRate: (json['progressRate'] as num).toDouble(),
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

  EventSalesInfo toEntity() {
    return EventSalesInfo(
      eventId: eventId,
      targetAmount: targetAmount,
      achievedAmount: achievedAmount,
      achievementRate: achievementRate,
      progressRate: progressRate,
    );
  }

  factory EventSalesInfoModel.fromEntity(EventSalesInfo entity) {
    return EventSalesInfoModel(
      eventId: entity.eventId,
      targetAmount: entity.targetAmount,
      achievedAmount: entity.achievedAmount,
      achievementRate: entity.achievementRate,
      progressRate: entity.progressRate,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EventSalesInfoModel &&
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
    return 'EventSalesInfoModel(eventId: $eventId, '
        'targetAmount: $targetAmount, achievedAmount: $achievedAmount, '
        'achievementRate: $achievementRate%, progressRate: $progressRate%)';
  }
}
