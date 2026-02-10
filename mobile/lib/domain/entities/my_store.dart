/// 내 거래처 엔티티
///
/// 한 달 일정에 등록된 거래처 정보를 담는 도메인 엔티티입니다.
/// 거래처명, 코드, 주소, 대표자, 전화번호 정보를 포함합니다.
class MyStore {
  /// 거래처 고유 ID
  final int storeId;

  /// 거래처명 (예: "(유)경산식품")
  final String storeName;

  /// 거래처 코드 (예: "1025172")
  final String storeCode;

  /// 주소
  final String address;

  /// 대표자명
  final String representativeName;

  /// 대표자 전화번호 (없을 수 있음)
  final String? phoneNumber;

  const MyStore({
    required this.storeId,
    required this.storeName,
    required this.storeCode,
    required this.address,
    required this.representativeName,
    this.phoneNumber,
  });

  MyStore copyWith({
    int? storeId,
    String? storeName,
    String? storeCode,
    String? address,
    String? representativeName,
    String? phoneNumber,
  }) {
    return MyStore(
      storeId: storeId ?? this.storeId,
      storeName: storeName ?? this.storeName,
      storeCode: storeCode ?? this.storeCode,
      address: address ?? this.address,
      representativeName: representativeName ?? this.representativeName,
      phoneNumber: phoneNumber ?? this.phoneNumber,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'storeId': storeId,
      'storeName': storeName,
      'storeCode': storeCode,
      'address': address,
      'representativeName': representativeName,
      'phoneNumber': phoneNumber,
    };
  }

  factory MyStore.fromJson(Map<String, dynamic> json) {
    return MyStore(
      storeId: json['storeId'] as int,
      storeName: json['storeName'] as String,
      storeCode: json['storeCode'] as String,
      address: json['address'] as String,
      representativeName: json['representativeName'] as String,
      phoneNumber: json['phoneNumber'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MyStore &&
        other.storeId == storeId &&
        other.storeName == storeName &&
        other.storeCode == storeCode &&
        other.address == address &&
        other.representativeName == representativeName &&
        other.phoneNumber == phoneNumber;
  }

  @override
  int get hashCode {
    return Object.hash(
      storeId,
      storeName,
      storeCode,
      address,
      representativeName,
      phoneNumber,
    );
  }

  @override
  String toString() {
    return 'MyStore(storeId: $storeId, storeName: $storeName, '
        'storeCode: $storeCode, address: $address, '
        'representativeName: $representativeName, '
        'phoneNumber: $phoneNumber)';
  }
}
