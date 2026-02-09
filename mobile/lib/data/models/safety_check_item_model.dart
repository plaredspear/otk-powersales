import '../../domain/entities/safety_check_item.dart';

/// 안전점검 항목 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 SafetyCheckItem 엔티티로 변환합니다.
class SafetyCheckItemModel {
  final int id;
  final String label;
  final int sortOrder;
  final bool required;

  const SafetyCheckItemModel({
    required this.id,
    required this.label,
    required this.sortOrder,
    required this.required,
  });

  /// snake_case JSON에서 파싱
  factory SafetyCheckItemModel.fromJson(Map<String, dynamic> json) {
    return SafetyCheckItemModel(
      id: json['id'] as int,
      label: json['label'] as String,
      sortOrder: json['sort_order'] as int,
      required: json['required'] as bool,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'label': label,
      'sort_order': sortOrder,
      'required': required,
    };
  }

  /// Domain Entity로 변환
  SafetyCheckItem toEntity() {
    return SafetyCheckItem(
      id: id,
      label: label,
      sortOrder: sortOrder,
      required: required,
    );
  }

  /// Domain Entity에서 생성
  factory SafetyCheckItemModel.fromEntity(SafetyCheckItem entity) {
    return SafetyCheckItemModel(
      id: entity.id,
      label: entity.label,
      sortOrder: entity.sortOrder,
      required: entity.required,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SafetyCheckItemModel &&
        other.id == id &&
        other.label == label &&
        other.sortOrder == sortOrder &&
        other.required == required;
  }

  @override
  int get hashCode {
    return Object.hash(id, label, sortOrder, required);
  }

  @override
  String toString() {
    return 'SafetyCheckItemModel(id: $id, label: $label, sortOrder: $sortOrder, required: $required)';
  }
}
