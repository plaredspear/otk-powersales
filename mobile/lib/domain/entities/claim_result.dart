/// 클레임 등록 결과 Entity
///
/// 클레임 등록 API 응답 데이터를 담습니다.
class ClaimRegisterResult {
  const ClaimRegisterResult({
    required this.id,
    required this.accountName,
    required this.accountId,
    required this.productName,
    required this.productCode,
    required this.createdAt,
  });

  /// 클레임 ID
  final int id;

  /// 거래처명
  final String accountName;

  /// 거래처 ID
  final int accountId;

  /// 제품명
  final String productName;

  /// 제품 코드
  final String productCode;

  /// 등록 일시
  final DateTime createdAt;

  /// JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'accountName': accountName,
      'accountId': accountId,
      'productName': productName,
      'productCode': productCode,
      'createdAt': createdAt.toIso8601String(),
    };
  }

  /// JSON 역직렬화
  factory ClaimRegisterResult.fromJson(Map<String, dynamic> json) {
    return ClaimRegisterResult(
      id: json['id'] as int,
      accountName: json['accountName'] as String,
      accountId: json['accountId'] as int,
      productName: json['productName'] as String,
      productCode: json['productCode'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  /// copyWith
  ClaimRegisterResult copyWith({
    int? id,
    String? accountName,
    int? accountId,
    String? productName,
    String? productCode,
    DateTime? createdAt,
  }) {
    return ClaimRegisterResult(
      id: id ?? this.id,
      accountName: accountName ?? this.accountName,
      accountId: accountId ?? this.accountId,
      productName: productName ?? this.productName,
      productCode: productCode ?? this.productCode,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ClaimRegisterResult &&
        other.id == id &&
        other.accountName == accountName &&
        other.accountId == accountId &&
        other.productName == productName &&
        other.productCode == productCode &&
        other.createdAt == createdAt;
  }

  @override
  int get hashCode => Object.hash(
        id,
        accountName,
        accountId,
        productName,
        productCode,
        createdAt,
      );

  @override
  String toString() {
    return 'ClaimRegisterResult('
        'id: $id, '
        'accountName: $accountName, '
        'accountId: $accountId, '
        'productName: $productName, '
        'productCode: $productCode, '
        'createdAt: $createdAt'
        ')';
  }
}
