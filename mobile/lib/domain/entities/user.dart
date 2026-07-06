/// 사용자 엔티티
/// 인증 및 사용자 정보를 담는 도메인 엔티티
class User {
  /// 사용자 ID
  final int id;

  /// 사번 (8자리 숫자)
  final String employeeCode;

  /// 이름
  final String name;

  /// 소속 조직명 (nullable)
  final String? orgName;

  /// 권한 (USER, LEADER, ADMIN)
  final String role;

  /// 원본 권한 문자열 (SF `DKRetail__AppAuthority__c` picklist: `여사원`/`조장`/`지점장`/`AccountViewAll`).
  ///
  /// [role] 은 도메인 어휘(USER/LEADER/ADMIN)로 번역되면서 `null`/`여사원`/그 외 미매칭 값이
  /// 모두 `USER` 로 뭉개진다. "내 정보" 화면처럼 원본 권한이 미지정(null/공백)인 사용자를
  /// 실제 여사원과 구분해 표시해야 하는 곳에서만 이 값을 참조한다.
  /// 백엔드 미지정 시 `null`.
  final String? rawRole;

  const User({
    required this.id,
    required this.employeeCode,
    required this.name,
    this.orgName,
    required this.role,
    this.rawRole,
  });

  User copyWith({
    int? id,
    String? employeeCode,
    String? name,
    String? orgName,
    bool clearOrgName = false,
    String? role,
    String? rawRole,
  }) {
    return User(
      id: id ?? this.id,
      employeeCode: employeeCode ?? this.employeeCode,
      name: name ?? this.name,
      orgName: clearOrgName ? null : (orgName ?? this.orgName),
      role: role ?? this.role,
      rawRole: rawRole ?? this.rawRole,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'employeeCode': employeeCode,
      'name': name,
      'orgName': orgName,
      'role': role,
      'rawRole': rawRole,
    };
  }

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] as int,
      employeeCode: json['employeeCode'] as String,
      name: json['name'] as String,
      orgName: json['orgName'] as String?,
      role: json['role'] as String,
      rawRole: json['rawRole'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is User &&
        other.id == id &&
        other.employeeCode == employeeCode &&
        other.name == name &&
        other.orgName == orgName &&
        other.role == role &&
        other.rawRole == rawRole;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      employeeCode,
      name,
      orgName,
      role,
      rawRole,
    );
  }

  @override
  String toString() {
    return 'User(id: $id, employeeCode: $employeeCode, name: $name, orgName: $orgName, role: $role, rawRole: $rawRole)';
  }
}
