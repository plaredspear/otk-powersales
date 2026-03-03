/// 유통기한 등록 폼
///
/// 유통기한 신규 등록 시 입력 데이터를 담는 값 객체입니다.
class ShelfLifeRegisterForm {
  /// 거래처 코드
  final String accountCode;

  /// 거래처명
  final String accountName;

  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 유통기한
  final DateTime expiryDate;

  /// 마감 전 알림 날짜
  final DateTime alertDate;

  /// 설명 (선택)
  final String description;

  const ShelfLifeRegisterForm({
    required this.accountCode,
    required this.accountName,
    required this.productCode,
    required this.productName,
    required this.expiryDate,
    required this.alertDate,
    this.description = '',
  });

  ShelfLifeRegisterForm copyWith({
    String? accountCode,
    String? accountName,
    String? productCode,
    String? productName,
    DateTime? expiryDate,
    DateTime? alertDate,
    String? description,
  }) {
    return ShelfLifeRegisterForm(
      accountCode: accountCode ?? this.accountCode,
      accountName: accountName ?? this.accountName,
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      expiryDate: expiryDate ?? this.expiryDate,
      alertDate: alertDate ?? this.alertDate,
      description: description ?? this.description,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'accountCode': accountCode,
      'accountName': accountName,
      'productCode': productCode,
      'productName': productName,
      'expiryDate': expiryDate.toIso8601String().substring(0, 10),
      'alertDate': alertDate.toIso8601String().substring(0, 10),
      'description': description,
    };
  }

  /// 필수 필드가 모두 입력되었는지 검증
  bool get isValid {
    return accountCode.isNotEmpty && productCode.isNotEmpty;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ShelfLifeRegisterForm) return false;
    return other.accountCode == accountCode &&
        other.accountName == accountName &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.expiryDate == expiryDate &&
        other.alertDate == alertDate &&
        other.description == description;
  }

  @override
  int get hashCode {
    return Object.hash(
      accountCode,
      accountName,
      productCode,
      productName,
      expiryDate,
      alertDate,
      description,
    );
  }

  @override
  String toString() {
    return 'ShelfLifeRegisterForm(accountCode: $accountCode, '
        'accountName: $accountName, productCode: $productCode, '
        'productName: $productName, expiryDate: $expiryDate, '
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
