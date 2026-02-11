import '../../domain/entities/inspection_field_type.dart';

/// 현장 유형 모델 (DTO)
///
/// Backend API의 JSON을 파싱하여 InspectionFieldType 엔티티로 변환합니다.
class InspectionFieldTypeModel {
  final String code;
  final String name;

  const InspectionFieldTypeModel({
    required this.code,
    required this.name,
  });

  /// JSON에서 파싱
  factory InspectionFieldTypeModel.fromJson(Map<String, dynamic> json) {
    return InspectionFieldTypeModel(
      code: json['code'] as String,
      name: json['name'] as String,
    );
  }

  /// JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'code': code,
      'name': name,
    };
  }

  /// Domain Entity로 변환
  InspectionFieldType toEntity() {
    return InspectionFieldType(
      code: code,
      name: name,
    );
  }

  /// Domain Entity에서 생성
  factory InspectionFieldTypeModel.fromEntity(InspectionFieldType entity) {
    return InspectionFieldTypeModel(
      code: entity.code,
      name: entity.name,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is InspectionFieldTypeModel &&
        other.code == code &&
        other.name == name;
  }

  @override
  int get hashCode => Object.hash(code, name);

  @override
  String toString() {
    return 'InspectionFieldTypeModel(code: $code, name: $name)';
  }
}
