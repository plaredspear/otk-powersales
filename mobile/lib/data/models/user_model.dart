import '../../domain/entities/user.dart';

/// 사용자 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 User 엔티티로 변환합니다.
class UserModel {
  final int id;
  final String employeeId;
  final String name;
  final String department;
  final String branchName;
  final String role;

  const UserModel({
    required this.id,
    required this.employeeId,
    required this.name,
    required this.department,
    required this.branchName,
    required this.role,
  });

  /// snake_case JSON에서 파싱
  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as int,
      employeeId: json['employee_id'] as String,
      name: json['name'] as String,
      department: json['department'] as String,
      branchName: json['branch_name'] as String,
      role: json['role'] as String,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'employee_id': employeeId,
      'name': name,
      'department': department,
      'branch_name': branchName,
      'role': role,
    };
  }

  /// Domain Entity로 변환
  User toEntity() {
    return User(
      id: id,
      employeeId: employeeId,
      name: name,
      department: department,
      branchName: branchName,
      role: role,
    );
  }

  /// Domain Entity에서 생성
  factory UserModel.fromEntity(User entity) {
    return UserModel(
      id: entity.id,
      employeeId: entity.employeeId,
      name: entity.name,
      department: entity.department,
      branchName: entity.branchName,
      role: entity.role,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is UserModel &&
        other.id == id &&
        other.employeeId == employeeId &&
        other.name == name &&
        other.department == department &&
        other.branchName == branchName &&
        other.role == role;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      employeeId,
      name,
      department,
      branchName,
      role,
    );
  }

  @override
  String toString() {
    return 'UserModel(id: $id, employeeId: $employeeId, name: $name, department: $department, branchName: $branchName, role: $role)';
  }
}
