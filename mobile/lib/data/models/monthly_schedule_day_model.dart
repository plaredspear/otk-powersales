import '../../domain/entities/monthly_schedule_day.dart';

/// 월간 일정 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 MonthlyScheduleDay 엔티티로 변환합니다.
class MonthlyScheduleDayModel {
  final DateTime date;
  final bool hasWork;
  final String? workingType;

  const MonthlyScheduleDayModel({
    required this.date,
    required this.hasWork,
    this.workingType,
  });

  /// snake_case JSON에서 파싱
  factory MonthlyScheduleDayModel.fromJson(Map<String, dynamic> json) {
    return MonthlyScheduleDayModel(
      date: DateTime.parse(json['date'] as String),
      hasWork: json['hasWork'] as bool,
      workingType: json['workingType'] as String?,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'date': date.toIso8601String().split('T')[0], // YYYY-MM-DD 형식
      'hasWork': hasWork,
      'workingType': workingType,
    };
  }

  /// Domain Entity로 변환
  MonthlyScheduleDay toEntity() {
    return MonthlyScheduleDay(
      date: date,
      hasWork: hasWork,
      workingType: workingType,
    );
  }

  /// Domain Entity에서 생성
  factory MonthlyScheduleDayModel.fromEntity(MonthlyScheduleDay entity) {
    return MonthlyScheduleDayModel(
      date: entity.date,
      hasWork: entity.hasWork,
      workingType: entity.workingType,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MonthlyScheduleDayModel &&
        other.date == date &&
        other.hasWork == hasWork &&
        other.workingType == workingType;
  }

  @override
  int get hashCode => Object.hash(date, hasWork, workingType);

  @override
  String toString() {
    return 'MonthlyScheduleDayModel(date: $date, hasWork: $hasWork, workingType: $workingType)';
  }
}
