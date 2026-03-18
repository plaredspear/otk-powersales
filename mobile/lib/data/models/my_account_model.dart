import '../../domain/entities/my_account.dart';

/// 내 거래처 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 MyAccount 엔티티로 변환합니다.
class MyAccountModel {
  /// 거래처 고유 ID
  final int accountId;

  /// 거래처명
  final String accountName;

  /// 거래처 코드
  final String accountCode;

  /// 주소
  final String address;

  /// 대표자명
  final String representativeName;

  /// 대표자 전화번호
  final String? phoneNumber;

  const MyAccountModel({
    required this.accountId,
    required this.accountName,
    required this.accountCode,
    required this.address,
    required this.representativeName,
    this.phoneNumber,
  });

  /// snake_case JSON에서 파싱
  factory MyAccountModel.fromJson(Map<String, dynamic> json) {
    return MyAccountModel(
      accountId: json['account_id'] as int,
      accountName: json['account_name'] as String,
      accountCode: json['account_code'] as String,
      address: json['address'] as String,
      representativeName: json['representative_name'] as String,
      phoneNumber: json['phone_number'] as String?,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'account_id': accountId,
      'account_name': accountName,
      'account_code': accountCode,
      'address': address,
      'representative_name': representativeName,
      'phone_number': phoneNumber,
    };
  }

  /// Domain Entity로 변환
  MyAccount toEntity() {
    return MyAccount(
      accountId: accountId,
      accountName: accountName,
      accountCode: accountCode,
      address: address,
      representativeName: representativeName,
      phoneNumber: phoneNumber,
    );
  }

  /// Domain Entity에서 생성
  factory MyAccountModel.fromEntity(MyAccount entity) {
    return MyAccountModel(
      accountId: entity.accountId,
      accountName: entity.accountName,
      accountCode: entity.accountCode,
      address: entity.address,
      representativeName: entity.representativeName,
      phoneNumber: entity.phoneNumber,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MyAccountModel &&
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
    return 'MyAccountModel(accountId: $accountId, accountName: $accountName, '
        'accountCode: $accountCode, address: $address, '
        'representativeName: $representativeName, '
        'phoneNumber: $phoneNumber)';
  }
}
