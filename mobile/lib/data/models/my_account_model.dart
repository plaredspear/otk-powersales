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
  final String? address;

  /// 상세주소
  final String? addressDetail;

  /// 대표자명
  final String? representativeName;

  /// 대표자 전화번호
  final String? phoneNumber;

  const MyAccountModel({
    required this.accountId,
    required this.accountName,
    required this.accountCode,
    this.address,
    this.addressDetail,
    this.representativeName,
    this.phoneNumber,
  });

  /// snake_case JSON에서 파싱
  factory MyAccountModel.fromJson(Map<String, dynamic> json) {
    return MyAccountModel(
      accountId: json['accountId'] as int,
      accountName: json['accountName'] as String,
      accountCode: json['accountCode'] as String,
      address: json['address'] as String?,
      addressDetail: json['addressDetail'] as String?,
      representativeName: json['representativeName'] as String?,
      phoneNumber: json['phoneNumber'] as String?,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'accountId': accountId,
      'accountName': accountName,
      'accountCode': accountCode,
      'address': address,
      'addressDetail': addressDetail,
      'representativeName': representativeName,
      'phoneNumber': phoneNumber,
    };
  }

  /// Domain Entity로 변환
  MyAccount toEntity() {
    return MyAccount(
      accountId: accountId,
      accountName: accountName,
      accountCode: accountCode,
      address: address,
      addressDetail: addressDetail,
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
      addressDetail: entity.addressDetail,
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
        other.addressDetail == addressDetail &&
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
      addressDetail,
      representativeName,
      phoneNumber,
    );
  }

  @override
  String toString() {
    return 'MyAccountModel(accountId: $accountId, accountName: $accountName, '
        'accountCode: $accountCode, address: $address, '
        'addressDetail: $addressDetail, '
        'representativeName: $representativeName, '
        'phoneNumber: $phoneNumber)';
  }
}
