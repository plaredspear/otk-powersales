/// 오늘 일정 엔티티
///
/// 홈화면에 표시되는 오늘의 스케줄 정보를 나타낸다.
class Schedule {
  final int id;
  final String storeName;
  final String startTime;
  final String endTime;
  final String type;

  const Schedule({
    required this.id,
    required this.storeName,
    required this.startTime,
    required this.endTime,
    required this.type,
  });

  Schedule copyWith({
    int? id,
    String? storeName,
    String? startTime,
    String? endTime,
    String? type,
  }) {
    return Schedule(
      id: id ?? this.id,
      storeName: storeName ?? this.storeName,
      startTime: startTime ?? this.startTime,
      endTime: endTime ?? this.endTime,
      type: type ?? this.type,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'storeName': storeName,
      'startTime': startTime,
      'endTime': endTime,
      'type': type,
    };
  }

  factory Schedule.fromJson(Map<String, dynamic> json) {
    return Schedule(
      id: json['id'] as int,
      storeName: json['storeName'] as String,
      startTime: json['startTime'] as String,
      endTime: json['endTime'] as String,
      type: json['type'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is Schedule &&
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
    return 'Schedule(id: $id, storeName: $storeName, startTime: $startTime, endTime: $endTime, type: $type)';
  }
}
