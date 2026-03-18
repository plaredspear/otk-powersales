/// 안전점검 현황 응답 DTO
class SafetyCheckStatusModel {
  final String date;
  final int totalCount;
  final int submittedCount;
  final int notSubmittedCount;
  final List<MemberStatusModel> members;

  const SafetyCheckStatusModel({
    required this.date,
    required this.totalCount,
    required this.submittedCount,
    required this.notSubmittedCount,
    required this.members,
  });

  factory SafetyCheckStatusModel.fromJson(Map<String, dynamic> json) {
    return SafetyCheckStatusModel(
      date: json['date'] as String,
      totalCount: json['total_count'] as int,
      submittedCount: json['submitted_count'] as int,
      notSubmittedCount: json['not_submitted_count'] as int,
      members: (json['members'] as List<dynamic>)
          .map((e) => MemberStatusModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class MemberStatusModel {
  final int id;
  final String employeeId;
  final String employeeName;
  final String? accountName;
  final bool submitted;
  final String? submittedAt;
  final List<EquipmentStatusModel> equipments;
  final int yesCount;
  final int noCount;
  final String? precautions;
  final int precautionCount;
  final String? workReportStatus;

  const MemberStatusModel({
    required this.id,
    required this.employeeId,
    required this.employeeName,
    this.accountName,
    required this.submitted,
    this.submittedAt,
    required this.equipments,
    required this.yesCount,
    required this.noCount,
    this.precautions,
    required this.precautionCount,
    this.workReportStatus,
  });

  factory MemberStatusModel.fromJson(Map<String, dynamic> json) {
    return MemberStatusModel(
      id: json['id'] as int,
      employeeId: json['employee_id'] as String,
      employeeName: json['employee_name'] as String,
      accountName: json['account_name'] as String?,
      submitted: json['submitted'] as bool,
      submittedAt: json['submitted_at'] as String?,
      equipments: (json['equipments'] as List<dynamic>)
          .map((e) => EquipmentStatusModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      yesCount: json['yes_count'] as int,
      noCount: json['no_count'] as int,
      precautions: json['precautions'] as String?,
      precautionCount: json['precaution_count'] as int,
      workReportStatus: json['work_report_status'] as String?,
    );
  }
}

class EquipmentStatusModel {
  final int seqNum;
  final String label;
  final String answer;

  const EquipmentStatusModel({
    required this.seqNum,
    required this.label,
    required this.answer,
  });

  factory EquipmentStatusModel.fromJson(Map<String, dynamic> json) {
    return EquipmentStatusModel(
      seqNum: json['seq_num'] as int,
      label: json['label'] as String,
      answer: json['answer'] as String,
    );
  }
}
