/// 현장 점검 테마 엔티티
///
/// 점검 등록 시 선택할 수 있는 테마 정보를 담는 도메인 엔티티입니다.
class InspectionTheme {
  /// 테마 ID
  final int id;

  /// 테마명
  final String name;

  /// 테마 시작일
  final DateTime startDate;

  /// 테마 종료일
  final DateTime endDate;

  const InspectionTheme({
    required this.id,
    required this.name,
    required this.startDate,
    required this.endDate,
  });

  /// 현재 유효한 테마인지 확인 (오늘 날짜가 기간 내에 포함되는지)
  bool get isValid {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    return (today.isAfter(startDate) || today.isAtSameMomentAs(startDate)) &&
        (today.isBefore(endDate) || today.isAtSameMomentAs(endDate));
  }

  /// 테마 기간 문자열 (YYYY.MM.DD ~ YYYY.MM.DD)
  String get periodString {
    final startStr = _formatDate(startDate);
    final endStr = _formatDate(endDate);
    return '$startStr ~ $endStr';
  }

  String _formatDate(DateTime date) {
    return '${date.year}.${date.month.toString().padLeft(2, '0')}.${date.day.toString().padLeft(2, '0')}';
  }

  InspectionTheme copyWith({
    int? id,
    String? name,
    DateTime? startDate,
    DateTime? endDate,
  }) {
    return InspectionTheme(
      id: id ?? this.id,
      name: name ?? this.name,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'startDate': startDate.toIso8601String().substring(0, 10),
      'endDate': endDate.toIso8601String().substring(0, 10),
    };
  }

  factory InspectionTheme.fromJson(Map<String, dynamic> json) {
    return InspectionTheme(
      id: json['id'] as int,
      name: json['name'] as String,
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! InspectionTheme) return false;
    return other.id == id &&
        other.name == name &&
        other.startDate == startDate &&
        other.endDate == endDate;
  }

  @override
  int get hashCode => Object.hash(id, name, startDate, endDate);

  @override
  String toString() {
    return 'InspectionTheme(id: $id, name: $name, '
        'startDate: $startDate, endDate: $endDate)';
  }
}
