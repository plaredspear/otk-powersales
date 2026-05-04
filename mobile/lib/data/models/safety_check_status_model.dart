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
      totalCount: json['totalCount'] as int,
      submittedCount: json['submittedCount'] as int,
      notSubmittedCount: json['notSubmittedCount'] as int,
      members: (json['members'] as List<dynamic>)
          .map((e) => MemberStatusModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}

class MemberStatusModel {
  final int id;
  final String employeeCode;
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
    required this.employeeCode,
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
      employeeCode: json['employeeCode'] as String,
      employeeName: json['employeeName'] as String,
      accountName: json['accountName'] as String?,
      submitted: json['submitted'] as bool,
      submittedAt: json['submittedAt'] as String?,
      equipments: (json['equipments'] as List<dynamic>)
          .map((e) => EquipmentStatusModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      yesCount: json['yesCount'] as int,
      noCount: json['noCount'] as int,
      precautions: json['precautions'] as String?,
      precautionCount: json['precautionCount'] as int,
      workReportStatus: json['workReportStatus'] as String?,
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
      seqNum: json['seqNum'] as int,
      label: json['label'] as String,
      answer: json['answer'] as String,
    );
  }
}
