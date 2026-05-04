import '../../domain/entities/user.dart';

/// 사용자 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 User 엔티티로 변환합니다.
class UserModel {
  final int id;
  final String employeeCode;
  final String name;
  final String? orgName;
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
      role: json['role'] as String,
    );
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

  /// Domain Entity로 변환
  User toEntity() {
    return User(
      id: id,
      employeeCode: employeeCode,
      name: name,
      orgName: orgName,
      role: role,
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
