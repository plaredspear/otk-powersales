import '../../domain/entities/notice.dart';

/// Notice API 모델 (DTO)
///
/// API 응답의 snake_case JSON을 Domain Entity로 변환한다.
class NoticeModel {
  final int id;
  final String title;
  final String type;
  final DateTime createdAt;

  const NoticeModel({
    required this.id,
    required this.title,
    required this.type,
    required this.createdAt,
  });

  factory NoticeModel.fromJson(Map<String, dynamic> json) {
    return NoticeModel(
      id: json['id'] as int,
      title: json['title'] as String,
      type: json['type'] as String,
      createdAt: DateTime.parse(json['created_at'] as String),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'type': type,
      'created_at': createdAt.toIso8601String(),
    };
  }

  Notice toEntity() {
    return Notice(
      id: id,
      title: title,
      type: type,
      createdAt: createdAt,
    );
  }

  factory NoticeModel.fromEntity(Notice entity) {
    return NoticeModel(
      id: entity.id,
      title: entity.title,
      type: entity.type,
      createdAt: entity.createdAt,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is NoticeModel &&
        other.id == id &&
        other.title == title &&
        other.type == type &&
        other.createdAt == createdAt;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      title,
      type,
      createdAt,
    );
  }

  @override
  String toString() {
    return 'NoticeModel(id: $id, title: $title, type: $type, createdAt: $createdAt)';
  }
}
