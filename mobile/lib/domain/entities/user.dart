/// 사용자 엔티티
/// 인증 및 사용자 정보를 담는 도메인 엔티티
class User {
  /// 사용자 ID
  final int id;

  /// 사번 (8자리 숫자)
  final String employeeId;

  /// 이름
  final String name;

  /// 소속 조직명 (nullable)
  final String? orgName;

  /// 권한 (USER, LEADER, ADMIN)
  final String role;

  const User({
    required this.id,
    required this.employeeId,
    required this.name,
    this.orgName,
    required this.role,
  });

  User copyWith({
    int? id,
    String? employeeId,
    String? name,
    String? orgName,
    bool clearOrgName = false,
    String? role,
  }) {
    return User(
      id: id ?? this.id,
      employeeId: employeeId ?? this.employeeId,
      name: name ?? this.name,
      orgName: clearOrgName ? null : (orgName ?? this.orgName),
      role: role ?? this.role,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'employeeId': employeeId,
      'name': name,
      'orgName': orgName,
      'role': role,
    };
  }

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] as int,
      employeeId: json['employeeId'] as String,
      name: json['name'] as String,
      orgName: json['orgName'] as String?,
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
        other.orgName == orgName &&
        other.role == role;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      employeeId,
      name,
      orgName,
      role,
    );
  }

  @override
  String toString() {
    return 'User(id: $id, employeeId: $employeeId, name: $name, orgName: $orgName, role: $role)';
  }
}
