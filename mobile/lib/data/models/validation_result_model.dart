import '../../domain/entities/validation_error.dart';

/// 유효성 에러 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 ValidationError 엔티티로 변환합니다.
class ValidationErrorModel {
  final String errorType;
  final String message;
  final int? minOrderQuantity;
  final int? supplyQuantity;
  final int? dcQuantity;

  const ValidationErrorModel({
    required this.errorType,
    required this.message,
    this.minOrderQuantity,
    this.supplyQuantity,
    this.dcQuantity,
  });

  /// snake_case JSON에서 파싱
  factory ValidationErrorModel.fromJson(Map<String, dynamic> json) {
    return ValidationErrorModel(
      errorType: json['error_type'] as String,
      message: json['message'] as String,
      minOrderQuantity: json['min_order_quantity'] as int?,
      supplyQuantity: json['supply_quantity'] as int?,
      dcQuantity: json['dc_quantity'] as int?,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'error_type': errorType,
      'message': message,
      'min_order_quantity': minOrderQuantity,
      'supply_quantity': supplyQuantity,
      'dc_quantity': dcQuantity,
    };
  }

  /// Domain Entity로 변환
  ValidationError toEntity() {
    return ValidationError(
      errorType: ValidationErrorType.fromCode(errorType),
      message: message,
      minOrderQuantity: minOrderQuantity,
      supplyQuantity: supplyQuantity,
      dcQuantity: dcQuantity,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ValidationErrorModel &&
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
    return 'ValidationErrorModel(errorType: $errorType, message: $message, '
        'minOrderQuantity: $minOrderQuantity, supplyQuantity: $supplyQuantity, '
        'dcQuantity: $dcQuantity)';
  }
}

/// 유효성 검증 결과 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 ValidationResult 값 객체로 변환합니다.
/// API 응답 예시:
/// ```json
/// {
///   "success": true,
///   "valid": false,
///   "errors": [
///     {
///       "product_code": "01234567",
///       "error_type": "MIN_ORDER_QUANTITY",
///       "message": "최소 주문 수량은 10박스입니다",
///       "min_order_quantity": 10
///     }
///   ]
/// }
/// ```
class ValidationResultModel {
  final bool isValid;
  final List<ValidationErrorWithProductModel> errors;

  const ValidationResultModel({
    required this.isValid,
    this.errors = const [],
  });

  /// snake_case JSON에서 파싱
  factory ValidationResultModel.fromJson(Map<String, dynamic> json) {
    // API 응답이 { "data": { ... } } 또는 직접 형태일 수 있음
    final data = json.containsKey('data')
        ? json['data'] as Map<String, dynamic>
        : json;

    final isValid = data['valid'] as bool? ?? true;
    final errorsJson = data['errors'] as List<dynamic>? ?? [];

    return ValidationResultModel(
      isValid: isValid,
      errors: errorsJson
          .map((e) => ValidationErrorWithProductModel.fromJson(
              e as Map<String, dynamic>))
          .toList(),
    );
  }

  /// Domain Entity로 변환
  ValidationResult toEntity() {
    final errorMap = <String, ValidationError>{};
    for (final error in errors) {
      errorMap[error.productCode] = error.error.toEntity();
    }

    return ValidationResult(
      isValid: isValid,
      errors: errorMap,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ValidationResultModel) return false;
    if (other.isValid != isValid) return false;
    if (other.errors.length != errors.length) return false;
    for (var i = 0; i < errors.length; i++) {
      if (other.errors[i] != errors[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      isValid,
      Object.hashAll(errors),
    );
  }

  @override
  String toString() {
    return 'ValidationResultModel(isValid: $isValid, errors: ${errors.length})';
  }
}

/// 제품 코드와 함께 묶인 유효성 에러 모델
///
/// API 응답에서 errors 배열의 각 항목은 product_code를 포함합니다.
class ValidationErrorWithProductModel {
  final String productCode;
  final ValidationErrorModel error;

  const ValidationErrorWithProductModel({
    required this.productCode,
    required this.error,
  });

  factory ValidationErrorWithProductModel.fromJson(Map<String, dynamic> json) {
    return ValidationErrorWithProductModel(
      productCode: json['product_code'] as String,
      error: ValidationErrorModel(
        errorType: json['error_type'] as String,
        message: json['message'] as String,
        minOrderQuantity: json['min_order_quantity'] as int?,
        supplyQuantity: json['supply_quantity'] as int?,
        dcQuantity: json['dc_quantity'] as int?,
      ),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ValidationErrorWithProductModel &&
        other.productCode == productCode &&
        other.error == error;
  }

  @override
  int get hashCode => Object.hash(productCode, error);

  @override
  String toString() {
    return 'ValidationErrorWithProductModel(productCode: $productCode, '
        'error: $error)';
  }
}

/// 주문서 전송 결과 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 OrderSubmitResult 값 객체로 변환합니다.
/// API 응답 예시:
/// ```json
/// {
///   "success": true,
///   "data": {
///     "order_id": 123,
///     "order_request_number": "OP00000074",
///     "status": "PENDING"
///   }
/// }
/// ```
class OrderSubmitResultModel {
  final int orderId;
  final String orderRequestNumber;
  final String status;

  const OrderSubmitResultModel({
    required this.orderId,
    required this.orderRequestNumber,
    required this.status,
  });

  /// snake_case JSON에서 파싱
  factory OrderSubmitResultModel.fromJson(Map<String, dynamic> json) {
    // API 응답이 { "data": { ... } } 형태로 감싸져 있음
    final data = json.containsKey('data')
        ? json['data'] as Map<String, dynamic>
        : json;

    return OrderSubmitResultModel(
      orderId: data['order_id'] as int,
      orderRequestNumber: data['order_request_number'] as String,
      status: data['status'] as String,
    );
  }

  /// Domain Entity로 변환
  OrderSubmitResult toEntity() {
    return OrderSubmitResult(
      orderId: orderId,
      orderRequestNumber: orderRequestNumber,
      status: status,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is OrderSubmitResultModel &&
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
    return 'OrderSubmitResultModel(orderId: $orderId, '
        'orderRequestNumber: $orderRequestNumber, status: $status)';
  }
}
