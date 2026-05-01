import '../../domain/entities/leader_schedule_created.dart';

/// `POST /api/v1/leader/team-member-schedule` 응답 모델.
class LeaderScheduleCreateResponseModel {
  final int scheduleId;
  final int targetEmployeeId;
  final String workingDate;
  final String workingType;
  final String workingCategory3;
  final int proxyRegisteredBy;

  const LeaderScheduleCreateResponseModel({
    required this.scheduleId,
    required this.targetEmployeeId,
    required this.workingDate,
    required this.workingType,
    required this.workingCategory3,
    required this.proxyRegisteredBy,
  });

  factory LeaderScheduleCreateResponseModel.fromJson(
      Map<String, dynamic> json) {
    return LeaderScheduleCreateResponseModel(
      scheduleId: json['schedule_id'] as int,
      targetEmployeeId: json['target_employee_id'] as int,
      workingDate: json['working_date'] as String,
      workingType: json['working_type'] as String,
      workingCategory3: json['working_category3'] as String,
      proxyRegisteredBy: json['proxy_registered_by'] as int,
    );
  }

  LeaderScheduleCreated toEntity() => LeaderScheduleCreated(
        scheduleId: scheduleId,
        targetEmployeeId: targetEmployeeId,
        workingDate: DateTime.parse(workingDate),
        workingType: workingType,
        workingCategory3: workingCategory3,
        proxyRegisteredBy: proxyRegisteredBy,
      );
}
