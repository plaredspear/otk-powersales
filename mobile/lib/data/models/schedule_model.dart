import '../../domain/entities/schedule.dart';

/// Schedule API 모델 (DTO)
///
/// API 응답의 snake_case JSON을 Domain Entity로 변환한다.
class ScheduleModel {
  final int id;
  final String storeName;
  final String startTime;
  final String endTime;
  final String type;

  const ScheduleModel({
    required this.id,
    required this.storeName,
    required this.startTime,
    required this.endTime,
    required this.type,
  });

  factory ScheduleModel.fromJson(Map<String, dynamic> json) {
    return ScheduleModel(
      id: json['id'] as int,
      storeName: json['store_name'] as String,
      startTime: json['start_time'] as String,
      endTime: json['end_time'] as String,
      type: json['type'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'store_name': storeName,
      'start_time': startTime,
      'end_time': endTime,
      'type': type,
    };
  }

  Schedule toEntity() {
    return Schedule(
      id: id,
      storeName: storeName,
      startTime: startTime,
      endTime: endTime,
      type: type,
    );
  }

  factory ScheduleModel.fromEntity(Schedule entity) {
    return ScheduleModel(
      id: entity.id,
      storeName: entity.storeName,
      startTime: entity.startTime,
      endTime: entity.endTime,
      type: entity.type,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ScheduleModel &&
        other.id == id &&
        other.storeName == storeName &&
        other.startTime == startTime &&
        other.endTime == endTime &&
        other.type == type;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      storeName,
      startTime,
      endTime,
      type,
    );
  }

  @override
  String toString() {
    return 'ScheduleModel(id: $id, storeName: $storeName, startTime: $startTime, endTime: $endTime, type: $type)';
  }
}
