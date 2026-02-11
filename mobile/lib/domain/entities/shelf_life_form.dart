/// 유통기한 등록 폼
///
/// 유통기한 신규 등록 시 입력 데이터를 담는 값 객체입니다.
class ShelfLifeRegisterForm {
  /// 거래처 ID
  final int storeId;

  /// 제품 코드
  final String productCode;

  /// 유통기한
  final DateTime expiryDate;

  /// 마감 전 알림 날짜
  final DateTime alertDate;

  /// 설명 (선택)
  final String description;

  const ShelfLifeRegisterForm({
    required this.storeId,
    required this.productCode,
    required this.expiryDate,
    required this.alertDate,
    this.description = '',
  });

  ShelfLifeRegisterForm copyWith({
    int? storeId,
    String? productCode,
    DateTime? expiryDate,
    DateTime? alertDate,
    String? description,
  }) {
    return ShelfLifeRegisterForm(
      storeId: storeId ?? this.storeId,
      productCode: productCode ?? this.productCode,
      expiryDate: expiryDate ?? this.expiryDate,
      alertDate: alertDate ?? this.alertDate,
      description: description ?? this.description,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'storeId': storeId,
      'productCode': productCode,
      'expiryDate': expiryDate.toIso8601String().substring(0, 10),
      'alertDate': alertDate.toIso8601String().substring(0, 10),
      'description': description,
    };
  }

  /// 필수 필드가 모두 입력되었는지 검증
  bool get isValid {
    return storeId > 0 && productCode.isNotEmpty;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ShelfLifeRegisterForm) return false;
    return other.storeId == storeId &&
        other.productCode == productCode &&
        other.expiryDate == expiryDate &&
        other.alertDate == alertDate &&
        other.description == description;
  }

  @override
  int get hashCode {
    return Object.hash(
      storeId,
      productCode,
      expiryDate,
      alertDate,
      description,
    );
  }

  @override
  String toString() {
    return 'ShelfLifeRegisterForm(storeId: $storeId, '
        'productCode: $productCode, expiryDate: $expiryDate, '
        'alertDate: $alertDate, description: $description)';
  }
}

/// 유통기한 수정 폼
///
/// 유통기한 수정 시 입력 데이터를 담는 값 객체입니다.
/// 거래처와 제품은 수정 불가하므로 포함하지 않습니다.
class ShelfLifeUpdateForm {
  /// 유통기한
  final DateTime expiryDate;

  /// 마감 전 알림 날짜
  final DateTime alertDate;

  /// 설명 (선택)
  final String description;

  const ShelfLifeUpdateForm({
    required this.expiryDate,
    required this.alertDate,
    this.description = '',
  });

  ShelfLifeUpdateForm copyWith({
    DateTime? expiryDate,
    DateTime? alertDate,
    String? description,
  }) {
    return ShelfLifeUpdateForm(
      expiryDate: expiryDate ?? this.expiryDate,
      alertDate: alertDate ?? this.alertDate,
      description: description ?? this.description,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'expiryDate': expiryDate.toIso8601String().substring(0, 10),
      'alertDate': alertDate.toIso8601String().substring(0, 10),
      'description': description,
    };
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ShelfLifeUpdateForm) return false;
    return other.expiryDate == expiryDate &&
        other.alertDate == alertDate &&
        other.description == description;
  }

  @override
  int get hashCode => Object.hash(expiryDate, alertDate, description);

  @override
  String toString() {
    return 'ShelfLifeUpdateForm(expiryDate: $expiryDate, '
        'alertDate: $alertDate, description: $description)';
  }
}
