/// 안전점검 항목 엔티티
///
/// 안전점검 체크리스트의 개별 항목을 나타냅니다.
class SafetyCheckItem {
  /// 항목 ID
  final int id;

  /// 항목명 (화면 표시 텍스트)
  final String label;

  /// 표시 순서
  final int sortOrder;

  /// 필수 체크 여부
  final bool required;

  const SafetyCheckItem({
    required this.id,
    required this.label,
    required this.sortOrder,
    required this.required,
  });

  SafetyCheckItem copyWith({
    int? id,
    String? label,
    int? sortOrder,
    bool? required,
  }) {
    return SafetyCheckItem(
      id: id ?? this.id,
      label: label ?? this.label,
      sortOrder: sortOrder ?? this.sortOrder,
      required: required ?? this.required,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'label': label,
      'sortOrder': sortOrder,
      'required': required,
    };
  }

  factory SafetyCheckItem.fromJson(Map<String, dynamic> json) {
    return SafetyCheckItem(
      id: json['id'] as int,
      label: json['label'] as String,
      sortOrder: json['sortOrder'] as int,
      required: json['required'] as bool,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SafetyCheckItem &&
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
    return 'SafetyCheckItem(id: $id, label: $label, sortOrder: $sortOrder, required: $required)';
  }
}
