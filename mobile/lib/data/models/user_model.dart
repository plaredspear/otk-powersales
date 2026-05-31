import '../../domain/entities/user.dart';

/// 사용자 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 User 엔티티로 변환합니다.
class UserModel {
  final int id;
  final String employeeCode;
  final String name;
  final String? orgName;

  /// Backend 원본 role 값 (SF `DKRetail__AppAuthority__c` picklist: `여사원`/`조장`/`지점장`/null).
  /// 도메인(`User.role`, `USER`/`LEADER`/`ADMIN`)으로는 [toEntity]에서 [_toDomainRole]로 번역한다.
  final String role;

  const UserModel({
    required this.id,
    required this.employeeCode,
    required this.name,
    this.orgName,
    required this.role,
  });

  /// snake_case JSON에서 파싱
  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as int,
      employeeCode: json['employeeCode'] as String,
      name: json['name'] as String,
      orgName: json['orgName'] as String?,
      // role 은 nullable (ADMIN 부트스트랩 등). null/공백은 기본 사원으로 처리.
      role: (json['role'] as String?) ?? '',
    );
  }

  /// Backend SF picklist role → 도메인 role(`USER`/`LEADER`/`ADMIN`) 번역.
  ///
  /// Backend `LoginResponse.UserInfo.role`은 SF picklist value(`여사원`/`조장`/`지점장`)를
  /// 그대로 내려주므로, 모바일이 사용하는 role 어휘로 매핑한다.
  /// 이미 도메인 값(영문)이거나 미지정/그 외 권한은 기본 사원(`USER`)으로 처리한다.
  static String _toDomainRole(String raw) {
    switch (raw) {
      case '조장':
      case 'LEADER':
        return 'LEADER';
      case '지점장':
      case 'ADMIN':
        return 'ADMIN';
      case '여사원':
      case 'USER':
        return 'USER';
      default:
        return 'USER';
    }
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'employeeCode': employeeCode,
      'name': name,
      'orgName': orgName,
      'role': role,
    };
  }

  /// Domain Entity로 변환 (role 은 도메인 어휘로 번역)
  User toEntity() {
    return User(
      id: id,
      employeeCode: employeeCode,
      name: name,
      orgName: orgName,
      role: _toDomainRole(role),
    );
  }

  /// Domain Entity에서 생성
  factory UserModel.fromEntity(User entity) {
    return UserModel(
      id: entity.id,
      employeeCode: entity.employeeCode,
      name: entity.name,
      orgName: entity.orgName,
      role: entity.role,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is UserModel &&
        other.id == id &&
        other.employeeCode == employeeCode &&
        other.name == name &&
        other.orgName == orgName &&
        other.role == role;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      employeeCode,
      name,
      orgName,
      role,
    );
  }

  @override
  String toString() {
    return 'UserModel(id: $id, employeeCode: $employeeCode, name: $name, orgName: $orgName, role: $role)';
  }
}
