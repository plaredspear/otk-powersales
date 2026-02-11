import '../../domain/entities/inspection_theme.dart';

/// 현장 점검 테마 모델 (DTO)
///
/// Backend API의 JSON을 파싱하여 InspectionTheme 엔티티로 변환합니다.
class InspectionThemeModel {
  final int id;
  final String name;
  final String startDate;
  final String endDate;

  const InspectionThemeModel({
    required this.id,
    required this.name,
    required this.startDate,
    required this.endDate,
  });

  /// JSON에서 파싱
  factory InspectionThemeModel.fromJson(Map<String, dynamic> json) {
    return InspectionThemeModel(
      id: json['id'] as int,
      name: json['name'] as String,
      startDate: json['startDate'] as String,
      endDate: json['endDate'] as String,
    );
  }

  /// JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'startDate': startDate,
      'endDate': endDate,
    };
  }

  /// Domain Entity로 변환
  InspectionTheme toEntity() {
    return InspectionTheme(
      id: id,
      name: name,
      startDate: DateTime.parse(startDate),
      endDate: DateTime.parse(endDate),
    );
  }

  /// Domain Entity에서 생성
  factory InspectionThemeModel.fromEntity(InspectionTheme entity) {
    return InspectionThemeModel(
      id: entity.id,
      name: entity.name,
      startDate: entity.startDate.toIso8601String().substring(0, 10),
      endDate: entity.endDate.toIso8601String().substring(0, 10),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is InspectionThemeModel &&
        other.id == id &&
        other.name == name &&
        other.startDate == startDate &&
        other.endDate == endDate;
  }

  @override
  int get hashCode => Object.hash(id, name, startDate, endDate);

  @override
  String toString() {
    return 'InspectionThemeModel(id: $id, name: $name, '
        'startDate: $startDate, endDate: $endDate)';
  }
}
