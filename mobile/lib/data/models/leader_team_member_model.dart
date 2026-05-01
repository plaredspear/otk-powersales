import '../../domain/entities/leader_team_member.dart';

/// `GET /api/v1/mobile/leader/team-members` 응답의 단일 항목 모델.
class LeaderTeamMemberModel {
  final int id;
  final String employeeCode;
  final String name;
  final String? status;
  final String? costCenterCode;

  const LeaderTeamMemberModel({
    required this.id,
    required this.employeeCode,
    required this.name,
    required this.status,
    required this.costCenterCode,
  });

  factory LeaderTeamMemberModel.fromJson(Map<String, dynamic> json) {
    return LeaderTeamMemberModel(
      id: json['id'] as int,
      employeeCode: json['employee_code'] as String,
      name: json['name'] as String,
      status: json['status'] as String?,
      costCenterCode: json['cost_center_code'] as String?,
    );
  }

  LeaderTeamMember toEntity() => LeaderTeamMember(
        id: id,
        employeeCode: employeeCode,
        name: name,
        status: status,
        costCenterCode: costCenterCode,
      );
}
