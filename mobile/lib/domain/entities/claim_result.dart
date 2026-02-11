/// 클레임 등록 결과 Entity
///
/// 클레임 등록 API 응답 데이터를 담습니다.
class ClaimRegisterResult {
  const ClaimRegisterResult({
    required this.id,
    required this.storeName,
    required this.storeId,
    required this.productName,
    required this.productCode,
    required this.createdAt,
  });

  /// 클레임 ID
  final int id;

  /// 거래처명
  final String storeName;

  /// 거래처 ID
  final int storeId;

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
      'storeName': storeName,
      'storeId': storeId,
      'productName': productName,
      'productCode': productCode,
      'createdAt': createdAt.toIso8601String(),
    };
  }

  /// JSON 역직렬화
  factory ClaimRegisterResult.fromJson(Map<String, dynamic> json) {
    return ClaimRegisterResult(
      id: json['id'] as int,
      storeName: json['storeName'] as String,
      storeId: json['storeId'] as int,
      productName: json['productName'] as String,
      productCode: json['productCode'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  /// copyWith
  ClaimRegisterResult copyWith({
    int? id,
    String? storeName,
    int? storeId,
    String? productName,
    String? productCode,
    DateTime? createdAt,
  }) {
    return ClaimRegisterResult(
      id: id ?? this.id,
      storeName: storeName ?? this.storeName,
      storeId: storeId ?? this.storeId,
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
        other.storeName == storeName &&
        other.storeId == storeId &&
        other.productName == productName &&
        other.productCode == productCode &&
        other.createdAt == createdAt;
  }

  @override
  int get hashCode => Object.hash(
        id,
        storeName,
        storeId,
        productName,
        productCode,
        createdAt,
      );

  @override
  String toString() {
    return 'ClaimRegisterResult('
        'id: $id, '
        'storeName: $storeName, '
        'storeId: $storeId, '
        'productName: $productName, '
        'productCode: $productCode, '
        'createdAt: $createdAt'
        ')';
  }
}
