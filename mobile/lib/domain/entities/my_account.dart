/// 내 거래처 엔티티
///
/// 한 달 일정에 등록된 거래처 정보를 담는 도메인 엔티티입니다.
/// 거래처명, 코드, 주소, 대표자, 전화번호 정보를 포함합니다.
class MyAccount {
  /// 거래처 고유 ID
  final int accountId;

  /// 거래처명 (예: "(유)경산식품")
  final String accountName;

  /// 거래처 코드 (예: "1025172")
  final String accountCode;

  /// 주소
  final String address;

  /// 대표자명
  final String representativeName;

  /// 대표자 전화번호 (없을 수 있음)
  final String? phoneNumber;

  const MyAccount({
    required this.accountId,
    required this.accountName,
    required this.accountCode,
    required this.address,
    required this.representativeName,
    this.phoneNumber,
  });

  MyAccount copyWith({
    int? accountId,
    String? accountName,
    String? accountCode,
    String? address,
    String? representativeName,
    String? phoneNumber,
  }) {
    return MyAccount(
      accountId: accountId ?? this.accountId,
      accountName: accountName ?? this.accountName,
      accountCode: accountCode ?? this.accountCode,
      address: address ?? this.address,
      representativeName: representativeName ?? this.representativeName,
      phoneNumber: phoneNumber ?? this.phoneNumber,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'accountId': accountId,
      'accountName': accountName,
      'accountCode': accountCode,
      'address': address,
      'representativeName': representativeName,
      'phoneNumber': phoneNumber,
    };
  }

  factory MyAccount.fromJson(Map<String, dynamic> json) {
    return MyAccount(
      accountId: json['accountId'] as int,
      accountName: json['accountName'] as String,
      accountCode: json['accountCode'] as String,
      address: json['address'] as String,
      representativeName: json['representativeName'] as String,
      phoneNumber: json['phoneNumber'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MyAccount &&
        other.accountId == accountId &&
        other.accountName == accountName &&
        other.accountCode == accountCode &&
        other.address == address &&
        other.representativeName == representativeName &&
        other.phoneNumber == phoneNumber;
  }

  @override
  int get hashCode {
    return Object.hash(
      accountId,
      accountName,
      accountCode,
      address,
      representativeName,
      phoneNumber,
    );
  }

  @override
  String toString() {
    return 'MyAccount(accountId: $accountId, accountName: $accountName, '
        'accountCode: $accountCode, address: $address, '
        'representativeName: $representativeName, '
        'phoneNumber: $phoneNumber)';
  }
}
