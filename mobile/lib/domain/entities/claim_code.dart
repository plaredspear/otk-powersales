/// 클레임 관련 코드 및 타입 Entity
///
/// 클레임 등록 시 사용되는 공통 코드와 타입을 정의합니다.

/// 기한 종류 Enum
enum ClaimDateType {
  /// 유통기한
  expiryDate('EXPIRY_DATE', '유통기한'),

  /// 제조일자
  manufactureDate('MANUFACTURE_DATE', '제조일자');

  const ClaimDateType(this.code, this.displayName);

  final String code;
  final String displayName;

  /// JSON 직렬화
  String toJson() => code;

  /// JSON 역직렬화
  static ClaimDateType fromJson(String json) {
    return ClaimDateType.values.firstWhere(
      (type) => type.code == json,
      orElse: () => ClaimDateType.expiryDate,
    );
  }

  /// 코드로 ClaimDateType 찾기
  static ClaimDateType fromCode(String code) => fromJson(code);
}

/// 구매 방법 Entity
class PurchaseMethod {
  const PurchaseMethod({
    required this.code,
    required this.name,
  });

  final String code;
  final String name;

  /// JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'code': code,
      'name': name,
    };
  }

  /// JSON 역직렬화
  factory PurchaseMethod.fromJson(Map<String, dynamic> json) {
    return PurchaseMethod(
      code: json['code'] as String,
      name: json['name'] as String,
    );
  }

  /// copyWith
  PurchaseMethod copyWith({
    String? code,
    String? name,
  }) {
    return PurchaseMethod(
      code: code ?? this.code,
      name: name ?? this.name,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is PurchaseMethod && other.code == code && other.name == name;
  }

  @override
  int get hashCode => Object.hash(code, name);

  @override
  String toString() => 'PurchaseMethod(code: $code, name: $name)';
}

/// 요청사항 Entity
class ClaimRequestType {
  const ClaimRequestType({
    required this.code,
    required this.name,
  });

  final String code;
  final String name;

  /// JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'code': code,
      'name': name,
    };
  }

  /// JSON 역직렬화
  factory ClaimRequestType.fromJson(Map<String, dynamic> json) {
    return ClaimRequestType(
      code: json['code'] as String,
      name: json['name'] as String,
    );
  }

  /// copyWith
  ClaimRequestType copyWith({
    String? code,
    String? name,
  }) {
    return ClaimRequestType(
      code: code ?? this.code,
      name: name ?? this.name,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ClaimRequestType &&
        other.code == code &&
        other.name == name;
  }

  @override
  int get hashCode => Object.hash(code, name);

  @override
  String toString() => 'ClaimRequestType(code: $code, name: $name)';
}
