/// 공지사항 엔티티
///
/// 홈화면에 표시되는 공지사항 정보를 나타낸다.
class Notice {
  final int id;
  final String title;
  final String type;
  final DateTime createdAt;

  const Notice({
    required this.id,
    required this.title,
    required this.type,
    required this.createdAt,
  });

  /// 공지 유형 표시 텍스트
  String get typeDisplayName {
    switch (type) {
      case 'BRANCH':
        return '지점공지';
      case 'ALL':
        return '전체공지';
      default:
        return type;
    }
  }

  Notice copyWith({
    int? id,
    String? title,
    String? type,
    DateTime? createdAt,
  }) {
    return Notice(
      id: id ?? this.id,
      title: title ?? this.title,
      type: type ?? this.type,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'type': type,
      'createdAt': createdAt.toIso8601String(),
    };
  }

  factory Notice.fromJson(Map<String, dynamic> json) {
    return Notice(
      id: json['id'] as int,
      title: json['title'] as String,
      type: json['type'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is Notice &&
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
    return 'Notice(id: $id, title: $title, type: $type, createdAt: $createdAt)';
  }
}
