import '../../domain/entities/claim_code.dart';

/// 구매 방법 Model
class PurchaseMethodModel {
  const PurchaseMethodModel({
    required this.code,
    required this.name,
  });

  final String code;
  final String name;

  /// JSON 역직렬화
  factory PurchaseMethodModel.fromJson(Map<String, dynamic> json) {
    return PurchaseMethodModel(
      code: json['code'] as String,
      name: json['name'] as String,
    );
  }

  /// JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'code': code,
      'name': name,
    };
  }

  /// Entity로 변환
  PurchaseMethod toEntity() {
    return PurchaseMethod(
      code: code,
      name: name,
    );
  }

  /// Entity에서 변환
  factory PurchaseMethodModel.fromEntity(PurchaseMethod entity) {
    return PurchaseMethodModel(
      code: entity.code,
      name: entity.name,
    );
  }
}

/// 요청사항 Model
class ClaimRequestTypeModel {
  const ClaimRequestTypeModel({
    required this.code,
    required this.name,
  });

  final String code;
  final String name;

  /// JSON 역직렬화
  factory ClaimRequestTypeModel.fromJson(Map<String, dynamic> json) {
    return ClaimRequestTypeModel(
      code: json['code'] as String,
      name: json['name'] as String,
    );
  }

  /// JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'code': code,
      'name': name,
    };
  }

  /// Entity로 변환
  ClaimRequestType toEntity() {
    return ClaimRequestType(
      code: code,
      name: name,
    );
  }

  /// Entity에서 변환
  factory ClaimRequestTypeModel.fromEntity(ClaimRequestType entity) {
    return ClaimRequestTypeModel(
      code: entity.code,
      name: entity.name,
    );
  }
}
