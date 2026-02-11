/// 현장 유형 엔티티
///
/// 점검 등록 시 선택할 수 있는 현장 유형 정보를 담는 도메인 엔티티입니다.
class InspectionFieldType {
  /// 유형 코드
  final String code;

  /// 유형명
  final String name;

  const InspectionFieldType({
    required this.code,
    required this.name,
  });

  InspectionFieldType copyWith({
    String? code,
    String? name,
  }) {
    return InspectionFieldType(
      code: code ?? this.code,
      name: name ?? this.name,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'code': code,
      'name': name,
    };
  }

  factory InspectionFieldType.fromJson(Map<String, dynamic> json) {
    return InspectionFieldType(
      code: json['code'] as String,
      name: json['name'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! InspectionFieldType) return false;
    return other.code == code && other.name == name;
  }

  @override
  int get hashCode => Object.hash(code, name);

  @override
  String toString() {
    return 'InspectionFieldType(code: $code, name: $name)';
  }
}
