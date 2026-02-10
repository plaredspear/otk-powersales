import '../../domain/entities/my_store.dart';

/// 내 거래처 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 MyStore 엔티티로 변환합니다.
class MyStoreModel {
  /// 거래처 고유 ID
  final int storeId;

  /// 거래처명
  final String storeName;

  /// 거래처 코드
  final String storeCode;

  /// 주소
  final String address;

  /// 대표자명
  final String representativeName;

  /// 대표자 전화번호
  final String? phoneNumber;

  const MyStoreModel({
    required this.storeId,
    required this.storeName,
    required this.storeCode,
    required this.address,
    required this.representativeName,
    this.phoneNumber,
  });

  /// snake_case JSON에서 파싱
  factory MyStoreModel.fromJson(Map<String, dynamic> json) {
    return MyStoreModel(
      storeId: json['store_id'] as int,
      storeName: json['store_name'] as String,
      storeCode: json['store_code'] as String,
      address: json['address'] as String,
      representativeName: json['representative_name'] as String,
      phoneNumber: json['phone_number'] as String?,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'store_id': storeId,
      'store_name': storeName,
      'store_code': storeCode,
      'address': address,
      'representative_name': representativeName,
      'phone_number': phoneNumber,
    };
  }

  /// Domain Entity로 변환
  MyStore toEntity() {
    return MyStore(
      storeId: storeId,
      storeName: storeName,
      storeCode: storeCode,
      address: address,
      representativeName: representativeName,
      phoneNumber: phoneNumber,
    );
  }

  /// Domain Entity에서 생성
  factory MyStoreModel.fromEntity(MyStore entity) {
    return MyStoreModel(
      storeId: entity.storeId,
      storeName: entity.storeName,
      storeCode: entity.storeCode,
      address: entity.address,
      representativeName: entity.representativeName,
      phoneNumber: entity.phoneNumber,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MyStoreModel &&
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
    return 'MyStoreModel(storeId: $storeId, storeName: $storeName, '
        'storeCode: $storeCode, address: $address, '
        'representativeName: $representativeName, '
        'phoneNumber: $phoneNumber)';
  }
}
