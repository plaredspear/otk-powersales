import '../../domain/entities/shelf_life_form.dart';

/// 유통기한 수정 API 요청 모델
///
/// PUT /api/v1/shelf-life/{id} 요청 바디를 snake_case JSON으로 직렬화합니다.
class ShelfLifeUpdateRequest {
  final String expiryDate;
  final String alertDate;
  final String description;

  const ShelfLifeUpdateRequest({
    required this.expiryDate,
    required this.alertDate,
    this.description = '',
  });

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'expiry_date': expiryDate,
      'alert_date': alertDate,
      'description': description,
    };
  }

  /// Domain Form에서 생성
  factory ShelfLifeUpdateRequest.fromForm(ShelfLifeUpdateForm form) {
    return ShelfLifeUpdateRequest(
      expiryDate: form.expiryDate.toIso8601String().substring(0, 10),
      alertDate: form.alertDate.toIso8601String().substring(0, 10),
      description: form.description,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ShelfLifeUpdateRequest) return false;
    return other.expiryDate == expiryDate &&
        other.alertDate == alertDate &&
        other.description == description;
  }

  @override
  int get hashCode => Object.hash(expiryDate, alertDate, description);

  @override
  String toString() {
    return 'ShelfLifeUpdateRequest(expiryDate: $expiryDate, '
        'alertDate: $alertDate, description: $description)';
  }
}
