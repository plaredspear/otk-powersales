/// 조장의 본인 팀원 정보.
class LeaderTeamMember {
  final int id;
  final String employeeCode;
  final String name;
  final String? status;
  final String? costCenterCode;

  const LeaderTeamMember({
    required this.id,
    required this.employeeCode,
    required this.name,
    required this.status,
    required this.costCenterCode,
  });

  /// 휴직/퇴직 여부 — 화면에서 등록 버튼 비활성 처리에 사용.
  bool get isInactive => status == '휴직' || status == '퇴직';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is LeaderTeamMember &&
          id == other.id &&
          employeeCode == other.employeeCode;

  @override
  int get hashCode => id.hashCode ^ employeeCode.hashCode;
}
