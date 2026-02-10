/// 유효성 에러 유형 열거형
///
/// 주문서 유효성 검증 시 발생할 수 있는 에러 유형들입니다.
enum ValidationErrorType {
  /// 최소 주문 수량 미달
  minOrderQuantity('MIN_ORDER_QUANTITY', '최소 주문 수량 미달'),

  /// 공급 수량 부족
  supplyQuantity('SUPPLY_QUANTITY', '공급 수량 부족'),

  /// DC 수량 부족
  dcQuantity('DC_QUANTITY', 'DC 수량 부족'),

  /// 여신 잔액 초과
  creditExceeded('CREDIT_EXCEEDED', '여신 잔액 초과');

  const ValidationErrorType(this.code, this.displayName);

  /// API 코드 값
  final String code;

  /// 화면에 표시되는 이름
  final String displayName;

  /// API 코드에서 ValidationErrorType으로 변환
  static ValidationErrorType fromCode(String code) {
    return ValidationErrorType.values.firstWhere(
      (type) => type.code == code,
      orElse: () => ValidationErrorType.minOrderQuantity,
    );
  }

  String toJson() => code;
  static ValidationErrorType fromJson(String json) => fromCode(json);
}

/// 유효성 에러 엔티티
///
/// 주문서 유효성 검증 결과에서 개별 제품의 에러 정보입니다.
class ValidationError {
  /// 에러 유형
  final ValidationErrorType errorType;

  /// 에러 메시지
  final String message;

  /// 최소 주문 수량 (박스)
  final int? minOrderQuantity;

  /// 공급 수량
  final int? supplyQuantity;

  /// DC 수량
  final int? dcQuantity;

  const ValidationError({
    required this.errorType,
    required this.message,
    this.minOrderQuantity,
    this.supplyQuantity,
    this.dcQuantity,
  });

  ValidationError copyWith({
    ValidationErrorType? errorType,
    String? message,
    int? minOrderQuantity,
    int? supplyQuantity,
    int? dcQuantity,
  }) {
    return ValidationError(
      errorType: errorType ?? this.errorType,
      message: message ?? this.message,
      minOrderQuantity: minOrderQuantity ?? this.minOrderQuantity,
      supplyQuantity: supplyQuantity ?? this.supplyQuantity,
      dcQuantity: dcQuantity ?? this.dcQuantity,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'errorType': errorType.code,
      'message': message,
      'minOrderQuantity': minOrderQuantity,
      'supplyQuantity': supplyQuantity,
      'dcQuantity': dcQuantity,
    };
  }

  factory ValidationError.fromJson(Map<String, dynamic> json) {
    return ValidationError(
      errorType: ValidationErrorType.fromCode(json['errorType'] as String),
      message: json['message'] as String,
      minOrderQuantity: json['minOrderQuantity'] as int?,
      supplyQuantity: json['supplyQuantity'] as int?,
      dcQuantity: json['dcQuantity'] as int?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ValidationError &&
        other.errorType == errorType &&
        other.message == message &&
        other.minOrderQuantity == minOrderQuantity &&
        other.supplyQuantity == supplyQuantity &&
        other.dcQuantity == dcQuantity;
  }

  @override
  int get hashCode {
    return Object.hash(
      errorType,
      message,
      minOrderQuantity,
      supplyQuantity,
      dcQuantity,
    );
  }

  @override
  String toString() {
    return 'ValidationError(errorType: $errorType, message: $message, '
        'minOrderQuantity: $minOrderQuantity, '
        'supplyQuantity: $supplyQuantity, dcQuantity: $dcQuantity)';
  }
}

/// 유효성 검증 결과 값 객체
///
/// 주문서 유효성 검증 API의 응답을 담는 값 객체입니다.
class ValidationResult {
  /// 유효 여부
  final bool isValid;

  /// 제품별 유효성 에러 목록 (productCode -> ValidationError)
  final Map<String, ValidationError> errors;

  const ValidationResult({
    required this.isValid,
    this.errors = const {},
  });

  /// 유효성 에러가 있는지 여부
  bool get hasErrors => errors.isNotEmpty;

  /// 특정 제품코드의 에러 조회
  ValidationError? getError(String productCode) => errors[productCode];

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ValidationResult) return false;
    if (other.isValid != isValid) return false;
    if (other.errors.length != errors.length) return false;
    for (final entry in errors.entries) {
      if (other.errors[entry.key] != entry.value) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      isValid,
      Object.hashAll(errors.entries.map((e) => Object.hash(e.key, e.value))),
    );
  }

  @override
  String toString() {
    return 'ValidationResult(isValid: $isValid, errors: ${errors.length})';
  }
}

/// 주문서 전송 결과 값 객체
///
/// 주문서 전송 API의 응답을 담는 값 객체입니다.
class OrderSubmitResult {
  /// 주문 ID
  final int orderId;

  /// 주문 요청번호
  final String orderRequestNumber;

  /// 주문 상태
  final String status;

  const OrderSubmitResult({
    required this.orderId,
    required this.orderRequestNumber,
    required this.status,
  });

  Map<String, dynamic> toJson() {
    return {
      'orderId': orderId,
      'orderRequestNumber': orderRequestNumber,
      'status': status,
    };
  }

  factory OrderSubmitResult.fromJson(Map<String, dynamic> json) {
    return OrderSubmitResult(
      orderId: json['orderId'] as int,
      orderRequestNumber: json['orderRequestNumber'] as String,
      status: json['status'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is OrderSubmitResult &&
        other.orderId == orderId &&
        other.orderRequestNumber == orderRequestNumber &&
        other.status == status;
  }

  @override
  int get hashCode {
    return Object.hash(orderId, orderRequestNumber, status);
  }

  @override
  String toString() {
    return 'OrderSubmitResult(orderId: $orderId, '
        'orderRequestNumber: $orderRequestNumber, status: $status)';
  }
}
