/// 사용자 엔티티
/// 인증 및 사용자 정보를 담는 도메인 엔티티
class User {
  /// 사용자 ID
  final int id;

  /// 사번 (8자리 숫자)
  final String employeeId;

  /// 이름
  final String name;

  /// 부서
  final String department;

  /// 소속 지점명
  final String branchName;

  /// 권한 (USER, LEADER, ADMIN)
  final String role;

  const User({
    required this.id,
    required this.employeeId,
    required this.name,
    required this.department,
    required this.branchName,
    required this.role,
  });

  User copyWith({
    int? id,
    String? employeeId,
    String? name,
    String? department,
    String? branchName,
    String? role,
  }) {
    return User(
      id: id ?? this.id,
      employeeId: employeeId ?? this.employeeId,
      name: name ?? this.name,
      department: department ?? this.department,
      branchName: branchName ?? this.branchName,
      role: role ?? this.role,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'employeeId': employeeId,
      'name': name,
      'department': department,
      'branchName': branchName,
      'role': role,
    };
  }

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] as int,
      employeeId: json['employeeId'] as String,
      name: json['name'] as String,
      department: json['department'] as String,
      branchName: json['branchName'] as String,
      role: json['role'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is User &&
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
    return 'User(id: $id, employeeId: $employeeId, name: $name, department: $department, branchName: $branchName, role: $role)';
  }
}
